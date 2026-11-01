package com.braveberry.voice.local.impl

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
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.voice.data.datasource.VoiceDataSource
import com.braveberry.voice.local.model.PleosSpeakerCatalog
import com.braveberry.voice.local.source.VoiceEventInbox
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/** Pleos Connect Speech Service와 직접 통신하는 유일한 경계. */
class PleosVoiceDataSourceImpl @Inject constructor(
    @ApplicationContext context: Context
) : VoiceDataSource {
    private val tts = TextToSpeech(context, TtsMode.ON_DEVICE)
    private val stt = SpeechToText(context, SttMode.ON_DEVICE)
    private val main = Handler(Looper.getMainLooper())
    private val inbox = VoiceEventInbox()
    override val events = inbox.events

    @Volatile private var released = false
    @Volatile private var ttsReady = false
    @Volatile private var sttReady = false
    private var initialized = false
    private var ttsListenerRegistered = false
    private var sttListenerRegistered = false
    private var lastRmsTime = 0L

    private fun emit(event: VoiceEvent) {
        if (!released) inbox.offer(event)
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
                    main.post {
                        if (released) return@post
                        runCatching {
                            tts.addEventListener(ttsListener)
                            ttsListenerRegistered = true
                            tts.setLocale(Locale.KOREA)
                            ttsReady = true
                            emit(VoiceEvent.TtsReady)
                            publishSpeakers()
                        }.onFailure { fail(VoiceEvent.Engine.TTS, it) }
                    }
                }

                override fun onFailure(errorType: TtsErrorType) =
                    emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, errorType.name))
            })
        }.onFailure { fail(VoiceEvent.Engine.TTS, it) }

        runCatching {
            stt.initialize(object : SttInitializeListener {
                override fun onSuccess() {
                    main.post {
                        if (released) return@post
                        runCatching {
                            stt.addListener(sttListener)
                            sttListenerRegistered = true
                            stt.setLocale(Locale.KOREA)
                            sttReady = true
                            emit(VoiceEvent.SttReady)
                        }.onFailure { fail(VoiceEvent.Engine.STT, it) }
                    }
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

    private fun publishSpeakers() {
        emit(VoiceEvent.SpeakerSupport(
            kind = VoiceEvent.SpeakerKind.MANUAL_STRING,
            values = PleosSpeakerCatalog.locales.keys.toList(),
            parameter = "String",
            languageTags = PleosSpeakerCatalog.languageTags
        ))
    }

    override fun selectSpeaker(value: String): Boolean {
        if (!ttsReady) {
            emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "TTS_NOT_READY"))
            return false
        }
        val locale = PleosSpeakerCatalog.locales[value] ?: run {
            emit(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "UNKNOWN_SPEAKER: $value"))
            return false
        }
        return runCatching {
            tts.setLocale(locale)
            tts.setSpeakerType(value)
            emit(VoiceEvent.SpeakerRequestSent(value))
        }.onFailure { fail(VoiceEvent.Engine.TTS, it) }.isSuccess
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

    private fun fail(engine: VoiceEvent.Engine, cause: Throwable) {
        if (cause is Error || cause is kotlinx.coroutines.CancellationException) throw cause
        android.util.Log.w("PleosVoice", "SDK call failed ($engine)", cause)
        emit(VoiceEvent.Failed(engine, cause.javaClass.simpleName))
    }

    override fun release() {
        if (released) return
        released = true
        inbox.close()
        // 콜백, 리스너, SDK 객체의 수명주기를 세션 종료에 맞춤.
        releaseSdk { if (ttsReady) tts.stop() }
        releaseSdk { if (sttReady) stt.stop() }
        releaseSdk { if (ttsListenerRegistered) tts.removeEventListener(ttsListener) }
        releaseSdk { if (sttListenerRegistered) stt.removeListener(sttListener) }
        releaseSdk { tts.release() }
        releaseSdk { stt.release() }
        ttsReady = false
        sttReady = false
    }

    private fun releaseSdk(action: () -> Unit) {
        try {
            action()
        } catch (failure: Exception) {
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            android.util.Log.w("PleosVoice", "SDK release failed", failure)
        }
    }
}
