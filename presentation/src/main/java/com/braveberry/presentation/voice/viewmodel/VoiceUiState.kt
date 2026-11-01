package com.braveberry.presentation.voice.viewmodel

import com.braveberry.domain.model.voice.VoiceEvent

enum class EngineStatus { INITIALIZING, READY, ERROR }

data class VoiceUiState(
    val ttsStatus: EngineStatus = EngineStatus.INITIALIZING,
    val sttStatus: EngineStatus = EngineStatus.INITIALIZING,
    val speaking: Boolean = false,
    val listening: Boolean = false,
    val transcript: String = "",
    val transcriptCompleted: Boolean = false,
    val error: String? = null
) {
    fun applyEvent(event: VoiceEvent): VoiceUiState = when (event) {
        VoiceEvent.TtsReady -> copy(ttsStatus = EngineStatus.READY, error = null)
        VoiceEvent.SttReady -> copy(sttStatus = EngineStatus.READY, error = null)
        VoiceEvent.Speaking -> copy(speaking = true, error = null)
        VoiceEvent.SpeechDone, VoiceEvent.SpeechStopped -> copy(speaking = false)
        VoiceEvent.Listening -> copy(listening = true, transcript = "", transcriptCompleted = false, error = null)
        is VoiceEvent.Transcription -> copy(transcript = event.text, transcriptCompleted = event.completed)
        VoiceEvent.ListeningStopped -> copy(listening = false)
        is VoiceEvent.Failed -> when (event.engine) {
            VoiceEvent.Engine.TTS -> copy(
                speaking = false,
                ttsStatus = if (ttsStatus == EngineStatus.READY) ttsStatus else EngineStatus.ERROR,
                error = "TTS: ${event.reason}"
            )
            VoiceEvent.Engine.STT -> copy(
                listening = false,
                sttStatus = if (sttStatus == EngineStatus.READY) sttStatus else EngineStatus.ERROR,
                error = "STT: ${event.reason}"
            )
        }
    }
}
