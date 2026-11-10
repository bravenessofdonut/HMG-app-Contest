package com.braveberry.location.data.impl

import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.domain.model.location.ProximityUpdate
import com.braveberry.domain.model.location.ProximityZone
import com.braveberry.location.data.datasource.LocationDataSource
import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityReading
import com.braveberry.location.data.model.ProximityRequest
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRepositoryImplTest {
    @Test fun supportEmitsLoadingAndTrue() = runTest {
        val result = LocationRepositoryImpl(FakeSource()).checkSupport().toList()
        assertTrue(result.first() is DataResource.Loading)
        assertEquals(true, (result.last() as DataResource.Success).data)
    }

    @Test fun liveReadingsAreMappedWithAllFields() = runTest {
        val result = LocationRepositoryImpl(FakeSource()).observeLocation().toList()
        val point = (result.last() as DataResource.Success).data!!
        assertEquals(37.0, point.latitude, 0.0)
        assertEquals(127.0, point.longitude, 0.0)
        assertEquals(3.0, point.altitudeMeters, 0.0)
        assertEquals(22f, point.headingDegrees, 0f)
        assertEquals(0.8f, point.confidence, 0f)
        assertEquals(123L, point.timestampMillis)
    }

    @Test fun noCachedFixIsSuccessNull() = runTest {
        val result = LocationRepositoryImpl(FakeSource(last = flowOf(null)))
            .getLastKnownLocation().toList()
        assertTrue(result.first() is DataResource.Loading)
        assertEquals(null, (result.last() as DataResource.Success).data)
    }

    @Test fun permissionFailureIsClassifiedAndCausePreserved() = runTest {
        val denied = SecurityException("permission")
        val result = LocationRepositoryImpl(FakeSource(live = flow { throw denied }))
            .observeLocation().toList().last() as DataResource.Error
        assertTrue(result.failure is ResourceFailure.PermissionRequired)
        assertSame(denied, result.failure.cause)
    }

    @Test fun unsupportedAndServiceUnavailableAreDistinguished() = runTest {
        val unsupported = LocationRepositoryImpl(FakeSource(live = flow {
            throw UnsupportedOperationException("not available")
        })).observeLocation().toList().last() as DataResource.Error
        assertTrue(unsupported.failure is ResourceFailure.Unsupported)

        val unavailable = LocationRepositoryImpl(FakeSource(live = flow {
            throw IOException("dead service")
        })).observeLocation().toList().last() as DataResource.Error
        assertTrue(unavailable.failure is ResourceFailure.ServiceUnavailable)
    }

    @Test fun invalidCoordinatesBecomeErrorNotFakeLocation() = runTest {
        val invalid = reading.copy(latitude = 91.0)
        val results = LocationRepositoryImpl(FakeSource(live = flowOf(invalid)))
            .observeLocation().toList()
        assertTrue(results.last() is DataResource.Error)
        assertTrue((results.last() as DataResource.Error).failure is ResourceFailure.ServiceUnavailable)
    }

    @Test fun cancellationIsRethrown() = runTest {
        try {
            LocationRepositoryImpl(FakeSource(live = flow { throw CancellationException("cancel") }))
                .observeLocation().toList()
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) { }
    }

    @Test fun proximityMapsRegisteredAndTransitions() = runTest {
        val source = FakeSource(proximity = flowOf(
            ProximityReading.Registered("alert-1"),
            ProximityReading.Entered("alert-1"),
            ProximityReading.Exited("alert-1"),
        ))
        val events = LocationRepositoryImpl(source)
            .observeProximity(ProximityZone(37.0, 127.0, 100f)).toList()
        assertEquals(ProximityRequest(37.0, 127.0, 100f), source.request)
        assertEquals(ProximityUpdate.Registered("alert-1"), (events[1] as DataResource.Success).data)
        assertEquals(ProximityUpdate.Entered("alert-1"), (events[2] as DataResource.Success).data)
        assertEquals(ProximityUpdate.Exited("alert-1"), (events[3] as DataResource.Success).data)
    }

    private class FakeSource(
        private val live: Flow<LocationReading?> = flowOf(reading),
        private val last: Flow<LocationReading?> = flowOf(reading),
        private val proximity: Flow<ProximityReading> = flowOf(),
    ) : LocationDataSource {
        var request: ProximityRequest? = null
        override fun isSupported() = true
        override fun observeLocation() = live
        override fun lastKnownLocation() = last
        override fun observeProximity(request: ProximityRequest): Flow<ProximityReading> {
            this.request = request
            return proximity
        }
    }

    private companion object {
        val reading = LocationReading(37.0, 127.0, 3.0, 22f, 0.8f, 123L)
    }
}
