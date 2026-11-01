package com.braveberry.voice.data.impl

import com.braveberry.domain.model.voice.EngineStatus
import com.braveberry.domain.model.voice.PlaybackPhase
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.domain.repository.VoiceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceSessionImplTest {
    @Test fun screenExitDoesNotInterruptConfirmedTwoTurnPlayback() = runTest {
        val engine = FakeEngine()
        val session = VoiceSessionImpl({ engine }, backgroundScope)
        session.acquire()
        runCurrent()
        session.previewDialogue("first", "second", "eunwoo", "haeun")
        runCurrent()
        session.release()
        engine.send(VoiceEvent.Speaking)
        runCurrent()
        engine.send(VoiceEvent.SpeechDone)
        runCurrent()
        assertEquals(listOf("init", "speaker:eunwoo", "first", "speaker:haeun", "second"), engine.calls)
        engine.send(VoiceEvent.Speaking)
        engine.send(VoiceEvent.SpeechDone)
        runCurrent()
        assertEquals(1, engine.releases)
    }

    @Test fun missingStartCallbackFailsWithoutPlayingSecondTurn() = runTest {
        val engine = FakeEngine()
        val session = VoiceSessionImpl({ engine }, backgroundScope)
        session.acquire()
        runCurrent()
        session.previewDialogue("first", "second", "eunwoo", "haeun")
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        assertTrue(session.state.value.playback.phase is PlaybackPhase.Failed)
        assertTrue(session.state.value.disconnected)
        assertFalse(engine.calls.contains("second"))
        assertEquals(1, engine.releases)
    }

    @Test fun duplicateTtsReadyDoesNotCancelPlaybackTimeout() = runTest {
        val engine = FakeEngine()
        val session = VoiceSessionImpl({ engine }, backgroundScope)
        session.acquire()
        runCurrent()
        session.previewDialogue("first", "second", "eunwoo", "haeun")
        runCurrent()
        engine.send(VoiceEvent.Speaking)
        runCurrent()
        engine.send(VoiceEvent.TtsReady)
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()
        assertTrue(session.state.value.playback.phase is PlaybackPhase.Failed)
        assertFalse(engine.calls.contains("second"))
    }

    @Test fun stoppingWithoutAcknowledgementCannotLockSessionForever() = runTest {
        val engine = FakeEngine()
        val session = VoiceSessionImpl({ engine }, backgroundScope)
        session.acquire()
        runCurrent()
        session.speak("hello", "eunwoo")
        runCurrent()
        session.stopSpeaking()
        runCurrent()
        assertTrue(session.state.value.stopping)
        advanceTimeBy(3_001)
        runCurrent()
        assertTrue(session.state.value.disconnected)
        assertFalse(session.state.value.canSpeak)
        assertEquals(1, engine.releases)
    }

    @Test fun stoppingReplacesEngineSoOldConnectionCannotDriveNewPlayback() = runTest {
        val old = FakeEngine()
        val fresh = FakeEngine()
        val engines = ArrayDeque(listOf(old, fresh))
        val session = VoiceSessionImpl({ engines.removeFirst() }, backgroundScope)
        session.acquire()
        runCurrent()
        session.speak("hello", "eunwoo")
        runCurrent()
        old.send(VoiceEvent.Speaking)
        runCurrent()
        session.stopSpeaking()
        runCurrent()
        old.send(VoiceEvent.SpeechStopped)
        runCurrent()
        assertEquals(1, old.releases)
        assertEquals(EngineStatus.READY, session.state.value.ttsStatus)
        session.speak("new", "eunwoo")
        runCurrent()
        assertEquals(listOf("init", "speaker:eunwoo", "new"), fresh.calls)
        // 폐기된 연결에서 발생하는 뒤늦은 완료 콜백이 새 발화를 완료시키지 못함.
        old.send(VoiceEvent.SpeechDone)
        runCurrent()
        assertTrue(session.state.value.playback.phase is PlaybackPhase.Starting)
    }

    @Test fun nonKoreanSpeakerCannotEnterPodcastEvenWhenUiBypassed() = runTest {
        val engine = FakeEngine()
        val session = VoiceSessionImpl({ engine }, backgroundScope)
        session.acquire()
        runCurrent()
        session.previewDialogue("bonjour", "hello", "sabine", "eunwoo")
        runCurrent()
        assertEquals(listOf("init"), engine.calls)
    }

    @Test fun duplicateSttReadyDoesNotCancelRecognitionStartTimeout() = runTest {
        val engine = FakeEngine()
        val session = VoiceSessionImpl({ engine }, backgroundScope)
        session.acquire()
        runCurrent()
        session.startListening()
        runCurrent()
        engine.send(VoiceEvent.SttReady)
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals("STT_START_TIMEOUT", session.state.value.sttError)
    }

    private class FakeEngine : VoiceRepository {
        private val queue = Channel<VoiceEvent>(Channel.UNLIMITED)
        override val events = queue.receiveAsFlow()
        val calls = mutableListOf<String>()
        var releases = 0
        fun send(event: VoiceEvent) { queue.trySend(event).getOrThrow() }
        override fun initialize() {
            calls += "init"
            send(VoiceEvent.TtsReady)
            send(VoiceEvent.SttReady)
            send(VoiceEvent.SpeakerSupport(
                VoiceEvent.SpeakerKind.MANUAL_STRING,
                listOf("eunwoo", "haeun", "sabine"), "java.lang.String",
                mapOf("eunwoo" to "ko-KR", "haeun" to "ko-KR", "sabine" to "fr-CA")
            ))
        }
        override fun speak(text: String) { calls += text }
        override fun selectSpeaker(value: String): Boolean { calls += "speaker:$value"; return true }
        override fun stopSpeaking() { calls += "stop" }
        override fun startListening() = Unit
        override fun stopListening() = Unit
        override fun release() { releases++ }
    }
}
