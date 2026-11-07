package com.braveberry.voice.local.source

import com.braveberry.domain.model.voice.VoiceEvent
import java.io.IOException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceEventInboxTest {
    @Test fun meterBurstCannotDisplaceCompletion() = runTest {
        val inbox = VoiceEventInbox(capacity = 1)
        repeat(100) { inbox.offer(VoiceEvent.SttRms(it.toDouble())) }
        inbox.offer(VoiceEvent.SpeechDone)
        inbox.close()
        val delivered = inbox.events.toList()
        assertEquals(2, delivered.size)
        assertTrue(VoiceEvent.SpeechDone in delivered)
        assertTrue(VoiceEvent.SttRms(99.0) in delivered)
    }

    @Test fun controlOverflowIsAnExplicitDisconnect() = runTest {
        val inbox = VoiceEventInbox(capacity = 1)
        inbox.offer(VoiceEvent.Speaking)
        inbox.offer(VoiceEvent.SpeechDone)
        val failure = runCatching { inbox.events.toList() }.exceptionOrNull()
        assertTrue(failure is IOException)
    }
}
