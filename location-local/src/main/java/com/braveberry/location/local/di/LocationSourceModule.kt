package com.braveberry.location.local.di

import com.braveberry.location.data.datasource.LocationDataSource
import com.braveberry.location.local.gateway.FusedLocationGatewayFactory
import com.braveberry.location.local.gateway.PleosFusedLocationGatewayFactory
import com.braveberry.location.local.impl.LocationDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationSourceModule {
    @Binds
    abstract fun bindLocationDataSource(impl: LocationDataSourceImpl): LocationDataSource

    @Binds
    abstract fun bindGatewayFactory(impl: PleosFusedLocationGatewayFactory): FusedLocationGatewayFactory
}
