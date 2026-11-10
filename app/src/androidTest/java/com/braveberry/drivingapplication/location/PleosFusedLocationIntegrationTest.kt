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
 * Real Pleos-device integration tests (not fake unit tests).
 * Run ONLY on an emulator where the FusedLocation service is installed.
 *
 * Prerequisites: grant pleos.car.permission.FUSED_LOCATION and enable the
 * target app's mock-location app-op (see scripts/verify-pleos-fused-location.sh).
 * All locations below are injected into the Android GPS test provider, exactly
 * as in the upstream Pleos fused-location sample, not fabricated in a Flow.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PleosFusedLocationIntegrationTest {
    private val hilt = HiltAndroidRule(this)

    // Grant after the test APK is installed; the pre-install adb grant can be reset.
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
                // Pleos may update its cached location asynchronously after a callback.
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

    @Test fun proximityReceivesRegisteredEnterExitEnterFromInjectedGps() = runBlocking {
        requireReady()
        withMockGps { injector ->
            // The initial location MUST be outside the geofence, so the first
            // explicit inside injection should produce an ENTER event.
            val live = LiveLocationProbe(observeLocation)
            try {
                live.awaitInjectedFix(injector, OUTSIDE_LAT, SEOUL_LON)
                val zone = ProximityZone(SEOUL_LAT, SEOUL_LON, 100f)
                val proximity = ProximityProbe(observeProximity, zone)
                try {
                    val id = proximity.expectRegistered().alertId
                    assertTrue("SDK returned blank alert ID", id.isNotBlank())
                    Log.i(TAG, "REGISTERED $id at radius=${zone.radiusMeters}m")

                    // At each step inject, then await the actual SDK transition.
                    // Repeat GPS injection while awaiting a location acknowledgment.
                    injector.inject(SEOUL_LAT, SEOUL_LON)
                    live.awaitInjectedFix(injector, SEOUL_LAT, SEOUL_LON)
                    proximity.expectEnter(id)

                    injector.inject(OUTSIDE_LAT, SEOUL_LON)
                    live.awaitInjectedFix(injector, OUTSIDE_LAT, SEOUL_LON)
                    proximity.expectExit(id)

                    injector.inject(SEOUL_LAT, SEOUL_LON)
                    live.awaitInjectedFix(injector, SEOUL_LAT, SEOUL_LON)
                    proximity.expectEnter(id)
                    Log.i(TAG, "PASS: proximity sequence ENTER -> EXIT -> ENTER")
                } finally {
                    // Cancellation must remove the alert and release the SDK.
                    proximity.stop()
                }
            } finally {
                live.stop()
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
        const val OUTSIDE_LAT = SEOUL_LAT + 0.005 // About 556 m north of a 100 m zone.
    }
}
