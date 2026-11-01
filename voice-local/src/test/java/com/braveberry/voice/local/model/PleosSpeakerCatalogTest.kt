package com.braveberry.voice.local.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PleosSpeakerCatalogTest {
    @Test fun matchesDocumentedSpeakerIdsWithoutDuplicates() {
        val expected = listOf(
            "seoyoon", "eunwoo", "haeun", "sanghyeok",
            "lyra", "finley", "iris", "arlo", "sabine", "noah"
        )
        assertEquals(expected, PleosSpeakerCatalog.languageTags.keys.toList())
        assertEquals(expected.size, expected.distinct().size)
        assertNull(PleosSpeakerCatalog.locales["seowoon"])
    }

    @Test fun speakerLocalesUseSupportedSdkTags() {
        val supportedTags = setOf("ko-KR", "en-US", "es-US", "es-MX", "fr-CA")
        assertEquals(supportedTags.intersect(PleosSpeakerCatalog.languageTags.values.toSet()),
            PleosSpeakerCatalog.languageTags.values.toSet())
        assertEquals("es-US", PleosSpeakerCatalog.locales.getValue("iris").toLanguageTag())
        assertEquals("fr-CA", PleosSpeakerCatalog.locales.getValue("sabine").toLanguageTag())
    }
}
