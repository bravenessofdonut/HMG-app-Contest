package com.braveberry.domain.usecase.location

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.location.GeoLocation
import com.braveberry.domain.model.location.ProximityUpdate
import com.braveberry.domain.model.location.ProximityZone
import com.braveberry.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class LocationUseCaseTest {
    @Test fun useCasesDelegateWithoutChangingDataResource() = runTest {
        val repo = FakeRepository()
        assertSame(repo.support, CheckLocationSupportUseCase(repo)().toList().single())
        assertSame(repo.location, ObserveLocationUseCase(repo)().toList().single())
        assertSame(repo.location, GetLastKnownLocationUseCase(repo)().toList().single())
        val zone = ProximityZone(37.5, 127.1, 200f)
        assertSame(repo.proximity, ObserveProximityUseCase(repo)(zone).toList().single())
        assertEquals(zone, repo.lastZone)
    }

    @Test fun proximityZoneRejectsInvalidCoordinatesAndRadius() {
        assertThrows(IllegalArgumentException::class.java) { ProximityZone(Double.NaN, 0.0, 10f) }
        assertThrows(IllegalArgumentException::class.java) { ProximityZone(91.0, 0.0, 10f) }
        assertThrows(IllegalArgumentException::class.java) { ProximityZone(0.0, 181.0, 10f) }
        assertThrows(IllegalArgumentException::class.java) { ProximityZone(0.0, 0.0, 0f) }
        assertThrows(IllegalArgumentException::class.java) { ProximityZone(0.0, 0.0, Float.NaN) }
    }

    private class FakeRepository : LocationRepository {
        val support: DataResource<Boolean> = DataResource.success(true)
        val location: DataResource<GeoLocation?> = DataResource.success(null)
        val proximity: DataResource<ProximityUpdate> = DataResource.success(ProximityUpdate.Registered("id"))
        var lastZone: ProximityZone? = null
        override fun checkSupport(): Flow<DataResource<Boolean>> = flowOf(support)
        override fun observeLocation(): Flow<DataResource<GeoLocation?>> = flowOf(location)
        override fun getLastKnownLocation(): Flow<DataResource<GeoLocation?>> = flowOf(location)
        override fun observeProximity(zone: ProximityZone): Flow<DataResource<ProximityUpdate>> {
            lastZone = zone
            return flowOf(proximity)
        }
    }
}
