package com.braveberry.voice.remote.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiAudioFormatTest {

    @Test
    fun acceptsAudioAt24kHzOrWithoutRate() {
        assertTrue(GeminiAudioFormat.isPlayableAudio("audio/pcm"))
        assertTrue(GeminiAudioFormat.isPlayableAudio("audio/L16;codec=pcm;rate=24000"))
    }

    @Test
    fun skipsNonAudioParts() {
        assertFalse(GeminiAudioFormat.isPlayableAudio("text/plain"))
    }

    @Test(expected = UnsupportedAudioFormatException::class)
    fun rejectsOtherSampleRate() {
        GeminiAudioFormat.isPlayableAudio("audio/L16;codec=pcm;rate=16000")
    }
}
