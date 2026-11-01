package com.braveberry.voice.data.datasource

import com.braveberry.domain.model.voice.VoiceEvent
import kotlinx.coroutines.flow.Flow

/** Pleos SDK와 Repository 사이의 경계. */
interface VoiceDataSource {
    val events: Flow<VoiceEvent>
    fun initialize()
    fun speak(text: String)
    fun stopSpeaking()
    fun startListening()
    fun stopListening()
    fun release()
}
