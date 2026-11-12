package com.braveberry.domain.usecase.voice

import com.braveberry.domain.repository.RemoteTtsRepository
import javax.inject.Inject

/** Remote TTS에서 선택 가능한 화자·언어 목록. */
class GetRemoteTtsOptionsUseCase @Inject constructor(private val repository: RemoteTtsRepository) {
    operator fun invoke() = repository.options
}
