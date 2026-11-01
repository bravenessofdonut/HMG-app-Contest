package com.braveberry.presentation.voice.viewmodel

import com.braveberry.domain.model.voice.VoiceEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceIndependentErrorTest {
    @Test fun sttInitializationDoesNotEraseTtsFailure() {
        val failed = VoiceUiState()
            .applyEvent(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "AUDIO_ERROR"))
            .applyEvent(VoiceEvent.SttReady)
        assertEquals("TTS: AUDIO_ERROR", failed.ttsError)
        assertEquals(null, failed.sttError)
    }

    @Test fun ttsSuccessDoesNotEraseSttFailure() {
        val failed = VoiceUiState()
            .applyEvent(VoiceEvent.Failed(VoiceEvent.Engine.STT, "RECOGNITION_ERROR"))
            .applyEvent(VoiceEvent.TtsReady)
        assertEquals("STT: RECOGNITION_ERROR", failed.sttError)
        assertEquals(null, failed.ttsError)
    }

    @Test fun speakerMetadataIsRetainedForUiGrouping() {
        val ready = VoiceUiState().applyEvent(VoiceEvent.SpeakerSupport(
            VoiceEvent.SpeakerKind.MANUAL_STRING,
            listOf("seowoon", "iris"),
            "java.lang.String",
            mapOf("seowoon" to "ko-KR", "iris" to "es-US")
        ))
        assertEquals("es-US", ready.speakerLanguageTags["iris"])
        assertEquals(listOf("seowoon", "iris"), ready.availableSpeakers)
    }
}
