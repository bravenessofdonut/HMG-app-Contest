package com.braveberry.poi.data.mapper

import com.braveberry.domain.model.poi.PoiInfo
import com.braveberry.poi.data.model.PoiDataModel

fun PoiDataModel.toDomain(): PoiInfo =
    PoiInfo(
        poi = poi,
        latitude = latitude,
        longitude = longitude,
        address = address
    )