package com.braveberry.voice.remote

import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.voice.remote.api.GeminiTtsClient
import com.braveberry.voice.remote.impl.RemoteTtsRepositoryImpl
import com.braveberry.voice.remote.model.GeminiSpeakerCatalog
import com.braveberry.voice.remote.player.PcmAudioPlayer
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.UnknownException
import com.braveberry.voice.remote.api.firebaseError
import java.io.IOException
import java.net.UnknownHostException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteTtsRepositoryImplTest {
    private val client = mockk<GeminiTtsClient>(relaxed = true)
    private val player = mockk<PcmAudioPlayer>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: RemoteTtsRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        Dispatchers.setMain(testDispatcher)
        repository = RemoteTtsRepositoryImpl(client, player, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun options_comeFromCatalog() {
        assertEquals(GeminiSpeakerCatalog.speakers, repository.options.speakers)
        assertEquals(GeminiSpeakerCatalog.defaultSpeaker, repository.options.defaultSpeaker)
        assertEquals(GeminiSpeakerCatalog.supportedLanguages, repository.options.languages)
    }

    @Test
    fun speak_whenReady_callsClientAndPlayer() = runTest {
        coEvery { client.generateSpeechStream("Hello", "Fenrir", "en-US") } returns flowOf(byteArrayOf(1, 2, 3))

        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("Hello", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        assertTrue(collectedEvents.contains(VoiceEvent.Speaking))
        assertTrue(collectedEvents.contains(VoiceEvent.SpeechDone))
        coVerify { player.playStream(any()) }
        job.cancel()
    }

    @Test
    fun speak_passesSelectedLanguageToClient() = runTest {
        coEvery { client.generateSpeechStream(any(), any(), any()) } returns flowOf(byteArrayOf(1, 2))
        // relaxed player는 PCM Flow를 수집하지 않으므로 실제 합성이 일어나도록 직접 수집함.
        coEvery { player.playStream(any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }

        repository.speak("こんにちは、テストです。", "Kore", "ja-JP")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { client.generateSpeechStream(any(), "Kore", "ja-JP") }
    }

    @Test
    fun speak_sendsNativeSentenceWithMatchingLanguage() = runTest {
        coEvery { client.generateSpeechStream(any(), any(), any()) } returns flowOf(byteArrayOf(1, 2))
        coEvery { player.playStream(any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }

        listOf(
            TtsSampleSentences.Japanese.ARRIVAL to TtsSampleSentences.Japanese.LANGUAGE,
            TtsSampleSentences.Chinese.ARRIVAL to TtsSampleSentences.Chinese.LANGUAGE
        ).forEach { (sentence, language) ->
            val sent = mutableListOf<String>()
            coEvery { client.generateSpeechStream(capture(sent), any(), any()) } returns flowOf(byteArrayOf(1, 2))

            repository.speak(sentence, "Kore", language)
            testScheduler.advanceUntilIdle()

            // Gemini는 텍스트로 언어를 판단하므로, 문장 단위로 나뉘어도 원문 그대로 같은 언어 코드와 함께 전달돼야 함.
            assertEquals(sentence, sent.joinToString(""))
            coVerify(atLeast = 1) { client.generateSpeechStream(any(), "Kore", language) }
        }
    }

    @Test
    fun speak_withUnsupportedLanguage_failsWithoutPlaying() = runTest {
        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("안녕하세요", "Kore", "ko-KR")
        testScheduler.advanceUntilIdle()

        assertTrue(collectedEvents.contains(
            VoiceEvent.Failed(VoiceEvent.Engine.TTS, "UNSUPPORTED_LANGUAGE: ko-KR")
        ))
        coVerify(exactly = 0) { player.playStream(any()) }
        coVerify(exactly = 0) { client.generateSpeechStream(any(), any(), any()) }
        job.cancel()
    }

    @Test
    fun noResponse_timesOutAfterOneRetryAndReportsFailure() = runTest {
        coEvery { client.generateSpeechStream(any(), any(), any()) } returns flow { awaitCancellation() }
        coEvery { player.playStream(any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("Hello there.", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        // 오디오를 하나도 받지 못했으므로 1회 재시도 후 실패함.
        coVerify(exactly = 2) { client.generateSpeechStream(any(), any(), any()) }
        assertTrue(collectedEvents.contains(
            VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")
        ))
        assertFalse(collectedEvents.contains(VoiceEvent.SpeechDone))
        job.cancel()
    }

    @Test
    fun stallAfterFirstChunk_failsWithoutRetry() = runTest {
        coEvery { client.generateSpeechStream(any(), any(), any()) } returns flow {
            emit(byteArrayOf(1, 2))
            awaitCancellation()
        }
        coEvery { player.playStream(any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("Hello there.", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        // 이미 재생된 오디오가 있으므로 같은 문장을 다시 읽지 않음.
        coVerify(exactly = 1) { client.generateSpeechStream(any(), any(), any()) }
        assertTrue(collectedEvents.contains(
            VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")
        ))
        job.cancel()
    }

    @Test
    fun slowResponseWithinLimit_completesNormally() = runTest {
        coEvery { client.generateSpeechStream(any(), any(), any()) } returns flow {
            delay(7_000)
            emit(byteArrayOf(1, 2))
        }
        coEvery { player.playStream(any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("Hello there.", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        assertTrue(collectedEvents.contains(VoiceEvent.SpeechDone))
        assertFalse(collectedEvents.any { it is VoiceEvent.Failed })
        job.cancel()
    }

    @Test
    fun authFailure_isNotRetried() = runTest {
        coEvery { client.generateSpeechStream(any(), any(), any()) } returns flow {
            throw firebaseError<InvalidAPIKeyException>()
        }
        val events = speakAndCollect()

        coVerify(exactly = 1) { client.generateSpeechStream(any(), any(), any()) }
        assertTrue(events.contains(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")))
    }

    @Test
    fun networkFailure_isRetriedOnceThenReported() = runTest {
        coEvery { client.generateSpeechStream(any(), any(), any()) } returns flow {
            throw firebaseError<UnknownException>(UnknownHostException("firebasevertexai.googleapis.com"))
        }
        val events = speakAndCollect()

        coVerify(exactly = 2) { client.generateSpeechStream(any(), any(), any()) }
        assertTrue(events.contains(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")))
    }

    @Test
    fun networkFailure_recoversOnRetry() = runTest {
        var calls = 0
        coEvery { client.generateSpeechStream(any(), any(), any()) } answers {
            calls++
            if (calls == 1) flow { throw firebaseError<UnknownException>(IOException("reset")) }
            else flowOf(byteArrayOf(1, 2))
        }
        val events = speakAndCollect()

        assertTrue(events.contains(VoiceEvent.SpeechDone))
        assertFalse(events.any { it is VoiceEvent.Failed })
    }

    @Test
    fun emptyAudio_isRetriedOnceThenReported() = runTest {
        // 응답은 정상 종료했지만 0바이트 청크만 옴.
        coEvery { client.generateSpeechStream(any(), any(), any()) } returns flowOf(ByteArray(0))
        val events = speakAndCollect()

        coVerify(exactly = 2) { client.generateSpeechStream(any(), any(), any()) }
        assertTrue(events.contains(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")))
        assertFalse(events.contains(VoiceEvent.SpeechDone))
    }

    @Test
    fun emptyAudio_recoversOnRetry() = runTest {
        var calls = 0
        coEvery { client.generateSpeechStream(any(), any(), any()) } answers {
            calls++
            if (calls == 1) flowOf() else flowOf(byteArrayOf(1, 2))
        }
        val events = speakAndCollect()

        assertTrue(events.contains(VoiceEvent.SpeechDone))
        assertFalse(events.any { it is VoiceEvent.Failed })
    }

    /** player가 PCM을 실제로 수집하게 한 뒤 한 문장을 재생하고 발생한 이벤트를 반환함. */
    private suspend fun kotlinx.coroutines.test.TestScope.speakAndCollect(): List<VoiceEvent> {
        coEvery { player.playStream(any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }
        repository.speak("Hello there.", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()
        job.cancel()
        return collectedEvents
    }

    @Test
    fun stopThrowsException_runLoopSurvivesAndProcessesNextSpeak() = runTest {
        every { player.stop() } throws RuntimeException("AudioTrack stop failure")
        var plays = 0
        // 첫 재생은 끝나지 않고 진행 중이어야 두 번째 speak가 stopPlayback()을 거침.
        coEvery { player.playStream(any()) } coAnswers { if (++plays == 1) awaitCancellation() }

        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("First", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()
        // 진행 중인 First를 끊는 과정에서 player.stop()이 예외를 던짐.
        repository.speak("Second", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { player.stop() }
        coVerify(exactly = 2) { player.playStream(any()) }        // Second가 버려지지 않음
        assertEquals(2, collectedEvents.count { it == VoiceEvent.Speaking })
        assertTrue(collectedEvents.contains(VoiceEvent.SpeechStopped))
        assertTrue(collectedEvents.contains(VoiceEvent.SpeechDone))
        assertFalse(collectedEvents.any { it is VoiceEvent.Failed })
        job.cancel()
    }

    @Test
    fun stuckPlayback_doesNotBlockNextSpeakBeyondJoinTimeout() = runTest {
        var plays = 0
        // 블로킹 write처럼 취소에 반응하지 않는 재생을 흉내 냄.
        coEvery { player.playStream(any()) } coAnswers {
            if (++plays == 1) withContext(NonCancellable) { delay(60_000) }
        }
        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("First", "Fenrir", "en-US")
        testScheduler.runCurrent()
        repository.speak("Second", "Fenrir", "en-US")
        testScheduler.advanceTimeBy(1_100)
        testScheduler.runCurrent()

        // First가 아직 끝나지 않았지만 1초 제한 후 Second가 시작됨.
        coVerify(exactly = 2) { player.playStream(any()) }
        assertTrue(collectedEvents.contains(VoiceEvent.SpeechStopped))
        job.cancel()
    }
}
