package com.braveberry.ai.remote.di

import com.braveberry.ai.data.datasource.AiRemoteDataSource
import com.braveberry.ai.remote.impl.AiRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AiRemoteDataSourceModule {

    @Binds
    abstract fun bindAiRemoteDataSource(
        impl: AiRemoteDataSourceImpl
    ): AiRemoteDataSource
}