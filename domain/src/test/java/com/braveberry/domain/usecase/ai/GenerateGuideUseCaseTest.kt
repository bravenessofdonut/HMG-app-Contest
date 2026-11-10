
package com.braveberry.domain.usecase.ai

import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.domain.model.ai.GuideRequest
import com.braveberry.domain.repository.AiRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class GenerateGuideUseCaseTest {

    private val repository = mockk<AiRepository>()
    private val useCase = GenerateGuideUseCase(repository)

    private val request = GuideRequest(
        placeName = "경복궁",
        region = "서울 종로구",
        language = "English"
    )

    @Test
    fun `GuideRequest를 Repository에 올바르게 전달한다`() = runTest {
        // Given
        every {
            repository.generateGuide(any())
        } returns flowOf(DataResource.success("Hello"))

        // When
        useCase(request).toList()

        // Then
        verify(exactly = 1) {
            repository.generateGuide(
                match {
                    it.placeName == "경복궁" &&
                            it.region == "서울 종로구" &&
                            it.language == "English"
                }
            )
        }
    }

    @Test
    fun `Loading과 스트리밍 Success를 그대로 전달한다`() = runTest {
        // Given
        val loading: DataResource<String> = DataResource.loading()
        val first: DataResource<String> = DataResource.success("Hello")
        val second: DataResource<String> = DataResource.success(" everyone!")

        every {
            repository.generateGuide(request)
        } returns flowOf(loading, first, second)

        // When
        val results = useCase(request).toList()

        // Then
        assertEquals(3, results.size)
        assertSame(loading, results[0])
        assertSame(first, results[1])
        assertSame(second, results[2])
    }

    @Test
    fun `Repository의 Error 상태를 그대로 전달한다`() = runTest {
        // Given
        val failure = ResourceFailure.ServiceUnavailable(
            IOException("Network error")
        )

        val error: DataResource<String> = DataResource.error(failure)

        every {
            repository.generateGuide(request)
        } returns flowOf(error)

        // When
        val results = useCase(request).toList()

        // Then
        assertEquals(1, results.size)
        assertSame(error, results[0])
    }
}
