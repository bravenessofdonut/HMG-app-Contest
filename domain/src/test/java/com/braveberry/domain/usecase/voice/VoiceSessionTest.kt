package com.braveberry.domain.usecase.voice

import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.domain.repository.VoiceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceSessionTest {
    @Test fun leavingAndReenteringKeepsTheTurnWithoutRestartingTheEngine() = runTest {
        val engine = FakeEngine()
        val session = VoiceSession({ engine }, backgroundScope)
        session.attachScreen()
        runCurrent()
        session.previewDialogue("first", "second", "one", "two")
        session.detachScreen()
        assertEquals(Playback.Speaking("A"), ObservePlaybackUseCase(session)().first())
        engine.send(VoiceEvent.SpeechDone)
        runCurrent()
        session.attachScreen()
        assertEquals("B", session.state.value.previewTurn)
        assertEquals(listOf("init", "first", "second"), engine.calls)
        session.detachScreen()
        engine.send(VoiceEvent.SpeechDone)
        runCurrent()
        assertEquals(listOf("init", "first", "second", "release"), engine.calls)
    }

    @Test fun completionAfterStopDoesNotStartTheSecondTurn() = runTest {
        val engine = FakeEngine()
        val session = VoiceSession({ engine }, backgroundScope)
        session.attachScreen()
        runCurrent()
        session.previewDialogue("first", "second", "one", "two")
        session.stopSpeaking()
        engine.send(VoiceEvent.SpeechDone)
        runCurrent()
        assertEquals(Playback.Idle, ObservePlaybackUseCase(session)().first())
        assertEquals(listOf("init", "first", "stop"), engine.calls)
        session.detachScreen()
    }

    private class FakeEngine : VoiceRepository {
        private val queue = Channel<VoiceEvent>(8)
        override val events = queue.receiveAsFlow()
        val calls = mutableListOf<String>()
        fun send(event: VoiceEvent) { queue.trySend(event).getOrThrow() }
        override fun initialize() {
            calls += "init"
            send(VoiceEvent.TtsReady)
        }
        override fun selectSpeaker(value: String) = true
        override fun speak(text: String) { calls += text }
        override fun stopSpeaking() { calls += "stop" }
        override fun startListening() = Unit
        override fun stopListening() = Unit
        override fun release() {
            calls += "release"
            queue.close()
        }
    }
}
