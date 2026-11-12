package com.braveberry.poi.remote.di

import com.braveberry.poi.data.datasource.PoiRemoteDataSource
import com.braveberry.poi.remote.api.GooglePlacesApi
import com.braveberry.poi.remote.factory.GooglePlacesServiceFactory
import com.braveberry.poi.remote.impl.PoiRemoteDataSourceImpl
import com.braveberry.poi.remote.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PoiRemoteModule {

    @Provides
    @Singleton
    fun provideGooglePlacesApi(): GooglePlacesApi =
        GooglePlacesServiceFactory.create()

    @Provides
    @Singleton
    fun providePoiRemoteDataSource(
        api: GooglePlacesApi
    ): PoiRemoteDataSource =
        PoiRemoteDataSourceImpl(
            api = api,
            apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
        )
}