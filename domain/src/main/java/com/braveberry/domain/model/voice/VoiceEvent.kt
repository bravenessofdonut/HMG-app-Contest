package com.braveberry.domain.model.voice

/** SDK 이벤트를 앱 도메인 타입으로 변환함. */
sealed interface VoiceEvent {
    data object TtsReady : VoiceEvent
    data object SttReady : VoiceEvent
    data object Speaking : VoiceEvent
    data object SpeechDone : VoiceEvent
    data object SpeechStopped : VoiceEvent
    data object Listening : VoiceEvent
    data class Transcription(val text: String, val completed: Boolean) : VoiceEvent
    data object ListeningStopped : VoiceEvent
    data class Failed(val engine: Engine, val reason: String) : VoiceEvent

    enum class Engine { TTS, STT }
}
