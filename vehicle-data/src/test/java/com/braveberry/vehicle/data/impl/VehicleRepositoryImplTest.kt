package com.braveberry.vehicle.data.impl

import com.braveberry.domain.model.vehicle.DoorPosition
import com.braveberry.data_resource.DataResource
import com.braveberry.vehicle.data.datasource.VehicleDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleRepositoryImplTest {
    @Test fun speedEmitsLoadingThenSuccess() = runTest {
        val repo = VehicleRepositoryImpl(FakeVehicleDataSource(speed = flowOf(10f)))
        val events = repo.observeSpeedMetersPerSecond().toList()
        assertTrue(events.first() is DataResource.Loading)
        assertEquals(10f, (events.last() as DataResource.Success).data)
    }

    @Test fun speedFailureDoesNotPreventIndependentDoorRead() = runTest {
        val repo = VehicleRepositoryImpl(FakeVehicleDataSource(
            speed = flow { throw IllegalStateException("service unavailable") },
            door = flowOf(1)
        ))
        assertTrue(repo.observeSpeedMetersPerSecond().toList().last() is DataResource.Error)
        assertEquals(DoorPosition.Unrecognized(1), (repo.observeDriverDoorPosition().toList().last() as DataResource.Success).data)
    }

    @Test fun missingDoorReadingIsAnObservedValue() = runTest {
        val repo = VehicleRepositoryImpl(FakeVehicleDataSource(door = flowOf(null)))
        val events = repo.observeDriverDoorPosition().toList()
        assertTrue(events.first() is DataResource.Loading)
        assertEquals(DoorPosition.Unavailable, (events.last() as DataResource.Success).data)
    }

    @Test fun cancellationPropagatesInsteadOfBecomingUiError() = runTest {
        val repo = VehicleRepositoryImpl(FakeVehicleDataSource(
            speed = flow { throw CancellationException("cancel") }
        ))
        try {
            repo.observeSpeedMetersPerSecond().toList()
            throw AssertionError("Expected coroutine cancellation")
        } catch (_: CancellationException) {
            // 취소는 UI에 표시할 일반 오류가 아님.
        }
    }

    private class FakeVehicleDataSource(
        private val speed: Flow<Float> = flowOf(0f),
        private val door: Flow<Int?> = flowOf(null)
    ) : VehicleDataSource {
        override fun speedMetersPerSecond() = speed
        override fun driverDoorPosition() = door
    }
}
