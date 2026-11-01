package com.braveberry.domain.usecase.voice

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

sealed interface Playback {
    data object Idle : Playback
    data class Speaking(val role: String?) : Playback
}

class ObservePlaybackUseCase @Inject constructor(private val session: VoiceSession) {
    operator fun invoke(): Flow<Playback> = session.state.map { state ->
        if (state.speaking) Playback.Speaking(state.previewTurn) else Playback.Idle
    }.distinctUntilChanged()
}
