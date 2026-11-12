
package com.braveberry.poi.remote.impl

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.braveberry.poi.remote.BuildConfig
import com.braveberry.poi.remote.factory.GooglePlacesServiceFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PoiRemoteDataSourceImplTest {

    private val datasource = PoiRemoteDataSourceImpl(
        api = GooglePlacesServiceFactory.create(),
        apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
    )

    @Test
    fun `경복궁 검색 시 Google Place ID와 좌표를 반환한다`() = runBlocking {
        // Given
        val placeName = "경복궁 서울특별시"

        assertTrue(
            "Google Places API Key가 설정되지 않았습니다.",
            BuildConfig.GOOGLE_PLACES_API_KEY.isNotBlank()
        )

        // When
        val result = withTimeout(30_000L) {
            datasource.getPoi(placeName)
        }

        // Then
        assertNotNull("검색 결과가 없습니다.", result)

        result!!

        Log.d("PoiTest", "POI: ${result.poi}")
        Log.d("PoiTest", "Latitude: ${result.latitude}")
        Log.d("PoiTest", "Longitude: ${result.longitude}")

        assertTrue(result.poi.isNotBlank())
        assertTrue(result.latitude in -90.0..90.0)
        assertTrue(result.longitude in -180.0..180.0)
    }
}
