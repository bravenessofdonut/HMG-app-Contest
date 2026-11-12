
package com.braveberry.poi.remote.impl

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.braveberry.poi.data.model.PoiDataModel
import com.braveberry.poi.remote.BuildConfig
import com.braveberry.poi.remote.factory.GooglePlacesServiceFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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

    private suspend fun searchPoi(placeName: String): PoiDataModel {
        assertTrue(
            "Google Places API Key가 설정되지 않았습니다.",
            BuildConfig.GOOGLE_PLACES_API_KEY.isNotBlank()
        )

        val result = withTimeout(30_000L) {
            datasource.getPoi(placeName)
        }

        assertNotNull("검색 결과가 없습니다: $placeName", result)

        return requireNotNull(result).also {
            Log.d("PoiTest", "Query: $placeName")
            Log.d("PoiTest", "POI: ${it.poi}")
            Log.d("PoiTest", "Latitude: ${it.latitude}")
            Log.d("PoiTest", "Longitude: ${it.longitude}")
            Log.d("PoiTest", "Address: ${it.address}")

            assertTrue(it.poi.isNotBlank())
            assertTrue(it.latitude in -90.0..90.0)
            assertTrue(it.longitude in -180.0..180.0)
            assertTrue(
                "주소값이 비어 있습니다.",
                !it.address.isNullOrBlank()
            )
        }
    }

    @Test
    fun `경복궁 검색 시 POI 좌표 주소를 반환한다`() = runBlocking {
        val result = searchPoi("경복궁 서울특별시")

        assertTrue(result.poi.isNotBlank())
        assertTrue(!result.address.isNullOrBlank())
    }

    @Test
    fun `영어 일본어 중국어로 검색해도 동일한 장소를 반환한다`() = runBlocking {

        // 한국어 검색을 기준으로 설정
        val koreanResult = searchPoi("경복궁 서울특별시")

        val queries = listOf(
            "English" to "Gyeongbokgung Palace Seoul South Korea",
            "Japanese" to "韓国 ソウル 景福宮",
            "Chinese" to "韩国 首尔 景福宫"
        )

        // 모든 언어의 검색 결과를 먼저 가져옴
        val results = queries.map { (language, query) ->
            language to searchPoi(query)
        }

        // 로그 출력
        results.forEach { (language, result) ->
            Log.d("PoiTest", "===== $language =====")
            Log.d("PoiTest", "Place ID: ${result.poi}")
            Log.d("PoiTest", "Address: ${result.address}")
            Log.d(
                "PoiTest",
                "Location: ${result.latitude}, ${result.longitude}"
            )
        }

        // 한국어 검색 결과와 동일한 Place ID인지 검증
        results.forEach { (language, result) ->
            assertEquals(
                "$language 검색 결과의 Place ID가 다릅니다.",
                koreanResult.poi,
                result.poi
            )
        }
    }
}
