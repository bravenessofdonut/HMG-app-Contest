package com.braveberry.domain.repository

import com.braveberry.domain.model.voice.VoiceEvent
import kotlinx.coroutines.flow.Flow

/** events는 세션 내부 단일 수집자 전용임. 화면은 세션의 StateFlow를 구독함. */
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
