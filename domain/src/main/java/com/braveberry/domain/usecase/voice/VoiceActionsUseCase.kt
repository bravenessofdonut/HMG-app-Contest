package com.braveberry.domain.usecase.voice

import com.braveberry.domain.repository.VoiceRepository
import javax.inject.Inject

class VoiceActionsUseCase @Inject constructor(
    private val repository: VoiceRepository
) {
    fun initialize() = repository.initialize()
    fun speak(text: String) = repository.speak(text)
    fun selectSpeaker(value: String): Boolean = repository.selectSpeaker(value)
    fun stopSpeaking() = repository.stopSpeaking()
    fun startListening() = repository.startListening()
    fun stopListening() = repository.stopListening()
    fun release() = repository.release()
}
