
package com.braveberry.ai.data.parser

import com.braveberry.domain.model.ai.GuideStreamModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.withTimeout

class GuideStreamParserTest {

    @Test
    fun `PLACE 헤더가 여러 청크로 나뉘어도 장소명을 추출한다`() = runTest {
        // Given
        val source = flowOf(
            "PLA",
            "CE: Gyeong",
            "bokgung Palace\nWelcome",
            " to Korea!"
        )

        // When
        val results = source.parseGuideStream().toList()

        // Then
        assertEquals(
            listOf(
                GuideStreamModel(
                    placeName = "Gyeongbokgung Palace"
                ),
                GuideStreamModel(
                    textChunk = "Welcome"
                ),
                GuideStreamModel(
                    textChunk = " to Korea!"
                )
            ),
            results
        )
    }

    @Test
    fun `PLACE 헤더와 줄바꿈이 없어도 청크를 전달한다`() = runTest {
        // Given
        val source = flowOf(
            "Hello",
            " everyone",
            "!"
        )

        // When
        val results = source.parseGuideStream().toList()

        // Then
        assertEquals(
            listOf(
                GuideStreamModel(textChunk = "Hello"),
                GuideStreamModel(textChunk = " everyone"),
                GuideStreamModel(textChunk = "!")
            ),
            results
        )
    }

    @Test
    fun `상위 스트림이 끝나지 않아도 일반 텍스트를 즉시 전달한다`() = runTest {
        // Given
        val source = flow<String> {
            emit("Hello")
            awaitCancellation()
        }

        // When
        val firstResult = withTimeout(1000) {
            source.parseGuideStream().first()
        }

        // Then
        assertEquals(
            GuideStreamModel(textChunk = "Hello"),
            firstResult
        )
    }
}
