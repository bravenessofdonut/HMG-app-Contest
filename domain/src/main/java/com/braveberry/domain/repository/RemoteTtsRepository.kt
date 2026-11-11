package com.braveberry.domain.repository

import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.VoiceEvent
import kotlinx.coroutines.flow.Flow

/**
 * Gemini Remote TTS를 제공하는 도메인 리포지토리 인터페이스.
 * language는 BCP-47 태그 (예: "en-US", "ja-JP", "cmn-CN").
 */
interface RemoteTtsRepository {
    val events: Flow<VoiceEvent>
    val options: RemoteTtsOptions
    fun speak(text: String, voiceName: String, language: String)
    fun stop()
    fun release()
    fun speakStream(tokens: Flow<String>, voiceName: String, language: String)
}
