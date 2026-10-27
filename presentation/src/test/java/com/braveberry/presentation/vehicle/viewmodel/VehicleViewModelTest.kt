package com.braveberry.presentation.vehicle.viewmodel

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.repository.VehicleRepository
import com.braveberry.domain.usecase.vehicle.ObserveDriverDoorUseCase
import com.braveberry.domain.usecase.vehicle.ObserveSpeedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModelTest {
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun speedAndDoorHaveIndependentOrbitStates() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = FakeRepository()
        repository.speed.tryEmit(DataResource.success(15f))
        repository.door.tryEmit(DataResource.error(IllegalStateException("door offline")))
        val vm = VehicleViewModel(ObserveSpeedUseCase(repository), ObserveDriverDoorUseCase(repository))
        val expected = VehicleUiState(
            speedMetersPerSecond = Reading.Ready(15f),
            driverDoorPosition = Reading.Failed("door offline")
        )
        // Orbit의 reduce 완료를 기다린 뒤 비교함. 실패 시 마지막 상태도 출력함.
        assertEquals(expected, vm.awaitState(expected))
    }

    @Test fun retrySpeedDoesNotResetDoorValue() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = FakeRepository()
        repository.speed.tryEmit(DataResource.success(5f))
        repository.door.tryEmit(DataResource.success(1))
        val vm = VehicleViewModel(ObserveSpeedUseCase(repository), ObserveDriverDoorUseCase(repository))
        // 최초 속도와 도어 값이 모두 반영된 이후에 재시도함.
        val beforeRetry = VehicleUiState(
            speedMetersPerSecond = Reading.Ready(5f),
            driverDoorPosition = Reading.Ready(1)
        )
        assertEquals(beforeRetry, vm.awaitState(beforeRetry))

        repository.speed.tryEmit(DataResource.success(9f))
        vm.retrySpeed()

        val afterRetry = beforeRetry.copy(speedMetersPerSecond = Reading.Ready(9f))
        assertEquals(afterRetry, vm.awaitState(afterRetry))
    }

    private suspend fun VehicleViewModel.awaitState(expected: VehicleUiState): VehicleUiState =
        withTimeoutOrNull(5_000) {
            container.stateFlow.first { it == expected }
        } ?: container.stateFlow.value

    private class FakeRepository : VehicleRepository {
        val speed = MutableSharedFlow<DataResource<Float>>(replay = 1)
        val door = MutableSharedFlow<DataResource<Int?>>(replay = 1)
        override fun observeSpeedMetersPerSecond(): Flow<DataResource<Float>> = speed
        override fun observeDriverDoorPosition(): Flow<DataResource<Int?>> = door
    }
}
