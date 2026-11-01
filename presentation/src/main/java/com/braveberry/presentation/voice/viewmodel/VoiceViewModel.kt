package com.braveberry.presentation.voice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.domain.usecase.voice.ObserveVoiceEventsUseCase
import com.braveberry.domain.usecase.voice.VoiceActionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

/** 재생 순서는 SDK 완료 콜백에 의해 진행됨. 고정 지연시간을 사용하지 않음. */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val observeVoiceEvents: ObserveVoiceEventsUseCase,
    private val voiceActions: VoiceActionsUseCase
) : ViewModel(), ContainerHost<VoiceUiState, Nothing> {
    override val container = container<VoiceUiState, Nothing>(VoiceUiState())
    private var previewQueue = ArrayDeque<PreviewTurn>()
    private var inPreview = false

    private data class PreviewTurn(val role: String, val speaker: String, val text: String)

    init {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            observeVoiceEvents().collect { event ->
                intent { reduce { state.applyEvent(event) } }
                when (event) {
                    VoiceEvent.SpeechDone -> if (inPreview) playNextPreviewTurn()
                    VoiceEvent.SpeechStopped -> clearPreview()
                    is VoiceEvent.Failed -> if (event.engine == VoiceEvent.Engine.TTS) clearPreview()
                    else -> Unit
                }
            }
        }
        viewModelScope.launch { voiceActions.initialize() }
    }

    fun speak(text: String, speaker: String) {
        if (text.isBlank()) return
        clearPreview()
        if (speaker.isNotBlank() && !voiceActions.selectSpeaker(speaker)) return
        voiceActions.speak(text.trim())
    }

    /** 실제 LLM 생성 대본이 아닌 고정 2턴 시연. */
    fun previewDialogue(aText: String, bText: String, aSpeaker: String, bSpeaker: String) {
        if (aText.isBlank() || bText.isBlank() || aSpeaker.isBlank() || bSpeaker.isBlank()) return
        if (aSpeaker == bSpeaker) return // 서로 다른 화자를 선택해야 시연함.
        if (inPreview || container.stateFlow.value.speaking || container.stateFlow.value.listening) return
        previewQueue.clear()
        previewQueue.addLast(PreviewTurn("A", aSpeaker, aText.trim()))
        previewQueue.addLast(PreviewTurn("B", bSpeaker, bText.trim()))
        inPreview = true
        playNextPreviewTurn()
    }

    private fun playNextPreviewTurn() {
        val turn = previewQueue.removeFirstOrNull()
        if (!inPreview || turn == null) {
            clearPreview()
            return
        }
        if (!voiceActions.selectSpeaker(turn.speaker)) {
            clearPreview()
            return
        }
        intent { reduce { state.copy(previewRunning = true, previewTurn = turn.role) } }
        voiceActions.speak(turn.text)
    }

    private fun clearPreview() {
        inPreview = false
        previewQueue.clear()
        intent { reduce { state.copy(previewRunning = false, previewTurn = null) } }
    }

    fun stopSpeaking() {
        clearPreview()
        voiceActions.stopSpeaking()
    }

    fun startListening() {
        if (inPreview || container.stateFlow.value.speaking) stopSpeaking()
        intent { reduce { state.copy(listeningPhase = ListeningPhase.REQUESTED) } }
        voiceActions.startListening()
    }

    fun stopListening() = voiceActions.stopListening()

    override fun onCleared() {
        inPreview = false
        previewQueue.clear()
        voiceActions.release()
        super.onCleared()
    }
}
