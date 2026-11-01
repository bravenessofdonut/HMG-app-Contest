package com.braveberry.domain.repository

import com.braveberry.domain.model.voice.VoiceEvent
import kotlinx.coroutines.flow.Flow

/** Pleos SDK 타입이 외부 계층으로 전파되지 않도록 제한함. */
interface VoiceRepository {
    val events: Flow<VoiceEvent>
    fun initialize()
    fun speak(text: String)
    fun stopSpeaking()
    fun startListening()
    fun stopListening()
    fun release()
}
