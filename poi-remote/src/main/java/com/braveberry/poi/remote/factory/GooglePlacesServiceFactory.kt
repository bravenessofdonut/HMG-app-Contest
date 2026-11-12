
package com.braveberry.poi.remote.factory

import com.braveberry.poi.remote.api.GooglePlacesApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object GooglePlacesServiceFactory {

    private const val BASE_URL = "https://places.googleapis.com/"

    fun create(): GooglePlacesApi {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                MoshiConverterFactory.create(moshi)
            )
            .build()
            .create(GooglePlacesApi::class.java)
    }
}
