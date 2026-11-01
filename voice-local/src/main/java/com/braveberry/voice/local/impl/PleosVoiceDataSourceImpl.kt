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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
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
        override fun onReady() = Unit
        override fun onStartedRecognition() = emit(VoiceEvent.Listening)
        override fun onUpdated(stt: String, completed: Boolean) =
            emit(VoiceEvent.Transcription(stt, completed))
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
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
