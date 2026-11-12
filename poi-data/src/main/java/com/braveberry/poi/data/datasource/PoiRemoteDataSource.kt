package com.braveberry.poi.data.datasource

import com.braveberry.poi.data.model.PoiDataModel

interface PoiRemoteDataSource {
    suspend fun getPoi(placeName: String): PoiDataModel?
}