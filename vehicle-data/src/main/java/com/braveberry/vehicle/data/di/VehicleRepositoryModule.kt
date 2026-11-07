package com.braveberry.vehicle.data.di

import com.braveberry.domain.repository.VehicleRepository
import com.braveberry.vehicle.data.impl.VehicleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 구현 소유 모듈이 계약 연결까지 책임짐. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class VehicleRepositoryModule {
    @Binds
    abstract fun bindVehicleRepository(impl: VehicleRepositoryImpl): VehicleRepository
}
