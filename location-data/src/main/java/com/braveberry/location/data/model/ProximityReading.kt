package com.braveberry.location.data.model

data class ProximityRequest(val latitude: Double, val longitude: Double, val radiusMeters: Float)

sealed interface ProximityReading {
    data class Registered(val alertId: String) : ProximityReading
    data class Entered(val alertId: String) : ProximityReading
    data class Exited(val alertId: String) : ProximityReading
}
