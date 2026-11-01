package com.braveberry.domain.repository

import com.braveberry.domain.model.voice.VoiceEvent
import kotlinx.coroutines.flow.Flow

/** Pleos SDK를 숨긴 음성 기능 계약. */
interface VoiceRepository {
    val events: Flow<VoiceEvent>
    fun initialize()
    fun speak(text: String)
    fun selectSpeaker(value: String): Boolean
    fun stopSpeaking()
    fun startListening()
    fun stopListening()
    fun release()
}
