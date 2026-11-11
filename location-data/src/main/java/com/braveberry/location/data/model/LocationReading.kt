package com.braveberry.location.data.model

data class LocationReading(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val heading: Float,
    val confidence: Float,
    val timestampMillis: Long,
)
