package com.braveberry.presentation.vehicle.viewmodel

sealed interface Reading<out T> {
    data object Loading : Reading<Nothing>
    data class Ready<T>(val data: T) : Reading<T>
    data class Failed(val explanation: String) : Reading<Nothing>
}

data class VehicleUiState(
    val speedMetersPerSecond: Reading<Float> = Reading.Loading,
    val driverDoorPosition: Reading<Int?> = Reading.Loading
)
