package com.braveberry.presentation.voice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** 차량 신호와 별개로 음성 화면의 UI 상태만 관리함. */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val observeVoiceEvents: ObserveVoiceEventsUseCase,
    private val voiceActions: VoiceActionsUseCase
) : ViewModel(), ContainerHost<VoiceUiState, Nothing> {
    override val container = container<VoiceUiState, Nothing>(VoiceUiState())

    init {
        // 동기적으로 구독을 시작한 후 SDK 초기화를 호출해 초기 이벤트 유실을 줄임.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            observeVoiceEvents().collect { event ->
                intent { reduce { state.applyEvent(event) } }
            }
        }
        viewModelScope.launch { voiceActions.initialize() }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        voiceActions.speak(text.trim())
    }

    fun stopSpeaking() = voiceActions.stopSpeaking()
    fun startListening() = voiceActions.startListening()
    fun stopListening() = voiceActions.stopListening()

    override fun onCleared() {
        // SDK 리스너·연결이 화면보다 오래 살아남지 않도록 해제함.
        voiceActions.release()
        super.onCleared()
    }
}
