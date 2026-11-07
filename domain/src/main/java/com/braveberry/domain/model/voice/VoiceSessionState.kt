package com.braveberry.domain.model.voice

enum class EngineStatus { INITIALIZING, READY, ERROR }
enum class ListeningPhase { IDLE, REQUESTED, INPUT_READY, RECOGNIZING, STOPPING, ENDED }

/** 하나의 재생 계획만 상태의 원천으로 관리함. UI에 필요한 읽기 전용 속성은 계산함. */
data class VoiceSessionState(
    val playback: PlaybackPlan = PlaybackPlan(),
    val ttsStatus: EngineStatus = EngineStatus.INITIALIZING,
    val sttStatus: EngineStatus = EngineStatus.INITIALIZING,
    val listening: Boolean = false,
    val listeningPhase: ListeningPhase = ListeningPhase.IDLE,
    val transcript: String = "",
    val transcriptCompleted: Boolean = false,
    val sttRms: Double? = null,
    val sttEndpoint: Pair<Long, Long>? = null,
    val speakerKind: VoiceEvent.SpeakerKind = VoiceEvent.SpeakerKind.UNSUPPORTED,
    val availableSpeakers: List<String> = emptyList(),
    val speakerLanguageTags: Map<String, String> = emptyMap(),
    val requestedSpeaker: String? = null,
    val disconnected: Boolean = false,
    val ttsError: String? = null,
    val sttError: String? = null
) {
    val speaking: Boolean get() = when (playback.phase) {
        is PlaybackPhase.Starting, is PlaybackPhase.Playing, is PlaybackPhase.Stopping -> true
        else -> false
    }
    val stopping: Boolean get() = playback.phase is PlaybackPhase.Stopping
    val previewTurn: String? get() = when (val p = playback.phase) {
        is PlaybackPhase.Starting -> p.turn.role
        is PlaybackPhase.Playing -> p.turn.role
        is PlaybackPhase.Stopping -> p.turn.role
        else -> null
    }
    val previewRunning: Boolean get() = previewTurn != null
    val recognitionActive: Boolean get() = listening || listeningPhase in setOf(
        ListeningPhase.REQUESTED, ListeningPhase.INPUT_READY, ListeningPhase.STOPPING
    )
    val canSpeak: Boolean get() = !disconnected && playback.phase == PlaybackPhase.Idle &&
        !recognitionActive && ttsStatus == EngineStatus.READY

    /** UI 선택지와 관계없이 실제 명령에서도 한국어 진행자 두 명만 허용함. */
    fun canPreview(a: String, b: String): Boolean =
        a != b && speakerLanguageTags[a] == "ko-KR" && speakerLanguageTags[b] == "ko-KR"

    fun applyEvent(event: VoiceEvent): VoiceSessionState = when (event) {
        VoiceEvent.TtsReady -> copy(ttsStatus = EngineStatus.READY, ttsError = null)
        VoiceEvent.SttReady -> copy(sttStatus = EngineStatus.READY, sttError = null)
        VoiceEvent.Speaking, VoiceEvent.SpeechDone, VoiceEvent.SpeechStopped -> this // 재생은 PlaybackPlan만 변경함
        VoiceEvent.Listening -> copy(
            listening = true, listeningPhase = ListeningPhase.RECOGNIZING,
            transcript = "", transcriptCompleted = false, sttRms = null,
            sttEndpoint = null, sttError = null
        )
        VoiceEvent.SttInputReady -> copy(listeningPhase = ListeningPhase.INPUT_READY)
        is VoiceEvent.SttRms -> copy(sttRms = event.value)
        is VoiceEvent.SttEndpoint -> copy(sttEndpoint = event.speechStart to event.speechEnd)
        is VoiceEvent.Transcription -> copy(transcript = event.text, transcriptCompleted = event.completed)
        VoiceEvent.ListeningStopped -> copy(listening = false, listeningPhase = ListeningPhase.ENDED)
        is VoiceEvent.SpeakerSupport -> copy(
            speakerKind = event.kind, availableSpeakers = event.values,
            speakerLanguageTags = event.languageTags
        )
        is VoiceEvent.SpeakerRequestSent -> copy(requestedSpeaker = event.value, ttsError = null)
        is VoiceEvent.Failed -> when (event.engine) {
            VoiceEvent.Engine.TTS -> copy(
                ttsStatus = if (ttsStatus == EngineStatus.READY) ttsStatus else EngineStatus.ERROR,
                ttsError = event.reason
            )
            VoiceEvent.Engine.STT -> copy(
                listening = false, listeningPhase = ListeningPhase.ENDED,
                sttStatus = if (sttStatus == EngineStatus.READY) sttStatus else EngineStatus.ERROR,
                sttError = event.reason
            )
        }
    }
}
