package com.braveberry.drivingapplication.location

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.location.GeoLocation
import com.braveberry.domain.model.location.ProximityZone
import com.braveberry.domain.usecase.location.CheckLocationSupportUseCase
import com.braveberry.domain.usecase.location.GetLastKnownLocationUseCase
import com.braveberry.domain.usecase.location.ObserveLocationUseCase
import com.braveberry.domain.usecase.location.ObserveProximityUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 실제 Pleos 에뮬레이터에서 SDK 동작 확인함.
 * FusedLocation 서비스가 설치된 환경에서만 실행함.
 *
 * 실행 전 FUSED_LOCATION 권한 부여가 필요함.
 * 앱의 mock-location app-op도 허용해야 함.
 * 좌표는 Flow가 아니라 GPS 테스트 Provider에 주입함.
 * 실행은 scripts/verify-pleos-fused-location.sh 사용함.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PleosFusedLocationIntegrationTest {
    private val hilt = HiltAndroidRule(this)

    // APK 설치 후 권한 부여함. 설치 중 권한이 초기화될 수 있음.
    @get:Rule val rules: TestRule = RuleChain
        .outerRule(GrantPermissionRule.grant(FUSED_LOCATION_PERMISSION))
        .around(hilt)

    @Inject lateinit var support: CheckLocationSupportUseCase
    @Inject lateinit var observeLocation: ObserveLocationUseCase
    @Inject lateinit var lastKnown: GetLastKnownLocationUseCase
    @Inject lateinit var observeProximity: ObserveProximityUseCase

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun inject() = hilt.inject()

    @Test fun pleosServiceAndRuntimePermissionAreAvailable() = runBlocking<Unit> {
        requireReady()
        Log.i(TAG, "PASS: Pleos service supported and FUSED_LOCATION permission granted")
    }

    @Test fun liveUpdatesDeliverInjectedCoordinates() = runBlocking {
        requireReady()
        withMockGps { injector ->
            val stream = LiveLocationProbe(observeLocation)
            try {
                val received = stream.awaitInjectedFix(injector, SEOUL_LAT, SEOUL_LON)
                assertValidFix(received)
                Log.i(TAG, "PASS: live update matches injected GPS: $received")
            } finally {
                stream.stop()
            }
        }
    }

    @Test fun lastKnownLocationReturnsPositionPreviouslyReceivedFromSdk() = runBlocking {
        requireReady()
        withMockGps { injector ->
            val stream = LiveLocationProbe(observeLocation)
            try {
                val live = stream.awaitInjectedFix(injector, SECOND_LAT, SECOND_LON)
                assertValidFix(live)
                // 위치 콜백 이후 캐시 갱신이 늦을 수 있음.
                var cached: GeoLocation? = null
                withTimeout(LOCATION_TIMEOUT_MS) {
                    while (cached == null) {
                        val result = withTimeout(10_000L) {
                            lastKnown().first { it !is DataResource.Loading }
                        }
                        when (result) {
                            is DataResource.Success -> {
                                val value = result.data
                                Log.i(TAG, "LAST_KNOWN cached=$value")
                                if (value != null && value.isNear(SECOND_LAT, SECOND_LON)) {
                                    cached = value
                                }
                            }
                            is DataResource.Error -> error("Last-known SDK failure: ${result.failure}")
                            is DataResource.Loading -> error("Unexpected loading")
                        }
                        if (cached == null) delay(600L)
                    }
                }
                assertValidFix(checkNotNull(cached))
                Log.i(TAG, "PASS: last known matches injected GPS: $cached")
            } finally {
                stream.stop()
            }
        }
    }

    @Test fun streamingCanRestartAfterCancellation() = runBlocking {
        requireReady()
        withMockGps { injector ->
            repeat(2) { run ->
                val stream = LiveLocationProbe(observeLocation)
                try {
                    val lat = if (run == 0) SEOUL_LAT else SECOND_LAT
                    val lon = if (run == 0) SEOUL_LON else SECOND_LON
                    assertValidFix(stream.awaitInjectedFix(injector, lat, lon))
                    Log.i(TAG, "STREAM SESSION ${run + 1} PASS")
                } finally {
                    stream.stop()
                }
            }
            Log.i(TAG, "PASS: SDK stream can restart after cancellation")
        }
    }

    private suspend fun requireReady() {
        val result = withTimeout(10_000L) {
            support().first { it !is DataResource.Loading }
        }
        check(result is DataResource.Success && result.data) {
            "Pleos FusedLocation service is unavailable: $result. " +
                "Run on the Pleos emulator, not a standard Android emulator."
        }
        check(context.checkSelfPermission(FUSED_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            "FUSED_LOCATION permission is not granted for ${context.packageName}. " +
                "Grant runtime permission before running integration tests."
        }
    }

    private suspend fun withMockGps(block: suspend (MockGpsInjector) -> Unit) {
        MockGpsInjector(context).use { injector ->
            injector.install()
            block(injector)
        }
    }

    private fun assertValidFix(value: GeoLocation) {
        assertTrue("Latitude invalid: $value", value.latitude.isFinite() && value.latitude in -90.0..90.0)
        assertTrue("Longitude invalid: $value", value.longitude.isFinite() && value.longitude in -180.0..180.0)
        assertTrue("Timestamp invalid: $value", value.timestampMillis > 0L)
    }

    private companion object {
        const val FUSED_LOCATION_PERMISSION = "pleos.car.permission.FUSED_LOCATION"
        const val SEOUL_LAT = 37.5665
        const val SEOUL_LON = 126.9780
        const val SECOND_LAT = 37.5700
        const val SECOND_LON = 126.9820
        const val OUTSIDE_LAT = SEOUL_LAT + 0.005 // 반경 100m 기준, 중심에서 북쪽 약 556m 떨어진 지점임.
    }
}
