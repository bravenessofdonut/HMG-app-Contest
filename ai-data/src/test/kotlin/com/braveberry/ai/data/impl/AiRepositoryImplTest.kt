
package com.braveberry.ai.data.impl

import com.braveberry.ai.data.datasource.AiRemoteDataSource
import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.ai.GuideRequest
import com.braveberry.domain.model.ai.GuideStreamModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AiRepositoryImplTest {

    private val datasource = mockk<AiRemoteDataSource>()
    private val repository = AiRepositoryImpl(datasource)

    private val request = GuideRequest(
        region = "서울 종로구",
        language = "English"
    )

    @Test
    fun `GuideRequest를 DataModel로 변환하여 전달한다`() = runTest {
        // Given
        every {
            datasource.generateGuide(any())
        } returns flowOf("PLACE: Gyeongbokgung Palace\nHello")

        // When
        repository.generateGuide(request).toList()

        // Then
        verify(exactly = 1) {
            datasource.generateGuide(
                match {
                    it.region == "서울 종로구" &&
                            it.language == "English"
                }
            )
        }
    }

    @Test
    fun `관광지명과 설명 청크를 DataResource로 전달한다`() = runTest {
        // Given
        every {
            datasource.generateGuide(any())
        } returns flowOf(
            "PLA",
            "CE: Gyeongbokgung Palace\nWelcome",
            " to Korea!"
        )

        // When
        val results = repository.generateGuide(request).toList()

        // Then
        assertEquals(4, results.size)
        assertTrue(results[0] is DataResource.Loading)

        val successes = results
            .filterIsInstance<DataResource.Success<*>>()
            .map { it.data }

        assertEquals(
            listOf(
                GuideStreamModel(placeName = "Gyeongbokgung Palace"),
                GuideStreamModel(textChunk = "Welcome"),
                GuideStreamModel(textChunk = " to Korea!")
            ),
            successes
        )
    }

    @Test
    fun `줄바꿈 없는 일반 텍스트도 청크 단위로 전달한다`() = runTest {
        // Given
        every {
            datasource.generateGuide(any())
        } returns flowOf(
            "Hello",
            " everyone",
            "!"
        )

        // When
        val results = repository.generateGuide(request).toList()

        // Then
        val successes = results
            .filterIsInstance<DataResource.Success<*>>()
            .map { it.data }

        assertEquals(
            listOf(
                GuideStreamModel(textChunk = "Hello"),
                GuideStreamModel(textChunk = " everyone"),
                GuideStreamModel(textChunk = "!")
            ),
            successes
        )
    }

    @Test
    fun `IOException 발생 시 Error 상태로 변환한다`() = runTest {
        // Given
        every {
            datasource.generateGuide(any())
        } returns flow {
            throw IOException("Network error")
        }

        // When
        val results = repository.generateGuide(request).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is DataResource.Loading)
        assertTrue(results[1] is DataResource.Error)
    }
}
