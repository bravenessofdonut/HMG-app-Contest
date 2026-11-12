package com.braveberry.poi.data.impl

import com.braveberry.data_resource.DataResource
import com.braveberry.poi.data.datasource.PoiRemoteDataSource
import com.braveberry.poi.data.model.PoiDataModel
import com.braveberry.domain.model.poi.PoiInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PoiRepositoryImplTest {

    private val datasource = mockk<PoiRemoteDataSource>()
    private val repository = PoiRepositoryImpl(datasource)

    private val placeName = "경복궁"

    private val poiDataModel = PoiDataModel(
        poi = "ChIJod7tSseifDUR9hXHLFNGMIs",
        latitude = 37.579617,
        longitude = 126.977041
    )

    @Test
    fun `장소명을 DataSource에 올바르게 전달한다`() = runTest {
        // Given
        coEvery {
            datasource.getPoi(any())
        } returns poiDataModel

        // When
        repository.getPoi(placeName).toList()

        // Then
        coVerify(exactly = 1) {
            datasource.getPoi("경복궁")
        }
    }

    @Test
    fun `POI 정보를 Domain Model로 변환하여 전달한다`() = runTest {
        // Given
        coEvery {
            datasource.getPoi(placeName)
        } returns poiDataModel

        // When
        val results = repository.getPoi(placeName).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is DataResource.Loading)

        val success = results[1] as DataResource.Success<*>

        assertEquals(
            PoiInfo(
                poi = "ChIJod7tSseifDUR9hXHLFNGMIs",
                latitude = 37.579617,
                longitude = 126.977041
            ),
            success.data
        )
    }

    @Test
    fun `검색 결과가 없으면 Success에 null을 전달한다`() = runTest {
        // Given
        coEvery {
            datasource.getPoi(placeName)
        } returns null

        // When
        val results = repository.getPoi(placeName).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is DataResource.Loading)

        val success = results[1] as DataResource.Success<*>
        assertEquals(null, success.data)
    }

    @Test
    fun `IOException 발생 시 Error 상태로 변환한다`() = runTest {
        // Given
        coEvery {
            datasource.getPoi(placeName)
        } throws IOException("Network error")

        // When
        val results = repository.getPoi(placeName).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is DataResource.Loading)
        assertTrue(results[1] is DataResource.Error)
    }
}
