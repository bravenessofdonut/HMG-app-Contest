package com.braveberry.presentation.voice.viewmodel

import com.braveberry.domain.model.voice.VoiceEvent

enum class EngineStatus { INITIALIZING, READY, ERROR }
enum class ListeningPhase { IDLE, REQUESTED, INPUT_READY, RECOGNIZING, ENDED }

data class VoiceUiState(
    val ttsStatus: EngineStatus = EngineStatus.INITIALIZING,
    val sttStatus: EngineStatus = EngineStatus.INITIALIZING,
    val speaking: Boolean = false,
    val listening: Boolean = false,
    val listeningPhase: ListeningPhase = ListeningPhase.IDLE,
    val transcript: String = "",
    val transcriptCompleted: Boolean = false,
    val sttRms: Double? = null,
    val sttEndpoint: Pair<Long, Long>? = null,
    val speakerKind: VoiceEvent.SpeakerKind = VoiceEvent.SpeakerKind.UNSUPPORTED,
    val availableSpeakers: List<String> = emptyList(),
    val speakerParameter: String = "확인 중",
    val requestedSpeaker: String? = null,
    val previewRunning: Boolean = false,
    val previewTurn: String? = null,
    val lastSttEvent: String = "아직 STT 이벤트 없음",
    val error: String? = null
) {
    fun applyEvent(event: VoiceEvent): VoiceUiState = when (event) {
        VoiceEvent.TtsReady -> copy(ttsStatus = EngineStatus.READY, error = null)
        VoiceEvent.SttReady -> copy(sttStatus = EngineStatus.READY, error = null, lastSttEvent = "SDK 초기화 성공")
        VoiceEvent.Speaking -> copy(speaking = true, error = null)
        VoiceEvent.SpeechDone, VoiceEvent.SpeechStopped -> copy(speaking = false)
        VoiceEvent.Listening -> copy(
            listening = true, listeningPhase = ListeningPhase.RECOGNIZING,
            transcript = "", transcriptCompleted = false, sttRms = null,
            sttEndpoint = null, lastSttEvent = "onStartedRecognition", error = null
        )
        VoiceEvent.SttInputReady -> copy(
            listeningPhase = ListeningPhase.INPUT_READY, lastSttEvent = "onReady"
        )
        is VoiceEvent.SttRms -> copy(sttRms = event.value, lastSttEvent = "onUpdatedRms")
        is VoiceEvent.SttEndpoint -> copy(
            sttEndpoint = event.speechStart to event.speechEnd, lastSttEvent = "onUpdatedEpdData"
        )
        is VoiceEvent.Transcription -> copy(
            transcript = event.text, transcriptCompleted = event.completed,
            lastSttEvent = if (event.completed) "onUpdated (final)" else "onUpdated (partial)"
        )
        VoiceEvent.ListeningStopped -> copy(
            listening = false, listeningPhase = ListeningPhase.ENDED,
            lastSttEvent = "onEndedRecognition"
        )
        is VoiceEvent.SpeakerSupport -> copy(
            speakerKind = event.kind, availableSpeakers = event.values, speakerParameter = event.parameter
        )
        is VoiceEvent.SpeakerRequestSent -> copy(requestedSpeaker = event.value, error = null)
        is VoiceEvent.Failed -> when (event.engine) {
            VoiceEvent.Engine.TTS -> copy(
                speaking = false,
                ttsStatus = if (ttsStatus == EngineStatus.READY) ttsStatus else EngineStatus.ERROR,
                error = "TTS: ${event.reason}"
            )
            VoiceEvent.Engine.STT -> copy(
                listening = false, listeningPhase = ListeningPhase.ENDED,
                sttStatus = if (sttStatus == EngineStatus.READY) sttStatus else EngineStatus.ERROR,
                lastSttEvent = "onError", error = "STT: ${event.reason}"
            )
        }
    }
}
