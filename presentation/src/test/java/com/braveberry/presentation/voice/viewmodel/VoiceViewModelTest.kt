package com.braveberry.presentation.voice.viewmodel

import com.braveberry.domain.model.voice.PlaybackPhase
import com.braveberry.domain.model.voice.PlaybackPlan
import com.braveberry.domain.model.voice.VoiceSessionState
import com.braveberry.domain.model.voice.VoiceTurn
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
    @Test fun reopenedScreenObservesCurrentTurnAndForwardsStop() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val active = VoiceTurn("hello", "eunwoo", "A")
            val state = MutableStateFlow(VoiceSessionState(playback = PlaybackPlan(PlaybackPhase.Playing(active))))
            val session = mockk<VoiceSession>(relaxUnitFun = true)
            val owner = mockk<VoiceSessionOwner>(relaxUnitFun = true)
            every { owner.acquire() } returns session
            every { session.state } returns state
            val vm = VoiceViewModel(owner)
            assertEquals("A", vm.container.stateFlow.value.previewTurn)
            state.value = state.value.copy(playback = PlaybackPlan(PlaybackPhase.Playing(active.copy(role = "B"))))
            withTimeout(5_000) { vm.container.stateFlow.first { it.previewTurn == "B" } }
            vm.stopSpeaking()
            verify(exactly = 1) { session.stopSpeaking() }
            verify(exactly = 1) { owner.acquire() }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
