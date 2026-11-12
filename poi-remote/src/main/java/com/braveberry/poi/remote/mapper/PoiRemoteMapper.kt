
package com.braveberry.poi.remote.mapper

import com.braveberry.poi.data.model.PoiDataModel
import com.braveberry.poi.remote.model.PlacesSearchResponseDto

fun PlacesSearchResponseDto.toDataModel(): PoiDataModel? {
    val place = places.firstOrNull() ?: return null

    val poi = place.id?.takeIf { it.isNotBlank() }
        ?: return null

    val latitude = place.location?.latitude
        ?: return null

    val longitude = place.location.longitude
        ?: return null

    return PoiDataModel(
        poi = poi,
        latitude = latitude,
        longitude = longitude
    )
}
