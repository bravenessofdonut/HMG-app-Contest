package com.braveberry.presentation.vehicle.viewmodel

import com.braveberry.data_resource.DataResource

import com.braveberry.data_resource.ResourceFailure
import com.braveberry.domain.model.vehicle.DoorPosition
import com.braveberry.domain.usecase.voice.Playback

sealed interface Reading<out T> {
    data object Loading : Reading<Nothing>
    data class Ready<T>(val data: T) : Reading<T>
    data class Failed(val reason: FailureReason) : Reading<Nothing>
}

data class VehicleUiState(
    val speedMetersPerSecond: Reading<Float> = Reading.Loading,
    val driverDoorPosition: Reading<DoorPosition> = Reading.Loading,
    val playback: Playback = Playback.Idle
)
enum class FailureReason { PERMISSION, UNSUPPORTED, TEMPORARY }

internal fun ResourceFailure.toFailureReason(): FailureReason = when (this) {
    is ResourceFailure.PermissionRequired -> FailureReason.PERMISSION
    is ResourceFailure.Unsupported -> FailureReason.UNSUPPORTED
    is ResourceFailure.ServiceUnavailable -> FailureReason.TEMPORARY
}

internal fun <T> DataResource<T>.toReading(): Reading<T> = when (this) {
    is DataResource.Loading -> Reading.Loading
    is DataResource.Success -> Reading.Ready(data)
    is DataResource.Error -> Reading.Failed(failure.toFailureReason())
}
