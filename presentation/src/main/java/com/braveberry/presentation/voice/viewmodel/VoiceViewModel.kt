package com.braveberry.presentation.voice.viewmodel

import androidx.lifecycle.ViewModel
import com.braveberry.domain.model.voice.VoiceSessionState
import com.braveberry.domain.usecase.voice.VoiceSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val session: VoiceSession
) : ViewModel(), ContainerHost<VoiceSessionState, Nothing> {
    override val container = container<VoiceSessionState, Nothing>(session.state.value)

    init {
        session.attachScreen()
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
        session.detachScreen()
        super.onCleared()
    }
}
