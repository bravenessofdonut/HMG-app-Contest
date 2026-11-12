package com.braveberry.poi.remote.model

data class PlacesSearchResponseDto(
    val places: List<PlaceDto> = emptyList()
)

data class PlaceDto(
    val id: String? = null,
    val location: PlaceLocationDto? = null
)

data class PlaceLocationDto(
    val latitude: Double? = null,
    val longitude: Double? = null
)