package com.braveberry.domain.usecase.voice

import com.braveberry.domain.repository.VoiceRepository
import javax.inject.Inject

/** 음성 기능 제어만 담당함. SDK 구체 타입은 모름. */
class VoiceActionsUseCase @Inject constructor(
    private val repository: VoiceRepository
) {
    fun initialize() = repository.initialize()
    fun speak(text: String) = repository.speak(text)
    fun stopSpeaking() = repository.stopSpeaking()
    fun startListening() = repository.startListening()
    fun stopListening() = repository.stopListening()
    fun release() = repository.release()
}
