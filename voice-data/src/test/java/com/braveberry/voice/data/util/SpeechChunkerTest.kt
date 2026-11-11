package com.braveberry.voice.data.util

import com.braveberry.voice.data.TtsSampleSentences.Chinese
import com.braveberry.voice.data.TtsSampleSentences.English
import com.braveberry.voice.data.TtsSampleSentences.Japanese
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechChunkerTest {

    /** LLM 스트리밍처럼 한 글자씩 토큰으로 흘려보냄. */
    private fun streamOf(text: String): Flow<String> = text.map { it.toString() }.asFlow()

    private suspend fun chunks(text: String, maxChars: Int = 300): List<String> =
        streamOf(text).toSpeechChunks(maxChars = maxChars).toList()

    @Test
    fun japanese_fullWidthExclamationEndsFirstChunk() = runTest {
        // 첫 소리 지연을 줄이려면 「！」에서 첫 청크가 끝나야 함.
        assertEquals(
            listOf(Japanese.SEATBELT, Japanese.ARRIVAL),
            chunks(Japanese.SEATBELT + Japanese.ARRIVAL)
        )
    }

    @Test
    fun chinese_fullWidthQuestionEndsFirstChunk() = runTest {
        assertEquals(
            listOf(Chinese.GUIDE_OFFER, Chinese.ARRIVAL),
            chunks(Chinese.GUIDE_OFFER + Chinese.ARRIVAL)
        )
    }

    @Test
    fun japanese_longSentenceIsCutAtIdeographicComma() = runTest {
        // 문장부호 없이 maxChars를 넘으면 「、」에서 끊어야 단어 중간이 잘리지 않음.
        val text = "東京駅から電車に乗って浅草へ向かい、雷門と浅草寺を見学したあと隅田川沿いを散歩します。"
        assertEquals("東京駅から電車に乗って浅草へ向かい、", chunks(text, maxChars = 20).first())
    }

    @Test
    fun cjkTextIsNotLostOrAltered() = runTest {
        // 분할 위치와 상관없이 원문 글자가 빠지거나 바뀌지 않아야 함.
        listOf(Japanese.ARRIVAL, Japanese.GUIDE_OFFER, Chinese.ARRIVAL, Chinese.SEATBELT).forEach {
            assertEquals(it, chunks(it).joinToString(""))
        }
    }

    @Test
    fun english_behaviourIsUnchanged() = runTest {
        // 둘째 문장은 minChars(40)를 넘으므로 단독 청크가 됨. 수정 전 동작과 동일해야 함.
        assertEquals(
            listOf(
                English.SEATBELT,
                "We will arrive at your destination shortly.",
                "Please check your belongings."
            ),
            chunks("${English.SEATBELT} ${English.ARRIVAL}")
        )
    }
}
