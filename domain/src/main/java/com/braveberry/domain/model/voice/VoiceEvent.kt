package com.braveberry.domain.model.voice

/** SDK 모델이 presentation 계층에 전파되지 않도록 유지함. */
sealed interface VoiceEvent {
    data object TtsReady : VoiceEvent
    data object SttReady : VoiceEvent
    data object Speaking : VoiceEvent
    data object SpeechDone : VoiceEvent
    data object SpeechStopped : VoiceEvent
    data object Listening : VoiceEvent
    data object SttInputReady : VoiceEvent
    data class SttRms(val value: Double) : VoiceEvent
    data class SttEndpoint(val speechStart: Long, val speechEnd: Long) : VoiceEvent
    data class Transcription(val text: String, val completed: Boolean) : VoiceEvent
    data object ListeningStopped : VoiceEvent
    data class SpeakerSupport(val kind: SpeakerKind, val values: List<String>, val parameter: String) : VoiceEvent
    data class SpeakerRequestSent(val value: String) : VoiceEvent
    data class Failed(val engine: Engine, val reason: String) : VoiceEvent

    enum class Engine { TTS, STT }
    enum class SpeakerKind { ENUM_VALUES, MANUAL_STRING, MANUAL_NUMBER, UNSUPPORTED }
}
