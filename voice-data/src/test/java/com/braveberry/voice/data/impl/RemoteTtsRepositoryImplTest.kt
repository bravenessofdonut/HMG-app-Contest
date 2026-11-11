package com.braveberry.voice.data.impl

import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.voice.data.TtsSampleSentences
import com.braveberry.voice.data.datasource.AudioOutputDataSource
import com.braveberry.voice.data.datasource.SpeechSynthesisDataSource
import java.io.IOException
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

/** SDK 없이 DataSource 계약만으로 재생 규칙을 검증함. 합성 SDK를 바꿔도 이 테스트는 그대로 유지돼야 함. */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteTtsRepositoryImplTest {
    private val synthesizer = mockk<SpeechSynthesisDataSource>(relaxed = true) {
        every { voices } returns listOf("Fenrir", "Kore")
        every { defaultVoice } returns "Fenrir"
        every { languages } returns listOf("en-US", "ja-JP", "cmn-CN")
        every { sampleRateHz } returns 24_000
    }
    private val output = mockk<AudioOutputDataSource>(relaxed = true)
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
        repository = RemoteTtsRepositoryImpl(synthesizer, output, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun options_comeFromDataSource() {
        assertEquals(listOf("Fenrir", "Kore"), repository.options.speakers)
        assertEquals("Fenrir", repository.options.defaultSpeaker)
        assertEquals(listOf("en-US", "ja-JP", "cmn-CN"), repository.options.languages)
    }

    @Test
    fun speak_whenReady_callsSynthesizerAndOutput() = runTest {
        coEvery { synthesizer.synthesize("Hello", "Fenrir", "en-US") } returns flowOf(byteArrayOf(1, 2, 3))

        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("Hello", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        assertTrue(collectedEvents.contains(VoiceEvent.Speaking))
        assertTrue(collectedEvents.contains(VoiceEvent.SpeechDone))
        coVerify { output.play(any(), 24_000) }
        job.cancel()
    }

    @Test
    fun speak_passesSelectedLanguageToSynthesizer() = runTest {
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flowOf(byteArrayOf(1, 2))
        // relaxed output은 PCM Flow를 수집하지 않으므로 실제 합성이 일어나도록 직접 수집함.
        coEvery { output.play(any(), any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }

        repository.speak("こんにちは、テストです。", "Kore", "ja-JP")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { synthesizer.synthesize(any(), "Kore", "ja-JP") }
    }

    @Test
    fun speak_withUnknownVoice_fallsBackToDefaultVoice() = runTest {
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flowOf(byteArrayOf(1, 2))
        coEvery { output.play(any(), any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }

        repository.speak("Hello there.", "seoyoon", "en-US")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { synthesizer.synthesize(any(), "Fenrir", "en-US") }
    }

    @Test
    fun speak_sendsNativeSentenceWithMatchingLanguage() = runTest {
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flowOf(byteArrayOf(1, 2))
        coEvery { output.play(any(), any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }

        listOf(
            TtsSampleSentences.Japanese.ARRIVAL to TtsSampleSentences.Japanese.LANGUAGE,
            TtsSampleSentences.Chinese.ARRIVAL to TtsSampleSentences.Chinese.LANGUAGE
        ).forEach { (sentence, language) ->
            val sent = mutableListOf<String>()
            coEvery { synthesizer.synthesize(capture(sent), any(), any()) } returns flowOf(byteArrayOf(1, 2))

            repository.speak(sentence, "Kore", language)
            testScheduler.advanceUntilIdle()

            // 문장 단위로 나뉘어도 원문 그대로 같은 언어 코드와 함께 전달돼야 함.
            assertEquals(sentence, sent.joinToString(""))
            coVerify(atLeast = 1) { synthesizer.synthesize(any(), "Kore", language) }
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
        coVerify(exactly = 0) { output.play(any(), any()) }
        coVerify(exactly = 0) { synthesizer.synthesize(any(), any(), any()) }
        job.cancel()
    }

    @Test
    fun noResponse_timesOutAfterOneRetryAndReportsFailure() = runTest {
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flow { awaitCancellation() }
        coEvery { output.play(any(), any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("Hello there.", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        // 오디오를 하나도 받지 못했으므로 1회 재시도 후 실패함.
        coVerify(exactly = 2) { synthesizer.synthesize(any(), any(), any()) }
        assertTrue(collectedEvents.contains(
            VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")
        ))
        assertFalse(collectedEvents.contains(VoiceEvent.SpeechDone))
        job.cancel()
    }

    @Test
    fun stallAfterFirstChunk_failsWithoutRetry() = runTest {
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flow {
            emit(byteArrayOf(1, 2))
            awaitCancellation()
        }
        coEvery { output.play(any(), any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("Hello there.", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        // 이미 재생된 오디오가 있으므로 같은 문장을 다시 읽지 않음.
        coVerify(exactly = 1) { synthesizer.synthesize(any(), any(), any()) }
        assertTrue(collectedEvents.contains(
            VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")
        ))
        job.cancel()
    }

    @Test
    fun slowResponseWithinLimit_completesNormally() = runTest {
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flow {
            delay(7_000)
            emit(byteArrayOf(1, 2))
        }
        coEvery { output.play(any(), any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
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
    fun nonTransientFailure_isNotRetried() = runTest {
        // 계약상 IOException이 아닌 실패는 다시 요청해도 회복되지 않음 (인증, 할당량 등).
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flow {
            throw IllegalStateException("auth")
        }
        val events = speakAndCollect()

        coVerify(exactly = 1) { synthesizer.synthesize(any(), any(), any()) }
        assertTrue(events.contains(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")))
    }

    @Test
    fun transientFailure_isRetriedOnceThenReported() = runTest {
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flow {
            throw IOException("network")
        }
        val events = speakAndCollect()

        coVerify(exactly = 2) { synthesizer.synthesize(any(), any(), any()) }
        assertTrue(events.contains(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")))
    }

    @Test
    fun transientFailure_recoversOnRetry() = runTest {
        var calls = 0
        coEvery { synthesizer.synthesize(any(), any(), any()) } answers {
            calls++
            if (calls == 1) flow { throw IOException("reset") }
            else flowOf(byteArrayOf(1, 2))
        }
        val events = speakAndCollect()

        assertTrue(events.contains(VoiceEvent.SpeechDone))
        assertFalse(events.any { it is VoiceEvent.Failed })
    }

    @Test
    fun emptyAudio_isRetriedOnceThenReported() = runTest {
        // 응답은 정상 종료했지만 0바이트 청크만 옴.
        coEvery { synthesizer.synthesize(any(), any(), any()) } returns flowOf(ByteArray(0))
        val events = speakAndCollect()

        coVerify(exactly = 2) { synthesizer.synthesize(any(), any(), any()) }
        assertTrue(events.contains(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SYNTHESIS_FAILED")))
        assertFalse(events.contains(VoiceEvent.SpeechDone))
    }

    @Test
    fun emptyAudio_recoversOnRetry() = runTest {
        var calls = 0
        coEvery { synthesizer.synthesize(any(), any(), any()) } answers {
            calls++
            if (calls == 1) flowOf() else flowOf(byteArrayOf(1, 2))
        }
        val events = speakAndCollect()

        assertTrue(events.contains(VoiceEvent.SpeechDone))
        assertFalse(events.any { it is VoiceEvent.Failed })
    }

    /** output이 PCM을 실제로 수집하게 한 뒤 한 문장을 재생하고 발생한 이벤트를 반환함. */
    private suspend fun kotlinx.coroutines.test.TestScope.speakAndCollect(): List<VoiceEvent> {
        coEvery { output.play(any(), any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
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
        every { output.stop() } throws RuntimeException("AudioTrack stop failure")
        var plays = 0
        // 첫 재생은 끝나지 않고 진행 중이어야 두 번째 speak가 stopPlayback()을 거침.
        coEvery { output.play(any(), any()) } coAnswers { if (++plays == 1) awaitCancellation() }

        val collectedEvents = mutableListOf<VoiceEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.toList(collectedEvents)
        }

        repository.speak("First", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()
        // 진행 중인 First를 끊는 과정에서 output.stop()이 예외를 던짐.
        repository.speak("Second", "Fenrir", "en-US")
        testScheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { output.stop() }
        coVerify(exactly = 2) { output.play(any(), any()) }        // Second가 버려지지 않음
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
        coEvery { output.play(any(), any()) } coAnswers {
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
        coVerify(exactly = 2) { output.play(any(), any()) }
        assertTrue(collectedEvents.contains(VoiceEvent.SpeechStopped))
        job.cancel()
    }
}
