package com.braveberry.location.local.impl

import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityReading
import com.braveberry.location.data.model.ProximityRequest
import com.braveberry.location.local.gateway.FusedLocationGateway
import com.braveberry.location.local.gateway.FusedLocationGatewayFactory
import com.braveberry.location.local.gateway.LocationCallback
import com.braveberry.location.local.gateway.ProximityCallback
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationDataSourceImplTest {
    @Test fun supportChecksWithoutInitialization() {
        val gateway = FakeGateway().apply { supported = false }
        assertFalse(LocationDataSourceImpl(FusedLocationGatewayFactory { gateway }).isSupported())
        assertEquals(0, gateway.initializations)
    }

    @Test fun permissionDeniedNeverInitializesSdk() = runTest {
        val gateway = FakeGateway().apply { permitted = false }
        val source = LocationDataSourceImpl(FusedLocationGatewayFactory { gateway })
        try {
            source.observeLocation().toList()
            throw AssertionError("permission failure required")
        } catch (_: SecurityException) { }
        assertEquals(0, gateway.initializations)
    }

    @Test fun unsupportedNeverInitializesSdk() = runTest {
        val gateway = FakeGateway().apply { supported = false }
        try {
            LocationDataSourceImpl(FusedLocationGatewayFactory { gateway }).observeLocation().toList()
            throw AssertionError("unsupported expected")
        } catch (_: UnsupportedOperationException) { }
        assertEquals(0, gateway.initializations)
    }

    @Test fun liveSubscriptionStopsAndReleasesOnCancellation() = runTest {
        val gateway = FakeGateway()
        val source = LocationDataSourceImpl(FusedLocationGatewayFactory { gateway })
        val values = mutableListOf<LocationReading?>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.observeLocation().collect { values += it }
        }
        gateway.locationCallback!!.onChanged(reading)
        gateway.locationCallback!!.onChanged(null)
        assertEquals(listOf(reading, null), values)
        job.cancel()
        assertEquals(1, gateway.stops)
        assertEquals(1, gateway.releases)
    }

    @Test fun lastKnownNoCacheIsNullAndReleasesAfterCallback() = runTest {
        val gateway = FakeGateway()
        val source = LocationDataSourceImpl(FusedLocationGatewayFactory { gateway })
        val task = async(UnconfinedTestDispatcher(testScheduler)) { source.lastKnownLocation().toList() }
        gateway.locationCallback!!.onChanged(null)
        assertEquals(listOf<LocationReading?>(null), task.await())
        assertEquals(0, gateway.stops)
        assertEquals(1, gateway.releases)
    }

    @Test fun callbackFailureReleasesSdk() = runTest {
        val gateway = FakeGateway()
        val source = LocationDataSourceImpl(FusedLocationGatewayFactory { gateway })
        val task = async(UnconfinedTestDispatcher(testScheduler)) {
            runCatching { source.observeLocation().toList() }.exceptionOrNull()
        }
        gateway.locationCallback!!.onError(IllegalStateException("binder lost"))
        assertTrue(task.await() is IOException)
        assertEquals(1, gateway.stops)
        assertEquals(1, gateway.releases)
    }

    @Test fun failureDuringRegistrationStillCleansUp() = runTest {
        val gateway = FakeGateway().apply { throwOnStart = true }
        val source = LocationDataSourceImpl(FusedLocationGatewayFactory { gateway })
        try {
            source.observeLocation().toList()
            throw AssertionError("expected error")
        } catch (_: IOException) { }
        assertEquals(1, gateway.stops)
        assertEquals(1, gateway.releases)
    }

    @Test fun proximityTransitionsAreScopedToRegisteredIdAndCancelRemovesAlert() = runTest {
        val gateway = FakeGateway()
        val source = LocationDataSourceImpl(FusedLocationGatewayFactory { gateway })
        val events = mutableListOf<ProximityReading>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.observeProximity(ProximityRequest(37.0, 127.0, 100f)).collect { events += it }
        }
        gateway.proximityCallback!!.onEntered("unexpected")
        gateway.proximityCallback!!.onRegistered("z1")
        gateway.proximityCallback!!.onEntered("wrong")
        gateway.proximityCallback!!.onEntered("z1")
        gateway.proximityCallback!!.onExited("z1")
        assertEquals(listOf(ProximityReading.Registered("z1"), ProximityReading.Entered("z1"),
            ProximityReading.Exited("z1")), events)
        job.cancel()
        assertEquals(listOf("z1"), gateway.removedIds)
        assertEquals(1, gateway.releases)
    }

    @Test fun lateProximityRegistrationAfterCancellationIsRemoved() = runTest {
        val gateway = FakeGateway()
        val source = LocationDataSourceImpl(FusedLocationGatewayFactory { gateway })
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.observeProximity(ProximityRequest(37.0, 127.0, 10f)).collect { }
        }
        val delayed = gateway.proximityCallback!!
        job.cancel()
        delayed.onRegistered("late")
        assertEquals(listOf("late"), gateway.removedIds)
    }

    private class FakeGateway : FusedLocationGateway {
        var supported = true
        var permitted = true
        var initializations = 0
        var releases = 0
        var stops = 0
        var throwOnStart = false
        var locationCallback: LocationCallback? = null
        var proximityCallback: ProximityCallback? = null
        val removedIds = mutableListOf<String>()
        override fun isSupported() = supported
        override fun hasPermission() = permitted
        override fun initialize() { initializations++ }
        override fun release() { releases++ }
        override fun startLocationUpdates(callback: LocationCallback) {
            locationCallback = callback
            if (throwOnStart) throw IllegalStateException("failed halfway")
        }
        override fun stopLocationUpdates() { stops++ }
        override fun requestLastLocation(callback: LocationCallback) { locationCallback = callback }
        override fun addProximity(request: ProximityRequest, callback: ProximityCallback) {
            proximityCallback = callback
        }
        override fun removeProximity(alertId: String) { removedIds += alertId }
    }

    private companion object {
        val reading = LocationReading(37.0, 127.0, 0.0, 0f, 1f, 123L)
    }
}
