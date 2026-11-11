package com.braveberry.presentation.voice.viewmodel

import com.braveberry.domain.model.voice.PlaybackPhase
import com.braveberry.domain.model.voice.PlaybackPlan
import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.VoiceSessionState
import com.braveberry.domain.model.voice.VoiceTurn
import com.braveberry.domain.repository.RemoteTtsRepository
import com.braveberry.domain.usecase.voice.GetRemoteTtsOptionsUseCase
import com.braveberry.domain.usecase.voice.SpeakRemoteTtsUseCase
import com.braveberry.domain.usecase.voice.StopRemoteTtsUseCase
import com.braveberry.domain.usecase.voice.VoiceSession
import com.braveberry.domain.usecase.voice.VoiceSessionOwner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModelTest {
    private val options = RemoteTtsOptions(listOf("Kore", "Fenrir"), "Fenrir", listOf("en-US", "ja-JP", "cmn-CN"))

    @Test fun reopenedScreenObservesCurrentTurnAndForwardsStop() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val active = VoiceTurn("hello", "eunwoo", "A")
            val state = MutableStateFlow(VoiceSessionState(playback = PlaybackPlan(PlaybackPhase.Playing(active))))
            val session = mockk<VoiceSession>(relaxUnitFun = true)
            val owner = mockk<VoiceSessionOwner>(relaxUnitFun = true)
            val remoteTtsRepository = mockk<RemoteTtsRepository>(relaxUnitFun = true)
            every { remoteTtsRepository.options } returns options
            every { owner.acquire() } returns session
            every { session.state } returns state
            val vm = VoiceViewModel(
                owner,
                SpeakRemoteTtsUseCase(remoteTtsRepository),
                StopRemoteTtsUseCase(remoteTtsRepository),
                GetRemoteTtsOptionsUseCase(remoteTtsRepository)
            )
            assertEquals("A", vm.container.stateFlow.value.previewTurn)
            state.value = state.value.copy(playback = PlaybackPlan(PlaybackPhase.Playing(active.copy(role = "B"))))
            withTimeout(5_000) { vm.container.stateFlow.first { it.previewTurn == "B" } }
            vm.stopSpeaking()
            verify(exactly = 1) { session.stopSpeaking() }
            verify(exactly = 1) { owner.acquire() }
            // 화면은 Remote TTS 생명주기를 건드리지 않음.
            verify(exactly = 0) { remoteTtsRepository.release() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun geminiCommandsGoThroughUseCasesWithLanguage() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val session = mockk<VoiceSession>(relaxUnitFun = true)
            val owner = mockk<VoiceSessionOwner>(relaxUnitFun = true)
            val remoteTtsRepository = mockk<RemoteTtsRepository>(relaxUnitFun = true)
            every { remoteTtsRepository.options } returns options
            every { owner.acquire() } returns session
            every { session.state } returns MutableStateFlow(VoiceSessionState())
            val vm = VoiceViewModel(
                owner,
                SpeakRemoteTtsUseCase(remoteTtsRepository),
                StopRemoteTtsUseCase(remoteTtsRepository),
                GetRemoteTtsOptionsUseCase(remoteTtsRepository)
            )

            vm.speakGemini("请系好安全带！", "Kore", "cmn-CN")
            vm.stopGemini()

            verify(exactly = 1) { remoteTtsRepository.speak("请系好安全带！", "Kore", "cmn-CN") }
            verify(exactly = 1) { remoteTtsRepository.stop() }
            // 화면 선택지는 하드코딩이 아니라 구현 모듈의 카탈로그에서 옴.
            assertEquals(options, vm.geminiOptions)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
