package com.braveberry.domain.usecase.voice

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.RemoteTtsPlayback
import com.braveberry.domain.repository.RemoteTtsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RemoteTtsUseCaseTest {

    @Test fun speakDelegatesTextVoiceAndLanguageUnmodified() {
        val repository = FakeRemoteTtsRepository()
        SpeakRemoteTtsUseCase(repository)("まもなく目的地に到着します。", "Kore", "ja-JP")
        assertEquals(listOf(Triple("まもなく目的地に到着します。", "Kore", "ja-JP")), repository.spoken)
    }

    @Test fun speakReturnsRepositoryFlowUnmodified() {
        val repository = FakeRemoteTtsRepository()
        assertSame(repository.result, SpeakRemoteTtsUseCase(repository)("Hello.", "Fenrir", "en-US"))
    }

    @Test fun optionsAreProvidedByRepository() {
        val repository = FakeRemoteTtsRepository()
        assertSame(repository.options, GetRemoteTtsOptionsUseCase(repository)())
    }

    private class FakeRemoteTtsRepository : RemoteTtsRepository {
        val spoken = mutableListOf<Triple<String, String, String>>()
        val result: Flow<DataResource<RemoteTtsPlayback>> = flowOf(DataResource.success(RemoteTtsPlayback.DONE))

        override val options = RemoteTtsOptions(listOf("Kore"), "Kore", listOf("ja-JP"))
        override fun speak(text: String, voiceName: String, language: String): Flow<DataResource<RemoteTtsPlayback>> {
            spoken += Triple(text, voiceName, language)
            return result
        }
        override fun speakStream(tokens: Flow<String>, voiceName: String, language: String) =
            error("not used")
    }
}
