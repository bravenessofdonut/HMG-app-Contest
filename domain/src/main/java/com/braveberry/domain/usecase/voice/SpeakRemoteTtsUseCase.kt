package com.braveberry.domain.usecase.voice

import com.braveberry.domain.repository.RemoteTtsRepository
import javax.inject.Inject

/** Remote TTS로 문장을 읽음. language는 BCP-47 태그 (en-US, ja-JP, cmn-CN). */
class SpeakRemoteTtsUseCase @Inject constructor(private val repository: RemoteTtsRepository) {
    operator fun invoke(text: String, voiceName: String, language: String) =
        repository.speak(text, voiceName, language)
}
