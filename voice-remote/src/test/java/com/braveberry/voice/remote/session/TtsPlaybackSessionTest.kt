package com.braveberry.voice.remote.session

import com.braveberry.voice.data.model.TtsPlaybackDataModel
import com.braveberry.voice.data.model.TtsPlaybackDataModel.DONE
import com.braveberry.voice.data.model.TtsPlaybackDataModel.SPEAKING
import com.braveberry.voice.data.model.TtsPlaybackDataModel.STOPPED
import com.braveberry.voice.remote.TtsSampleSentences
import com.braveberry.voice.remote.api.GeminiTtsApiService
import com.braveberry.voice.remote.mapper.firebaseError
import com.braveberry.voice.remote.model.TtsSpeechRequestDto
import com.braveberry.voice.remote.player.PcmAudioPlayer
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.UnknownException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.net.UnknownHostException
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsPlaybackSessionTest {
    private val api = mockk<GeminiTtsApiService>()
    private val player = mockk<PcmAudioPlayer>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val session = TtsPlaybackSession(api, player, testDispatcher)

    private fun speak(text: String, voice: String = "Fenrir", language: String = "en-US") =
        session.speak(flowOf(text), voice, language)

    /** relaxed player는 PCM Flow를 수집하지 않으므로 실제 합성이 일어나도록 직접 수집함. */
    private fun playerCollectsPcm() {
        coEvery { player.play(any(), any()) } coAnswers { firstArg<Flow<ByteArray>>().collect() }
    }

    private suspend fun speakFailure(): Throwable? = runCatching {
        speak("Hello there.", "Fenrir", "en-US").toList()
    }.exceptionOrNull()

    @Test
    fun speak_emitsSpeakingThenDone() = runTest(testDispatcher) {
        every { api.generateSpeech(TtsSpeechRequestDto("Hello", "Fenrir", "en-US")) } returns flowOf(byteArrayOf(1, 2, 3))
        playerCollectsPcm()

        val result = speak("Hello", "Fenrir", "en-US").toList()

        assertEquals(listOf(SPEAKING, DONE), result)
        coVerify { player.play(any(), 24_000) }
    }

    @Test
    fun speak_passesSelectedVoiceAndLanguage() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flowOf(byteArrayOf(1, 2))
        playerCollectsPcm()

        speak("こんにちは、テストです。", "Kore", "ja-JP").toList()

        verify(exactly = 1) { api.generateSpeech(match { it.voice == "Kore" && it.language == "ja-JP" }) }
    }

    @Test
    fun speak_sendsNativeSentenceWithMatchingLanguage() = runTest(testDispatcher) {
        playerCollectsPcm()

        listOf(
            TtsSampleSentences.Japanese.ARRIVAL to TtsSampleSentences.Japanese.LANGUAGE,
            TtsSampleSentences.Chinese.ARRIVAL to TtsSampleSentences.Chinese.LANGUAGE
        ).forEach { (sentence, language) ->
            val sent = mutableListOf<TtsSpeechRequestDto>()
            every { api.generateSpeech(capture(sent)) } returns flowOf(byteArrayOf(1, 2))

            speak(sentence, "Kore", language).toList()

            // 문장 단위로 나뉘어도 원문 그대로 같은 언어 코드와 함께 전달돼야 함.
            assertEquals(sentence, sent.joinToString("") { it.text })
            assertTrue(sent.all { it.language == language })
        }
    }

    @Test
    fun noResponse_timesOutAfterOneRetryAndFails() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flow { awaitCancellation() }
        playerCollectsPcm()

        val failure = speakFailure()

        // 오디오를 하나도 받지 못했으므로 1회 재시도 후 실패함.
        verify(exactly = 2) { api.generateSpeech(any()) }
        assertTrue(failure is IOException)
    }

    @Test
    fun stallAfterFirstChunk_failsWithoutRetry() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flow {
            emit(byteArrayOf(1, 2))
            awaitCancellation()
        }
        playerCollectsPcm()

        val failure = speakFailure()

        // 이미 재생된 오디오가 있으므로 같은 문장을 다시 읽지 않음.
        verify(exactly = 1) { api.generateSpeech(any()) }
        assertTrue(failure is IOException)
    }

    @Test
    fun slowResponseWithinLimit_completesNormally() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flow {
            delay(7_000)
            emit(byteArrayOf(1, 2))
        }
        playerCollectsPcm()

        val result = speak("Hello there.", "Fenrir", "en-US").toList()

        assertEquals(listOf(SPEAKING, DONE), result)
    }

    @Test
    fun authFailure_isNotRetriedAndBecomesSecurityException() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flow { throw firebaseError<InvalidAPIKeyException>() }
        playerCollectsPcm()

        val failure = speakFailure()

        verify(exactly = 1) { api.generateSpeech(any()) }
        assertTrue(failure is SecurityException)
    }

    @Test
    fun networkFailure_isRetriedOnceThenFails() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flow {
            throw firebaseError<UnknownException>(UnknownHostException("firebasevertexai.googleapis.com"))
        }
        playerCollectsPcm()

        val failure = speakFailure()

        verify(exactly = 2) { api.generateSpeech(any()) }
        assertTrue(failure is IOException)
    }

    @Test
    fun networkFailure_recoversOnRetry() = runTest(testDispatcher) {
        var calls = 0
        every { api.generateSpeech(any()) } answers {
            calls++
            if (calls == 1) flow { throw firebaseError<UnknownException>(IOException("reset")) }
            else flowOf(byteArrayOf(1, 2))
        }
        playerCollectsPcm()

        val result = speak("Hello there.", "Fenrir", "en-US").toList()

        assertEquals(listOf(SPEAKING, DONE), result)
    }

    @Test
    fun emptyAudio_isRetriedOnceThenFails() = runTest(testDispatcher) {
        // 응답은 정상 종료했지만 0바이트 청크만 옴.
        every { api.generateSpeech(any()) } returns flowOf(ByteArray(0))
        playerCollectsPcm()

        val failure = speakFailure()

        verify(exactly = 2) { api.generateSpeech(any()) }
        assertTrue(failure is IOException)
    }

    @Test
    fun emptyAudio_recoversOnRetry() = runTest(testDispatcher) {
        var calls = 0
        every { api.generateSpeech(any()) } answers {
            calls++
            if (calls == 1) flowOf() else flowOf(byteArrayOf(1, 2))
        }
        playerCollectsPcm()

        val result = speak("Hello there.", "Fenrir", "en-US").toList()

        assertEquals(listOf(SPEAKING, DONE), result)
    }

    @Test
    fun outputFailure_isPropagatedAsIOException() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flowOf(byteArrayOf(1, 2))
        coEvery { player.play(any(), any()) } throws IOException("AudioTrack.write failed")

        assertTrue(speakFailure() is IOException)
    }

    @Test
    fun newRequest_replacesActivePlayback() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flowOf(byteArrayOf(1, 2))
        var plays = 0
        // 첫 재생은 끝나지 않고 진행 중이어야 두 번째 요청이 교체를 거침.
        coEvery { player.play(any(), any()) } coAnswers {
            if (++plays == 1) awaitCancellation() else firstArg<Flow<ByteArray>>().collect()
        }

        val first = collectInBackground("First")
        val second = speak("Second", "Fenrir", "en-US").toList()
        advanceUntilIdle()

        assertEquals(listOf(SPEAKING, STOPPED), first)
        assertEquals(listOf(SPEAKING, DONE), second)
        verify(atLeast = 1) { player.stop() }
    }

    @Test
    fun stopThrowsException_nextRequestStillPlays() = runTest(testDispatcher) {
        every { api.generateSpeech(any()) } returns flowOf(byteArrayOf(1, 2))
        every { player.stop() } throws RuntimeException("AudioTrack stop failure")
        var plays = 0
        coEvery { player.play(any(), any()) } coAnswers { if (++plays == 1) awaitCancellation() }

        val first = collectInBackground("First")
        // 진행 중인 First를 교체하는 과정에서 player.stop()이 예외를 던짐.
        val second = speak("Second", "Fenrir", "en-US").toList()
        advanceUntilIdle()

        coVerify(exactly = 2) { player.play(any(), any()) }      // Second가 버려지지 않음
        assertEquals(listOf(SPEAKING, STOPPED), first)
        assertEquals(listOf(SPEAKING, DONE), second)
    }

    @Test
    fun stuckPlayback_doesNotBlockNextRequestBeyondJoinTimeout() = runTest(testDispatcher) {
        var plays = 0
        // 블로킹 write처럼 취소에 반응하지 않는 재생을 흉내 냄.
        coEvery { player.play(any(), any()) } coAnswers {
            if (++plays == 1) withContext(NonCancellable) { delay(60_000) }
        }

        collectInBackground("First")
        val secondJob = launch { speak("Second", "Fenrir", "en-US").collect() }
        advanceTimeBy(1_100)
        runCurrent()

        // First가 아직 끝나지 않았지만 1초 제한 후 Second가 시작됨.
        coVerify(exactly = 2) { player.play(any(), any()) }
        secondJob.cancel()
    }

    @Test
    fun cancellingCollection_stopsPlayback() = runTest(testDispatcher) {
        coEvery { player.play(any(), any()) } coAnswers { awaitCancellation() }

        val job = launch { speak("Hello there.", "Fenrir", "en-US").collect() }
        runCurrent()
        job.cancel()
        advanceUntilIdle()

        // 화면이 수집을 취소하면 블로킹 중인 출력도 깨워서 멈춰야 함.
        verify(atLeast = 1) { player.stop() }
    }

    /** 요청을 백그라운드에서 수집하고, 재생이 시작된 상태까지 진행시킴. */
    private fun TestScope.collectInBackground(text: String): List<TtsPlaybackDataModel> {
        val collected = mutableListOf<TtsPlaybackDataModel>()
        launch { speak(text, "Fenrir", "en-US").toList(collected) }
        runCurrent()
        return collected
    }
}
