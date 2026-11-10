package com.braveberry.drivingapplication.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.SystemClock
import android.util.Log
import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.location.GeoLocation
import com.braveberry.domain.model.location.ProximityUpdate
import com.braveberry.domain.model.location.ProximityZone
import com.braveberry.domain.usecase.location.ObserveLocationUseCase
import com.braveberry.domain.usecase.location.ObserveProximityUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/** Only instrumentation tests own this test provider. Do not use it from production. */
internal class MockGpsInjector(context: Context) : AutoCloseable {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var installed = false

    @SuppressLint("InlinedApi")
    fun install() {
        check(!installed) { "Test GPS provider already installed" }
        try {
            // Matches the Pleos FusedLocation sample: create a mock GPS provider.
            manager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, true, false, false,
                true, true, true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE,
            )
            installed = true
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        } catch (cause: Exception) {
            if (installed) runCatching { manager.removeTestProvider(LocationManager.GPS_PROVIDER) }
            installed = false
            throw AssertionError(
                "Cannot install mock GPS provider. Use Pleos emulator and run: " +
                    "adb shell appops set --user current com.braveberry.drivingapplication android:mock_location allow",
                cause,
            )
        }
    }

    fun inject(latitude: Double, longitude: Double) {
        check(installed) { "Call install() before injecting GPS" }
        val fix = Location(LocationManager.GPS_PROVIDER).apply {
            this.latitude = latitude
            this.longitude = longitude
            accuracy = 5f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        try {
            manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, fix)
        } catch (cause: Exception) {
            throw AssertionError("Mock GPS injection failed; check android:mock_location app-op", cause)
        }
        Log.i(TAG, "INJECT lat=$latitude lon=$longitude")
    }

    override fun close() {
        if (!installed) return
        installed = false
        try {
            manager.removeTestProvider(LocationManager.GPS_PROVIDER)
            Log.i(TAG, "GPS test provider removed")
        } catch (cause: Exception) {
            throw AssertionError("Could not remove GPS test provider", cause)
        }
    }
}

/** Real UseCase flow with an independent, explicit lifetime. Propagates SDK failures. */
internal class LiveLocationProbe(observe: ObserveLocationUseCase) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updates = Channel<GeoLocation>(Channel.UNLIMITED)
    private val worker: Job = scope.launch {
        try {
            observe().collect { result ->
                when (result) {
                    is DataResource.Success -> result.data?.let {
                        Log.i(TAG, "LOCATION lat=${it.latitude} lon=${it.longitude} " +
                            "heading=${it.headingDegrees} time=${it.timestampMillis}")
                        updates.send(it)
                    }
                    is DataResource.Error -> error("Location stream failure: ${result.failure}")
                    is DataResource.Loading -> Unit
                }
            }
        } catch (cause: Throwable) {
            if (cause !is CancellationException) updates.close(cause)
        } finally {
            updates.close()
        }
    }

    suspend fun awaitInjectedFix(injector: MockGpsInjector, lat: Double, lon: Double): GeoLocation =
        try {
            withTimeout(LOCATION_TIMEOUT_MS) {
                var matching: GeoLocation? = null
                while (matching == null) {
                    injector.inject(lat, lon)
                    matching = withTimeoutOrNull(INJECTION_INTERVAL_MS) {
                        var received: GeoLocation
                        do {
                            received = updates.receive()
                        } while (!received.isNear(lat, lon))
                        received
                    }
                }
                checkNotNull(matching)
            }
        } catch (cause: TimeoutCancellationException) {
            throw AssertionError(
                "No Pleos SDK location callback matching lat=$lat lon=$lon within " +
                    "${LOCATION_TIMEOUT_MS}ms. Check GPS injection and emulator service.",
                cause,
            )
        }

    suspend fun stop() {
        worker.cancelAndJoin()
        scope.cancel()
    }
}

internal class ProximityProbe(observe: ObserveProximityUseCase, zone: ProximityZone) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updates = Channel<ProximityUpdate>(Channel.UNLIMITED)
    private val worker = scope.launch {
        try {
            observe(zone).collect { result ->
                when (result) {
                    is DataResource.Success -> {
                        Log.i(TAG, "PROXIMITY ${result.data}")
                        updates.send(result.data)
                    }
                    is DataResource.Error -> error("Proximity stream failure: ${result.failure}")
                    is DataResource.Loading -> Unit
                }
            }
        } catch (cause: Throwable) {
            if (cause !is CancellationException) updates.close(cause)
        } finally {
            updates.close()
        }
    }

    suspend fun expectRegistered(): ProximityUpdate.Registered {
        val event = receive("Registered")
        check(event is ProximityUpdate.Registered) { "Expected Registered, got $event" }
        return event
    }

    suspend fun expectEnter(id: String) = expectTransition(ProximityUpdate.Entered(id))
    suspend fun expectExit(id: String) = expectTransition(ProximityUpdate.Exited(id))

    private suspend fun expectTransition(expected: ProximityUpdate) {
        val actual = receive(expected.toString())
        check(actual == expected) { "Expected $expected, got $actual" }
        Log.i(TAG, "VERIFIED $actual")
    }

    private suspend fun receive(expected: String): ProximityUpdate = try {
        withTimeout(PROXIMITY_TIMEOUT_MS) { updates.receive() }
    } catch (cause: TimeoutCancellationException) {
        throw AssertionError("Pleos proximity did not emit $expected within ${PROXIMITY_TIMEOUT_MS}ms", cause)
    }

    suspend fun stop() {
        worker.cancelAndJoin()
        scope.cancel()
    }
}

internal fun GeoLocation.isNear(lat: Double, lon: Double): Boolean =
    kotlin.math.abs(latitude - lat) < COORDINATE_TOLERANCE &&
        kotlin.math.abs(longitude - lon) < COORDINATE_TOLERANCE

internal const val TAG = "PleosLocationIT"
internal const val LOCATION_TIMEOUT_MS = 25_000L
internal const val PROXIMITY_TIMEOUT_MS = 25_000L
internal const val INJECTION_INTERVAL_MS = 1_250L
private const val COORDINATE_TOLERANCE = 0.0002
