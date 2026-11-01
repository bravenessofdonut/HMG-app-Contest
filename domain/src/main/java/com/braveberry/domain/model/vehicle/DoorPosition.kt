package com.braveberry.domain.model.vehicle

sealed interface DoorPosition {
    data object Unavailable : DoorPosition
    data class Unrecognized(val rawCode: Int) : DoorPosition
}
