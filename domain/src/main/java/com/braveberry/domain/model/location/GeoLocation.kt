package com.braveberry.domain.model.location

/** SDK 타입에 의존하지 않는 위치 스냅샷. 위도/경도는 WGS84 좌표. */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val headingDegrees: Float,
    val confidence: Float,
    val timestampMillis: Long,
)
