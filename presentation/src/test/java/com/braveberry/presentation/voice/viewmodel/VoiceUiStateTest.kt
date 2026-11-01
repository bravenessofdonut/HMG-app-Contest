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

    @Test fun speakerChoicesAreActualSdkValues() {
        val options = listOf("ONE", "TWO")
        val state = VoiceUiState().applyEvent(
            VoiceEvent.SpeakerSupport(VoiceEvent.SpeakerKind.ENUM_VALUES, options, "SpeakerType")
        )
        assertEquals(options, state.availableSpeakers)
        assertEquals(VoiceEvent.SpeakerKind.ENUM_VALUES, state.speakerKind)
        assertEquals("TWO", state.applyEvent(VoiceEvent.SpeakerRequestSent("TWO")).requestedSpeaker)
    }

    @Test fun sttDiagnosticsKeepFinalTranscriptAfterRecognitionEnds() {
        val listening = VoiceUiState().applyEvent(VoiceEvent.SttReady)
            .applyEvent(VoiceEvent.SttInputReady)
            .applyEvent(VoiceEvent.Listening)
            .applyEvent(VoiceEvent.SttRms(12.0))
            .applyEvent(VoiceEvent.Transcription("좋은 아침", true))
        val ended = listening.applyEvent(VoiceEvent.ListeningStopped)
        assertEquals("좋은 아침", ended.transcript)
        assertTrue(ended.transcriptCompleted)
        assertEquals(12.0, ended.sttRms!!, 0.0001)
        assertEquals(ListeningPhase.ENDED, ended.listeningPhase)
    }
}
