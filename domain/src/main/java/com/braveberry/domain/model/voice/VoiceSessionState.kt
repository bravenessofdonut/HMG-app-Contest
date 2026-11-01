package com.braveberry.domain.model.voice

enum class EngineStatus { INITIALIZING, READY, ERROR }
enum class ListeningPhase { IDLE, REQUESTED, INPUT_READY, RECOGNIZING, ENDED }

data class VoiceSessionState(
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
    val speakerLanguageTags: Map<String, String> = emptyMap(),
    val speakerParameter: String = "",
    val requestedSpeaker: String? = null,
    val stopping: Boolean = false,
    val disconnected: Boolean = false,
    val previewTurn: String? = null,
    val lastSttEvent: String = "",
    val ttsError: String? = null,
    val sttError: String? = null
) {
    val previewRunning: Boolean get() = previewTurn != null
    val recognitionActive: Boolean get() = listening || listeningPhase == ListeningPhase.REQUESTED ||
        listeningPhase == ListeningPhase.INPUT_READY
    val canSpeak: Boolean get() = !disconnected && !speaking && !stopping && !recognitionActive &&
        ttsStatus == EngineStatus.READY

    fun applyEvent(event: VoiceEvent): VoiceSessionState = when (event) {
        VoiceEvent.TtsReady -> copy(ttsStatus = EngineStatus.READY, ttsError = null)
        VoiceEvent.SttReady -> copy(sttStatus = EngineStatus.READY, sttError = null, lastSttEvent = "onInitialized")
        VoiceEvent.Speaking -> copy(speaking = true, ttsError = null)
        VoiceEvent.SpeechDone, VoiceEvent.SpeechStopped -> copy(speaking = false)
        VoiceEvent.Listening -> copy(
            listening = true, listeningPhase = ListeningPhase.RECOGNIZING,
            transcript = "", transcriptCompleted = false, sttRms = null,
            sttEndpoint = null, lastSttEvent = "onStartedRecognition", sttError = null
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
            speakerKind = event.kind, availableSpeakers = event.values,
            speakerLanguageTags = event.languageTags, speakerParameter = event.parameter
        )
        is VoiceEvent.SpeakerRequestSent -> copy(requestedSpeaker = event.value, ttsError = null)
        is VoiceEvent.Failed -> when (event.engine) {
            VoiceEvent.Engine.TTS -> copy(
                speaking = false,
                ttsStatus = if (ttsStatus == EngineStatus.READY) ttsStatus else EngineStatus.ERROR,
                ttsError = event.reason
            )
            VoiceEvent.Engine.STT -> copy(
                listening = false, listeningPhase = ListeningPhase.ENDED,
                sttStatus = if (sttStatus == EngineStatus.READY) sttStatus else EngineStatus.ERROR,
                lastSttEvent = "onError", sttError = event.reason
            )
        }
    }
}
