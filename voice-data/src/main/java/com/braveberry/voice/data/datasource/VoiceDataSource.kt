package com.braveberry.voice.data.datasource

import com.braveberry.domain.model.voice.VoiceEvent
import kotlinx.coroutines.flow.Flow

interface VoiceDataSource {
    val events: Flow<VoiceEvent>
    fun initialize()
    fun speak(text: String)
    fun selectSpeaker(value: String): Boolean
    fun stopSpeaking()
    fun startListening()
    fun stopListening()
    fun release()
}
