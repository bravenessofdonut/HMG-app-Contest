
package com.braveberry.domain.usecase.poi

import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.domain.model.poi.PoiInfo
import com.braveberry.domain.repository.PoiRepository
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

class GetPlaceIdUseCaseTest {

    private val repository = mockk<PoiRepository>()
    private val useCase = GetPoiUseCase(repository)

    private val placeName = "경복궁"

    private val poiInfo = PoiInfo(
        poi = "ChIJod7tSseifDUR9hXHLFNGMIs",
        latitude = 37.579617,
        longitude = 126.977041
    )

    @Test
    fun `장소명을 Repository에 올바르게 전달한다`() = runTest {
        // Given
        every {
            repository.getPoi(any())
        } returns flowOf(
            DataResource.success(poiInfo)
        )

        // When
        useCase(placeName).toList()

        // Then
        verify(exactly = 1) {
            repository.getPoi("경복궁")
        }
    }

    @Test
    fun `Loading과 Success를 그대로 전달한다`() = runTest {
        // Given
        val loading: DataResource<PoiInfo?> =
            DataResource.loading()

        val success: DataResource<PoiInfo?> =
            DataResource.success(poiInfo)

        every {
            repository.getPoi(placeName)
        } returns flowOf(loading, success)

        // When
        val results = useCase(placeName).toList()

        // Then
        assertEquals(2, results.size)
        assertSame(loading, results[0])
        assertSame(success, results[1])
    }

    @Test
    fun `검색 결과가 없으면 Success null을 그대로 전달한다`() = runTest {
        // Given
        val success: DataResource<PoiInfo?> =
            DataResource.success(null)

        every {
            repository.getPoi(placeName)
        } returns flowOf(success)

        // When
        val results = useCase(placeName).toList()

        // Then
        assertEquals(1, results.size)
        assertSame(success, results[0])
    }

    @Test
    fun `Repository의 Error 상태를 그대로 전달한다`() = runTest {
        // Given
        val failure = ResourceFailure.ServiceUnavailable(
            IOException("Network error")
        )

        val error: DataResource<PoiInfo?> =
            DataResource.error(failure)

        every {
            repository.getPoi(placeName)
        } returns flowOf(error)

        // When
        val results = useCase(placeName).toList()

        // Then
        assertEquals(1, results.size)
        assertSame(error, results[0])
    }
}