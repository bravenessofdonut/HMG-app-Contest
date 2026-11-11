package com.braveberry.voice.remote.util

import com.braveberry.voice.remote.TtsSampleSentences.Chinese
import com.braveberry.voice.remote.TtsSampleSentences.Japanese
import org.junit.Assert.assertEquals
import org.junit.Test

class TtsTextSanitizerTest {

    @Test
    fun keepsJapaneseAndChineseSentencesIntact() {
        // 기호 제거 규칙이 가나·한자·전각 문장부호를 지우지 않아야 함.
        listOf(
            Japanese.SEATBELT, Japanese.ARRIVAL, Japanese.GUIDE_OFFER,
            Chinese.SEATBELT, Chinese.ARRIVAL, Chinese.GUIDE_OFFER
        ).forEach { assertEquals(it, TtsTextSanitizer.clean(it)) }
    }

    @Test
    fun stripsMarkdownAroundCjkText() {
        assertEquals(Japanese.SEATBELT, TtsTextSanitizer.clean("**${Japanese.SEATBELT}**"))
        assertEquals(Chinese.SEATBELT, TtsTextSanitizer.clean("- ${Chinese.SEATBELT}"))
    }
}
