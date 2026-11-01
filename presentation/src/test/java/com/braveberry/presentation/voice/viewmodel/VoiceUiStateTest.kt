package com.braveberry.presentation.voice.viewmodel

import com.braveberry.domain.model.voice.VoiceEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceUiStateTest {
    @Test fun transcriptionPreservesIntermediateAndFinalResult() {
        val ready = VoiceUiState().applyEvent(VoiceEvent.SttReady)
        assertEquals(EngineStatus.READY, ready.sttStatus)
        val listening = ready.applyEvent(VoiceEvent.Listening)
        assertTrue(listening.listening)
        val partial = listening.applyEvent(VoiceEvent.Transcription("안녕", false))
        assertFalse(partial.transcriptCompleted)
        val completed = partial.applyEvent(VoiceEvent.Transcription("안녕하세요", true))
        assertEquals("안녕하세요", completed.transcript)
        assertTrue(completed.transcriptCompleted)
        assertFalse(completed.applyEvent(VoiceEvent.ListeningStopped).listening)
    }

    @Test fun ttsErrorDoesNotResetSttReadiness() {
        val ready = VoiceUiState().applyEvent(VoiceEvent.SttReady)
        val failed = ready.applyEvent(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "SERVICE_ERROR"))
        assertEquals(EngineStatus.READY, failed.sttStatus)
        assertEquals(EngineStatus.ERROR, failed.ttsStatus)
        assertEquals("TTS: SERVICE_ERROR", failed.error)
    }
}
