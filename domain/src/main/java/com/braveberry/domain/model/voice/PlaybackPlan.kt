package com.braveberry.domain.model.voice

/** SDK 콜백에는 발화 ID가 없음. 하나의 연결에서 순서대로 한 발화만 실행함. */
data class VoiceTurn(val text: String, val speaker: String, val role: String? = null) {
    init { require(text.isNotBlank()) }
}

sealed interface PlaybackPhase {
    data object Idle : PlaybackPhase
    data class Starting(val turn: VoiceTurn) : PlaybackPhase
    data class Playing(val turn: VoiceTurn) : PlaybackPhase
    data class Stopping(val turn: VoiceTurn) : PlaybackPhase
    data class Failed(val reason: String) : PlaybackPhase
}

/**
 * 순수 전이 모델. 대기 발화도 같은 값에 포함하므로 상태 플래그와 큐가 따로 어긋날 수 없음.
 * onDone은 onStart가 확인된 Playing에서만 성공으로 해석함.
 */
data class PlaybackPlan(
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val next: VoiceTurn? = null
) {
    fun begin(turn: VoiceTurn, after: VoiceTurn? = null): PlaybackPlan? =
        if (phase == PlaybackPhase.Idle) PlaybackPlan(PlaybackPhase.Starting(turn), after) else null

    fun started(): PlaybackPlan? = when (val active = phase) {
        is PlaybackPhase.Starting -> copy(phase = PlaybackPhase.Playing(active.turn))
        else -> null
    }

    fun finished(): Completion? = when (phase) {
        is PlaybackPhase.Playing -> Completion(PlaybackPlan(), next)
        else -> null
    }

    fun requestStop(): PlaybackPlan? = when (val active = phase) {
        is PlaybackPhase.Starting -> PlaybackPlan(PlaybackPhase.Stopping(active.turn))
        is PlaybackPhase.Playing -> PlaybackPlan(PlaybackPhase.Stopping(active.turn))
        else -> null
    }

    fun stopped(): PlaybackPlan? = when (phase) {
        is PlaybackPhase.Stopping -> PlaybackPlan()
        else -> null
    }

    fun failed(reason: String) = PlaybackPlan(PlaybackPhase.Failed(reason))

    data class Completion(val completed: PlaybackPlan, val following: VoiceTurn?)
}
