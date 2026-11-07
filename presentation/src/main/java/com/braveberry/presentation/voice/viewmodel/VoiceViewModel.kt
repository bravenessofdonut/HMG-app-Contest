package com.braveberry.presentation.voice.viewmodel

import androidx.lifecycle.ViewModel
import com.braveberry.domain.model.voice.VoiceSessionState
import com.braveberry.domain.usecase.voice.VoiceSessionOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

/** 세션을 대여해 읽기 전용 상태만 화면에 투영함. 재생 규칙을 다시 구현하지 않음. */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val owner: VoiceSessionOwner
) : ViewModel(), ContainerHost<VoiceSessionState, Nothing> {
    private val session = owner.acquire()
    override val container = container<VoiceSessionState, Nothing>(session.state.value)

    init {
        intent { session.state.collect { snapshot -> reduce { snapshot } } }
    }

    fun connect() = session.connect()
    fun speak(text: String, speaker: String) = session.speak(text, speaker)
    fun previewDialogue(aText: String, bText: String, aSpeaker: String, bSpeaker: String) =
        session.previewDialogue(aText, bText, aSpeaker, bSpeaker)
    fun stopSpeaking() = session.stopSpeaking()
    fun startListening() = session.startListening()
    fun stopListening() = session.stopListening()

    override fun onCleared() {
        owner.release()
        super.onCleared()
    }
}
