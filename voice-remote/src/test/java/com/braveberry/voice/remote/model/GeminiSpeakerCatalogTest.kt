package com.braveberry.voice.remote.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiSpeakerCatalogTest {

    @Test
    fun defaultSpeakerIsSupported() {
        assertTrue(GeminiSpeakerCatalog.isSupported(GeminiSpeakerCatalog.defaultSpeaker))
    }

    @Test
    fun unknownSpeakerIsNotSupported() {
        assertTrue(GeminiSpeakerCatalog.isSupported("Kore"))
        // Pleos 화자 ID는 Gemini 화자가 아님.
        assertFalse(GeminiSpeakerCatalog.isSupported("seoyoon"))
    }

    @Test
    fun supportsOnlyServiceLanguages() {
        assertEquals(listOf("en-US", "ja-JP", "cmn-CN"), GeminiSpeakerCatalog.supportedLanguages)
        assertFalse(GeminiSpeakerCatalog.isSupportedLanguage("zh-CN"))
        assertFalse(GeminiSpeakerCatalog.isSupportedLanguage("ko-KR"))
    }
    //이 테스트 코드는 이 세가지 언어 코드 적어두기 위함.
}
