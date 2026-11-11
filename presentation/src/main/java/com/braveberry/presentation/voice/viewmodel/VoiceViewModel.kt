package com.braveberry.presentation.voice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.VoiceSessionState
import com.braveberry.domain.usecase.voice.GetRemoteTtsOptionsUseCase
import com.braveberry.domain.usecase.voice.SpeakRemoteTtsUseCase
import com.braveberry.domain.usecase.voice.VoiceSessionOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

/** 세션을 대여해 읽기 전용 상태만 화면에 투영함. 재생 규칙을 다시 구현하지 않음. */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val owner: VoiceSessionOwner,
    private val speakRemoteTts: SpeakRemoteTtsUseCase,
    getRemoteTtsOptions: GetRemoteTtsOptionsUseCase
) : ViewModel(), ContainerHost<VoiceSessionState, Nothing> {
    private val session = owner.acquire()
    private var remoteTtsJob: Job? = null
    override val container = container<VoiceSessionState, Nothing>(session.state.value)

    /** 변하지 않는 선택지라 Orbit 상태가 아닌 값으로 노출함. */
    val remoteTtsOptions: RemoteTtsOptions = getRemoteTtsOptions()

    init {
        intent { session.state.collect { snapshot -> reduce { snapshot } } }
    }

    fun connect() = session.connect()
    fun speak(text: String, speaker: String) = session.speak(text, speaker)
    // 재생 상태와 실패(DataResource.Error)는 화면에 표시하지 않음(팀 논의 후 결정). 세부 원인은 로그에 남음.
    // 수집하는 동안만 재생되며, 화면을 떠나면 viewModelScope 취소로 함께 멈춤.
    fun speakRemote(text: String, voiceName: String, language: String) {
        remoteTtsJob?.cancel()
        remoteTtsJob = viewModelScope.launch {
            speakRemoteTts(text, voiceName, language).collect()
        }
    }

    fun stopRemote() {
        remoteTtsJob?.cancel()
        remoteTtsJob = null
    }

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
