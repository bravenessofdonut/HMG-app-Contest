package com.braveberry.domain.usecase.voice

import com.braveberry.domain.repository.VoiceRepository
import javax.inject.Inject

class ObserveVoiceEventsUseCase @Inject constructor(
    private val repository: VoiceRepository
) {
    operator fun invoke() = repository.events
}
