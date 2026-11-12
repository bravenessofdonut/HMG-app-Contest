package com.braveberry.voice.data.impl

import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.domain.model.voice.RemoteTtsPlayback
import com.braveberry.voice.data.datasource.TtsRemoteDataSource
import com.braveberry.voice.data.model.TtsOptionsDataModel
import com.braveberry.voice.data.model.TtsPlaybackDataModel
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteTtsRepositoryImplTest {
    private val datasource = mockk<TtsRemoteDataSource>()
    private val repository = RemoteTtsRepositoryImpl(datasource)

    @Test
    fun speak_emitsLoadingThenMappedPlayback() = runTest {
        every { datasource.speak("Hello", "Kore", "en-US") } returns
            flowOf(TtsPlaybackDataModel.SPEAKING, TtsPlaybackDataModel.DONE)

        val result = repository.speak("Hello", "Kore", "en-US").toList()

        assertTrue(result.first() is DataResource.Loading)
        assertEquals(
            listOf(RemoteTtsPlayback.SPEAKING, RemoteTtsPlayback.DONE),
            result.filterIsInstance<DataResource.Success<RemoteTtsPlayback>>().map { it.data }
        )
    }

    @Test
    fun speakStream_mapsStoppedPlayback() = runTest {
        val tokens = flowOf("Hello")
        every { datasource.speakStream(tokens, "Kore", "en-US") } returns flowOf(TtsPlaybackDataModel.STOPPED)

        val result = repository.speakStream(tokens, "Kore", "en-US").toList()

        assertEquals(RemoteTtsPlayback.STOPPED, (result.last() as DataResource.Success).data)
    }

    @Test
    fun failures_areClassifiedWithCausePreserved() = runTest {
        listOf(
            IOException("network") to ResourceFailure.ServiceUnavailable::class,
            SecurityException("auth") to ResourceFailure.PermissionRequired::class,
            UnsupportedOperationException("ko-KR") to ResourceFailure.Unsupported::class
        ).forEach { (cause, expected) ->
            every { datasource.speak(any(), any(), any()) } returns flow { throw cause }

            val error = repository.speak("Hello", "Kore", "en-US").toList().last() as DataResource.Error

            assertEquals(expected, error.failure::class)
            assertEquals(cause, error.failure.cause)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun unknownFailure_isPropagated() = runTest {
        every { datasource.speak(any(), any(), any()) } returns flow { throw IllegalStateException("bug") }

        repository.speak("Hello", "Kore", "en-US").toList()
    }

    @Test
    fun options_areMappedToDomain() {
        every { datasource.options } returns TtsOptionsDataModel(listOf("Kore", "Fenrir"), "Fenrir", listOf("en-US"))

        val options = repository.options

        assertEquals(listOf("Kore", "Fenrir"), options.speakers)
        assertEquals("Fenrir", options.defaultSpeaker)
        assertEquals(listOf("en-US"), options.languages)
    }
}
