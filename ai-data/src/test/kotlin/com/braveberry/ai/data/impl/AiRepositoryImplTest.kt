package com.braveberry.ai.data.impl
import com.braveberry.ai.data.datasource.AiRemoteDataSource
import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.domain.model.ai.GuideRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AiRepositoryImplTest {

    private val datasource = mockk<AiRemoteDataSource>()
    private val repository = AiRepositoryImpl(datasource)

    private val request = GuideRequest(
        placeName = "경복궁",
        region = "서울 종로구",
        language = "English"
    )

    @Test
    fun `정상 스트리밍 시 Loading 이후 청크별 Success를 반환한다`() = runTest {
        // Given
        every {
            datasource.generateGuide(any())
        } returns flowOf("Hello", " everyone", "!")

        // When
        val results = repository.generateGuide(request).toList()

        // Then
        assertEquals(4, results.size)
        assertTrue(results[0] is DataResource.Loading)

        val chunks = results
            .filterIsInstance<DataResource.Success<String>>()
            .map { it.data }

        assertEquals(
            listOf("Hello", " everyone", "!"),
            chunks
        )
    }

    @Test
    fun `GuideRequest가 DataModel로 변환되어 전달된다`() = runTest {
        // Given
        every {
            datasource.generateGuide(any())
        } returns flowOf("Guide")

        // When
        repository.generateGuide(request).toList()

        // Then
        verify(exactly = 1) {
            datasource.generateGuide(
                match {
                    it.placeName == "경복궁" &&
                            it.region == "서울 종로구" &&
                            it.language == "English"
                }
            )
        }
    }

    @Test
    fun `IOException 발생 시 ServiceUnavailable을 반환한다`() = runTest {
        // Given
        val exception = IOException("Network error")

        every {
            datasource.generateGuide(any())
        } returns flow {
            throw exception
        }

        // When
        val results = repository.generateGuide(request).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is DataResource.Loading)


        val error = results[1] as DataResource.Error

        assertTrue(
            error.failure is ResourceFailure.ServiceUnavailable
        )
        assertSame(exception, error.failure.cause)
    }

    @Test
    fun `분류되지 않은 예외는 그대로 전파된다`() = runTest {
        // Given
        val exception = IllegalStateException("Unknown error")

        every {
            datasource.generateGuide(any())
        } returns flow {
            throw exception
        }

        // When
        val thrown = runCatching {
            repository.generateGuide(request).toList()
        }.exceptionOrNull()

        // Then
        assertSame(exception, thrown)
    }
}
