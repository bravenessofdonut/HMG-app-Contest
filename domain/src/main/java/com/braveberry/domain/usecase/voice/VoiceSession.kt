package com.braveberry.domain.usecase.voice

import com.braveberry.domain.model.voice.VoiceSessionState
import kotlinx.coroutines.flow.StateFlow

/** 재생 명령과 관찰 계약. UI/Activity/SDK의 생명주기를 알지 못함. */
interface VoiceSession {
    val state: StateFlow<VoiceSessionState>
    fun connect()
    fun speak(text: String, speaker: String)
    fun previewDialogue(aText: String, bText: String, aSpeaker: String, bSpeaker: String)
    fun stopSpeaking()
    fun startListening()
    fun stopListening()
}
