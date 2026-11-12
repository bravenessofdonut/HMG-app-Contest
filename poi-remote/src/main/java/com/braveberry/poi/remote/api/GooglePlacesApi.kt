
package com.braveberry.poi.remote.api

import com.braveberry.poi.remote.model.PlacesSearchRequestDto
import com.braveberry.poi.remote.model.PlacesSearchResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GooglePlacesApi {

    @POST("v1/places:searchText")
    suspend fun searchPlaces(
        @Header("X-Goog-Api-Key")
        apiKey: String,

        @Header("X-Goog-FieldMask")
        fieldMask: String =
            "places.id,places.location,places.formattedAddress",

        @Body
        request: PlacesSearchRequestDto
    ): PlacesSearchResponseDto
}