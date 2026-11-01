package com.braveberry.voice.local.impl

import android.content.Context
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType as SttErrorType
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType as TtsErrorType
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitializeListener
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.voice.data.datasource.VoiceDataSource
import com.braveberry.voice.local.model.PleosSpeakerCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.lang.reflect.Method
import android.os.SystemClock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Pleos Connect Speech Service와 직접 통신하는 유일한 경계. */
class PleosVoiceDataSourceImpl @Inject constructor(
    @ApplicationContext context: Context
) : VoiceDataSource {
    private val tts = TextToSpeech(context, TtsMode.ON_DEVICE)
    private val stt = SpeechToText(context, SttMode.ON_DEVICE)
    private val eventBus = MutableSharedFlow<VoiceEvent>(extraBufferCapacity = 64)
    override val events = eventBus.asSharedFlow()

    @Volatile private var released = false
    @Volatile private var ttsReady = false
    @Volatile private var sttReady = false
    private var initialized = false
    private var ttsListenerRegistered = false
    private var sttListenerRegistered = false
    private var speakerSetter: Method? = null
    private var speakerKind = VoiceEvent.SpeakerKind.UNSUPPORTED
    private var speakerValues: List<String> = emptyList()
    private var lastRmsTime = 0L

    private fun emit(event: VoiceEvent) {
        if (!released) eventBus.tryEmit(event)
    }

    private val ttsListener = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = emit(VoiceEvent.Speaking)
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = emit(VoiceEvent.SpeechDone)
        override fun onStop() = emit(VoiceEvent.SpeechStopped)
        override fun onError(errorType: TtsErrorType) =
            emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, errorType.name))
    }

    private val sttListener = object : ResultListener {
        override fun onReady() = emit(VoiceEvent.SttInputReady)
        override fun onStartedRecognition() = emit(VoiceEvent.Listening)
        override fun onUpdated(stt: String, completed: Boolean) =
            emit(VoiceEvent.Transcription(stt, completed))
        override fun onUpdatedEpdData(on: Long, off: Long) =
            emit(VoiceEvent.SttEndpoint(on, off))
        override fun onUpdatedRms(rms: Double) {
            val now = SystemClock.elapsedRealtime()
            // UI 갱신 빈도를 제한함. 값은 SDK 원본 단위 그대로 표시함.
            if (now - lastRmsTime >= 250L) {
                lastRmsTime = now
                emit(VoiceEvent.SttRms(rms))
            }
        }
        override fun onEndedRecognition() = emit(VoiceEvent.ListeningStopped)
        override fun onError(errorType: SttErrorType) =
            emit(VoiceEvent.Failed(VoiceEvent.Engine.STT, errorType.name))
    }

    override fun initialize() {
        if (released || initialized) return
        initialized = true
        runCatching {
            tts.initialize(object : TtsInitializeListener {
                override fun onSuccess() {
                    if (released) return
                    runCatching {
                        tts.addEventListener(ttsListener)
                        ttsListenerRegistered = true
                        tts.setLocale(Locale.KOREA)
                        ttsReady = true
                        emit(VoiceEvent.TtsReady)
                        discoverSpeakerCapability()
                    }.onFailure { fail(VoiceEvent.Engine.TTS, it) }
                }

                override fun onFailure(errorType: TtsErrorType) =
                    emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, errorType.name))
            })
        }.onFailure { fail(VoiceEvent.Engine.TTS, it) }

        runCatching {
            stt.initialize(object : SttInitializeListener {
                override fun onSuccess() {
                    if (released) return
                    runCatching {
                        stt.addListener(sttListener)
                        sttListenerRegistered = true
                        stt.setLocale(Locale.KOREA)
                        sttReady = true
                        emit(VoiceEvent.SttReady)
                    }.onFailure { fail(VoiceEvent.Engine.STT, it) }
                }

                override fun onFailure(errorType: SttErrorType) =
                    emit(VoiceEvent.Failed(VoiceEvent.Engine.STT, errorType.name))
            })
        }.onFailure { fail(VoiceEvent.Engine.STT, it) }
    }

    override fun speak(text: String) {
        if (text.isBlank()) return
        if (!ttsReady) {
            emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "TTS_NOT_READY"))
            return
        }
        runCatching { tts.speak(text) }.onFailure { fail(VoiceEvent.Engine.TTS, it) }
    }

    /** SDK 3.3.1의 공개 시그니처를 확인하고 문서상 문자열 화자 또는 enum 값만 노출함. */
    private fun discoverSpeakerCapability() {
        val candidates = tts.javaClass.methods.filter {
            it.name == "setSpeakerType" && it.parameterTypes.size == 1
        }
        fun rank(method: Method): Int = when (val type = method.parameterTypes[0]) {
            String::class.java -> 1
            Int::class.javaPrimitiveType, Int::class.javaObjectType -> 2
            else -> if (type.isEnum) 0 else 10
        }
        val method = candidates.minByOrNull(::rank)
        speakerSetter = method
        val type = method?.parameterTypes?.singleOrNull()
        speakerKind = when {
            type?.isEnum == true -> VoiceEvent.SpeakerKind.ENUM_VALUES
            type == String::class.java -> VoiceEvent.SpeakerKind.MANUAL_STRING
            type == Int::class.javaPrimitiveType || type == Int::class.javaObjectType ->
                VoiceEvent.SpeakerKind.MANUAL_NUMBER
            else -> VoiceEvent.SpeakerKind.UNSUPPORTED
        }
        speakerValues = when {
            type?.isEnum == true -> type.enumConstants.map { (it as Enum<*>).name }
            speakerKind == VoiceEvent.SpeakerKind.MANUAL_STRING -> PleosSpeakerCatalog.locales.keys.toList()
            else -> emptyList()
        }
        emit(VoiceEvent.SpeakerSupport(
            kind = speakerKind,
            values = speakerValues,
            parameter = type?.name ?: "호출 불가",
            languageTags = if (speakerKind == VoiceEvent.SpeakerKind.MANUAL_STRING)
                PleosSpeakerCatalog.languageTags else emptyMap()
        ))
    }

    override fun selectSpeaker(value: String): Boolean {
        if (!ttsReady) {
            emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "TTS_NOT_READY"))
            return false
        }
        val method = speakerSetter ?: run {
            emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SPEAKER_TYPE_UNSUPPORTED"))
            return false
        }
        val type = method.parameterTypes.single()
        val argument: Any = when (speakerKind) {
            VoiceEvent.SpeakerKind.ENUM_VALUES ->
                type.enumConstants.firstOrNull { (it as Enum<*>).name == value } ?: return false
            VoiceEvent.SpeakerKind.MANUAL_STRING -> value.takeIf { it in PleosSpeakerCatalog.locales }
                ?: run {
                    emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "UNKNOWN_SPEAKER: $value"))
                    return false
                }
            VoiceEvent.SpeakerKind.MANUAL_NUMBER -> value.toIntOrNull() ?: return false
            VoiceEvent.SpeakerKind.UNSUPPORTED -> return false
        }
        return runCatching {
            // 변경한 화자의 언어를 먼저 지정함. 한국어로 고정하면 타 언어 화자 테스트가 부정확함.
            if (speakerKind == VoiceEvent.SpeakerKind.MANUAL_STRING) {
                tts.setLocale(PleosSpeakerCatalog.locales.getValue(value))
            }
            val result = method.invoke(tts, argument)
            if (result == false) error("setSpeakerType returned false")
            // 호출 성공은 실제 음색 변화의 보증이 아님. 직접 청취 확인해야 함.
            emit(VoiceEvent.SpeakerRequestSent(value))
        }.onFailure { fail(VoiceEvent.Engine.TTS, it.cause ?: it) }.isSuccess
    }

    override fun stopSpeaking() {
        if (ttsReady) runCatching { tts.stop() }.onFailure { fail(VoiceEvent.Engine.TTS, it) }
    }

    override fun startListening() {
        if (!sttReady) {
            emit(VoiceEvent.Failed(VoiceEvent.Engine.STT, "STT_NOT_READY"))
            return
        }
        runCatching { stt.request() }.onFailure { fail(VoiceEvent.Engine.STT, it) }
    }

    override fun stopListening() {
        if (sttReady) runCatching { stt.stop() }.onFailure { fail(VoiceEvent.Engine.STT, it) }
    }

    private fun fail(engine: VoiceEvent.Engine, cause: Throwable) =
        emit(VoiceEvent.Failed(engine, cause.message ?: cause.javaClass.simpleName))

    override fun release() {
        if (released) return
        released = true
        // 콜백, 리스너, SDK 객체의 수명주기를 ViewModel에 맞춤.
        runCatching { if (ttsReady) tts.stop() }
        runCatching { if (sttReady) stt.stop() }
        runCatching { if (ttsListenerRegistered) tts.removeEventListener(ttsListener) }
        runCatching { if (sttListenerRegistered) stt.removeListener(sttListener) }
        runCatching { tts.release() }
        runCatching { stt.release() }
        ttsReady = false
        sttReady = false
    }
}
