package com.braveberry.domain.usecase.voice

import com.braveberry.domain.repository.RemoteTtsRepository
import javax.inject.Inject

/** Remote TTS로 문장을 읽음. 반환된 Flow를 수집하는 동안 재생되고, 취소하면 멈춤. */
class SpeakRemoteTtsUseCase @Inject constructor(private val repository: RemoteTtsRepository) {
    operator fun invoke(text: String, voiceName: String, language: String) =
        repository.speak(text, voiceName, language)
}
