package com.braveberry.domain.repository

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.poi.PoiInfo
import kotlinx.coroutines.flow.Flow

interface PoiRepository {
    fun getPoi(
        placeName: String
    ): Flow<DataResource<PoiInfo?>>
}