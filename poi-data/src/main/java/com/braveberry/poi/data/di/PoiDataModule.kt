package com.braveberry.poi.data.di

import com.braveberry.domain.repository.PoiRepository
import com.braveberry.poi.data.impl.PoiRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PoiDataModule {

    @Binds
    abstract fun bindPoiRepository(
        impl: PoiRepositoryImpl
    ): PoiRepository
}