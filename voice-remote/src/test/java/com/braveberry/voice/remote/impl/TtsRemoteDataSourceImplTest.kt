package com.braveberry.voice.remote.impl

import com.braveberry.voice.data.model.TtsPlaybackDataModel.DONE
import com.braveberry.voice.data.model.TtsPlaybackDataModel.SPEAKING
import com.braveberry.voice.remote.model.GeminiSpeakerCatalog
import com.braveberry.voice.remote.session.TtsPlaybackSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsRemoteDataSourceImplTest {
    private val session = mockk<TtsPlaybackSession>()
    private val dataSource = TtsRemoteDataSourceImpl(session)

    @Test
    fun options_comeFromCatalog() {
        assertEquals(GeminiSpeakerCatalog.speakers, dataSource.options.voices)
        assertEquals(GeminiSpeakerCatalog.defaultSpeaker, dataSource.options.defaultVoice)
        assertEquals(GeminiSpeakerCatalog.supportedLanguages, dataSource.options.languages)
    }

    @Test
    fun speak_delegatesTextToSessionAndForwardsPlayback() = runTest {
        val tokens = slot<Flow<String>>()
        every { session.speak(capture(tokens), "Kore", "ja-JP") } returns flowOf(SPEAKING, DONE)

        val result = dataSource.speak("こんにちは。", "Kore", "ja-JP").toList()

        assertEquals(listOf(SPEAKING, DONE), result)
        assertEquals(listOf("こんにちは。"), tokens.captured.toList())
    }

    @Test
    fun speak_withUnknownVoice_fallsBackToDefaultVoice() = runTest {
        every { session.speak(any(), any(), any()) } returns flowOf(DONE)

        dataSource.speak("Hello there.", "seoyoon", "en-US").toList()

        verify(exactly = 1) { session.speak(any(), GeminiSpeakerCatalog.defaultSpeaker, "en-US") }
    }

    @Test
    fun speak_withUnsupportedLanguage_failsWithoutStartingSession() = runTest {
        val failure = runCatching { dataSource.speak("안녕하세요", "Kore", "ko-KR").toList() }.exceptionOrNull()

        assertTrue(failure is UnsupportedOperationException)
        verify(exactly = 0) { session.speak(any(), any(), any()) }
    }
}
