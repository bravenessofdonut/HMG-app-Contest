package com.braveberry.domain.model.poi

data class PoiInfo(
    val poi: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?
)