package com.braveberry.poi.remote.impl

import com.braveberry.poi.data.datasource.PoiRemoteDataSource
import com.braveberry.poi.data.model.PoiDataModel
import com.braveberry.poi.remote.api.GooglePlacesApi
import com.braveberry.poi.remote.mapper.toDataModel
import com.braveberry.poi.remote.model.PlacesSearchRequestDto

class PoiRemoteDataSourceImpl(
    private val api: GooglePlacesApi,
    private val apiKey: String
) : PoiRemoteDataSource {

    override suspend fun getPoi(
        placeName: String
    ): PoiDataModel? {
        val response = api.searchPlaces(
            apiKey = apiKey,
            request = PlacesSearchRequestDto(
                textQuery = placeName
            )
        )

        return response.toDataModel()
    }
}
