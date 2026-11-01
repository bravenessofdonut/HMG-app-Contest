package com.braveberry.domain.model.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceIndependentErrorTest {
    @Test fun sttInitializationDoesNotEraseTtsFailure() {
        val failed = VoiceSessionState()
            .applyEvent(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "AUDIO_ERROR"))
            .applyEvent(VoiceEvent.SttReady)
        assertEquals("AUDIO_ERROR", failed.ttsError)
        assertEquals(null, failed.sttError)
    }

    @Test fun ttsSuccessDoesNotEraseSttFailure() {
        val failed = VoiceSessionState()
            .applyEvent(VoiceEvent.Failed(VoiceEvent.Engine.STT, "RECOGNITION_ERROR"))
            .applyEvent(VoiceEvent.TtsReady)
        assertEquals("RECOGNITION_ERROR", failed.sttError)
        assertEquals(null, failed.ttsError)
    }

    @Test fun speakerMetadataIsRetainedForUiGrouping() {
        val ready = VoiceSessionState().applyEvent(VoiceEvent.SpeakerSupport(
            VoiceEvent.SpeakerKind.MANUAL_STRING,
            listOf("seowoon", "iris"),
            "java.lang.String",
            mapOf("seowoon" to "ko-KR", "iris" to "es-US")
        ))
        assertEquals("es-US", ready.speakerLanguageTags["iris"])
        assertEquals(listOf("seowoon", "iris"), ready.availableSpeakers)
    }
}
