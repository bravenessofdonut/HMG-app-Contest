package com.braveberry.voice.data.impl

import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.voice.data.datasource.VoiceDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceRepositoryImplTest {
    @Test fun delegatesCommandsAndDoesNotExposeSdkTypes() = runBlocking {
        val fake = FakeSource()
        val repository = VoiceRepositoryImpl(fake)
        repository.initialize()
        repository.speak("테스트")
        assertEquals(true, repository.selectSpeaker("VOICE_A"))
        repository.startListening()
        repository.stopListening()
        repository.stopSpeaking()
        repository.release()
        assertEquals(listOf("initialize", "speak:테스트", "speaker:VOICE_A", "listen", "stopListen", "stopSpeak", "release"), fake.calls)
        assertEquals(VoiceEvent.SttReady, repository.events.first())
    }

    private class FakeSource : VoiceDataSource {
        val calls = mutableListOf<String>()
        override val events: Flow<VoiceEvent> = flowOf(VoiceEvent.SttReady)
        override fun initialize() { calls += "initialize" }
        override fun speak(text: String) { calls += "speak:$text" }
        override fun selectSpeaker(value: String): Boolean { calls += "speaker:$value"; return true }
        override fun stopSpeaking() { calls += "stopSpeak" }
        override fun startListening() { calls += "listen" }
        override fun stopListening() { calls += "stopListen" }
        override fun release() { calls += "release" }
    }
}
