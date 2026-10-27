package com.braveberry.domain.usecase.vehicle

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleUseCaseTest {
    @Test fun observeSpeedDelegatesUnmodifiedValues() = runTest {
        val repository = FakeVehicleRepository()
        val emissions = ObserveSpeedUseCase(repository)().toList()
        assertEquals(1, emissions.size)
        assertSame(repository.speed, emissions.first())
    }

    @Test fun observeDoorPreservesMissingValues() = runTest {
        val repository = FakeVehicleRepository()
        val emissions = ObserveDriverDoorUseCase(repository)().toList()
        assertTrue(emissions.single() is DataResource.Success)
        assertEquals(null, (emissions.single() as DataResource.Success).data)
    }

    private class FakeVehicleRepository : VehicleRepository {
        val speed: DataResource<Float> = DataResource.success(8f)
        override fun observeSpeedMetersPerSecond(): Flow<DataResource<Float>> = flowOf(speed)
        override fun observeDriverDoorPosition(): Flow<DataResource<Int?>> = flowOf(DataResource.success(null))
    }
}
