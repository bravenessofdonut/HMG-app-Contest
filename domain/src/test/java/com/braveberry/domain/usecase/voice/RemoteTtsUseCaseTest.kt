package com.braveberry.domain.usecase.voice

import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.domain.repository.RemoteTtsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RemoteTtsUseCaseTest {

    @Test fun speakDelegatesTextVoiceAndLanguageUnmodified() {
        val repository = FakeRemoteTtsRepository()
        SpeakRemoteTtsUseCase(repository)("まもなく目的地に到着します。", "Kore", "ja-JP")
        assertEquals(listOf(Triple("まもなく目的地に到着します。", "Kore", "ja-JP")), repository.spoken)
    }

    @Test fun stopDelegatesOnce() {
        val repository = FakeRemoteTtsRepository()
        StopRemoteTtsUseCase(repository)()
        assertEquals(1, repository.stopCount)
    }

    @Test fun optionsAreProvidedByRepository() {
        val repository = FakeRemoteTtsRepository()
        assertSame(repository.options, GetRemoteTtsOptionsUseCase(repository)())
    }

    @Test fun useCasesDoNotTouchLifecycle() {
        // 생명주기(release)는 DI가 소유함. UseCase가 대신 호출하면 안 됨.
        val repository = FakeRemoteTtsRepository()
        SpeakRemoteTtsUseCase(repository)("Hello.", "Fenrir", "en-US")
        StopRemoteTtsUseCase(repository)()
        assertEquals(0, repository.lifecycleCalls)
    }

    private class FakeRemoteTtsRepository : RemoteTtsRepository {
        val spoken = mutableListOf<Triple<String, String, String>>()
        var stopCount = 0
        var lifecycleCalls = 0

        override val events: Flow<VoiceEvent> = emptyFlow()
        override val options = RemoteTtsOptions(listOf("Kore"), "Kore", listOf("ja-JP"))
        override fun speak(text: String, voiceName: String, language: String) {
            spoken += Triple(text, voiceName, language)
        }
        override fun stop() { stopCount++ }
        override fun release() { lifecycleCalls++ }
        override fun speakStream(tokens: Flow<String>, voiceName: String, language: String) =
            error("not used")
    }
}
