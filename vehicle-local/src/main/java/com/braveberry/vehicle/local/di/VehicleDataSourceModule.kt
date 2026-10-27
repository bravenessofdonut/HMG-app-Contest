package com.braveberry.vehicle.local.di

import com.braveberry.vehicle.data.datasource.VehicleDataSource
import com.braveberry.vehicle.local.impl.VehicleDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 실제 차량 SDK DataSource 구현을 연결함. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class VehicleDataSourceModule {
    @Binds
    abstract fun bindVehicleDataSource(impl: VehicleDataSourceImpl): VehicleDataSource
}
