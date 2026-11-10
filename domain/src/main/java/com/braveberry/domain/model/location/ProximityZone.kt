package com.braveberry.domain.model.location

data class ProximityZone(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid latitude" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid longitude" }
        require(radiusMeters.isFinite() && radiusMeters > 0f) { "Invalid radius" }
    }
}
