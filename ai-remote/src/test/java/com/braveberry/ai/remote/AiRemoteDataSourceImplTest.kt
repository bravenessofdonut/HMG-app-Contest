package com.braveberry.ai.remote

import com.braveberry.ai.remote.impl.AiRemoteDataSourceImpl
import com.braveberry.ai.data.model.GuideRequestDataModel
import com.braveberry.ai.remote.api.GeminiApiService
import com.braveberry.ai.remote.model.GuideResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class AiRemoteDataSourceImplTest {

    private val api = mockk<GeminiApiService>()
    private val datasource = AiRemoteDataSourceImpl(api)

    private val request = GuideRequestDataModel(
        region = "서울 종로구",
        language = "English"
    )

    @Test
    fun `정상 스트리밍 응답을 String 청크로 변환한다`() = runTest {
        // Given
        coEvery {
            api.generateGuide(any())
        } returns flowOf(
            GuideResponseDto("Hello"),
            GuideResponseDto(" everyone"),
            GuideResponseDto("!")
        )

        // When
        val results = datasource.generateGuide(request).toList()

        // Then
        assertEquals(
            listOf("Hello", " everyone", "!"),
            results
        )
    }

    @Test
    fun `DataModel이 DTO로 올바르게 변환되어 전달된다`() = runTest {
        // Given
        coEvery {
            api.generateGuide(any())
        } returns flowOf(GuideResponseDto("Guide"))

        // When
        datasource.generateGuide(request).toList()

        // Then
        coVerify(exactly = 1) {
            api.generateGuide(
                match {
                            it.region == "서울 종로구" &&
                            it.language == "English"
                }
            )
        }
    }

    @Test
    fun `API에서 IOException 발생 시 예외를 그대로 전파한다`() = runTest {
        // Given
        val exception = IOException("Network error")

        coEvery {
            api.generateGuide(any())
        } returns flow {
            throw exception
        }

        // When
        val thrown = runCatching {
            datasource.generateGuide(request).toList()
        }.exceptionOrNull()

        // Then
        assertSame(exception, thrown)
    }
}
