package com.braveberry.domain.usecase.voice

import com.braveberry.domain.repository.RemoteTtsRepository
import javax.inject.Inject

/** 진행 중인 Remote TTS 재생을 중지함. */
class StopRemoteTtsUseCase @Inject constructor(private val repository: RemoteTtsRepository) {
    operator fun invoke() = repository.stop()
}
