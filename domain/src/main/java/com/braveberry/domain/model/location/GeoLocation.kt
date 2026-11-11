package com.braveberry.domain.model.location

/** SDK와 분리된 위치 정보임. 위도·경도는 WGS84 좌표계 사용함. */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val headingDegrees: Float,
    val confidence: Float,
    val timestampMillis: Long,
)
