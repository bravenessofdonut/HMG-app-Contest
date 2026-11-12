package com.braveberry.drivingapplication.navi

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.navi.NaviCommand
import com.braveberry.domain.model.navi.NaviEventType
import com.braveberry.domain.model.navi.NaviPlace
import com.braveberry.domain.repository.NaviRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GooglePoiWaypointIntegrationTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: NaviRepository

    @Before fun setUp() { hiltRule.inject() }

    @Test fun googlePoiWaypointCanCompleteRouteSearch() {
        val args = InstrumentationRegistry.getArguments()
        val poiId = requireNotNull(args.getString("googlePoiId")?.takeIf { it.isNotBlank() }) {
            "Google Place ID 필요: googlePoiId 계측 인자를 입력해야 함"
        }
        val latitude = requireNotNull(args.getString("googleLatitude")) {
            "googleLatitude 계측 인자 필요"
        }.toDouble()
        val longitude = requireNotNull(args.getString("googleLongitude")) {
            "googleLongitude 계측 인자 필요"
        }.toDouble()
        val waypoint = NaviPlace(
            latitude = latitude,
            longitude = longitude,
            poiId = poiId,
            poiName = args.getString("googlePoiName") ?: "Google 테스트 경유지",
            poiSubId = args.getString("googlePoiSubId") ?: "0",
            address = args.getString("googleAddress") ?: "",
            poiProvider = "Google"
        )
        val destination = NaviPlace(
            latitude = 37.48544722,
            longitude = 127.03636666,
            poiId = "36khp37hh8gqcm5nqhe5",
            poiName = "42dot",
            poiSubId = "0",
            address = "서울시 강남구 남부순환로 2621"
        )

        // 공식 샘플 목적지와 Google 경유지를 한 경로로 요청함.
        // SDK 호출 반환만으로 성공 판정하지 않고 실제 경로 이벤트를 기다림.
        runBlocking {
            var started = false
            var routeRequested = false
            val routeResult = async(start = CoroutineStart.UNDISPATCHED) {
                repository.observeEvents().first { resource ->
                    when (resource) {
                        is DataResource.Loading -> false
                        is DataResource.Error -> throw AssertionError("이벤트 구독 실패: ${resource.failure}", resource.failure)
                        is DataResource.Success -> {
                            Log.i(TAG, "수신 이벤트: ${resource.data.type} / ${resource.data.detail}")
                            resource.data.type == NaviEventType.ROUTING_COMPLETED ||
                                resource.data.type == NaviEventType.ERROR
                        }
                    }
                }
            }
            try {
                requireSuccess("NaviHelper 초기화", withContext(Dispatchers.Main) {
                    repository.start().last()
                })
                started = true
                Log.i(TAG, "Google 경유지 요청: provider=Google, poiId=$poiId, lat=$latitude, lon=$longitude")
                requireSuccess("경로 요청 전송", withContext(Dispatchers.Main) {
                    repository.execute(NaviCommand.RequestRoute(destination, listOf(waypoint))).last()
                })
                routeRequested = true

                val result = withTimeoutOrNull(60_000L) { routeResult.await() }
                assertNotNull("60초 동안 탐색 완료 또는 오류 이벤트가 오지 않음. Pleos 내비 앱 상태 확인 필요", result)
                val event = (result as DataResource.Success).data
                assertEquals("NaviHelper 경로 탐색 오류: ${event.detail}", NaviEventType.ROUTING_COMPLETED, event.type)

                // 탐색 성공 후 실제 등록된 경유지 정보를 조회해 로그로 확인함.
                val waypointResult = async(start = CoroutineStart.UNDISPATCHED) {
                    repository.observeEvents().first { resource ->
                        resource is DataResource.Success &&
                            (resource.data.type == NaviEventType.WAYPOINT_INFO || resource.data.type == NaviEventType.ERROR)
                    }
                }
                try {
                    requireSuccess("경유지 정보 조회 요청", withContext(Dispatchers.Main) {
                        repository.execute(NaviCommand.GetWaypoints).last()
                    })
                    val waypointResponse = withTimeoutOrNull(10_000L) { waypointResult.await() }
                    Log.i(TAG, "조회된 경유지: ${((waypointResponse as? DataResource.Success)?.data)?.detail ?: "응답 없음"}")
                } finally {
                    waypointResult.cancel()
                }
            } finally {
                routeResult.cancel()
                if (started) {
                    withContext(Dispatchers.Main) {
                        if (routeRequested) runCatching {
                            repository.execute(NaviCommand.CancelRoute).last()
                        }.onFailure { Log.w(TAG, "테스트 경로 취소 실패", it) }
                        runCatching { repository.stop().last() }
                            .onFailure { Log.w(TAG, "NaviHelper 종료 실패", it) }
                    }
                }
            }
        }
    }

    private fun requireSuccess(operation: String, result: DataResource<Unit>) {
        assertTrue("$operation 실패: ${if (result is DataResource.Error) result.failure else result}",
            result is DataResource.Success)
    }

    private companion object {
        const val TAG = "GooglePoiNaviIT"
    }
}
