package com.braveberry.ai.remote.di

import com.braveberry.ai.remote.api.GeminiApiService
import com.braveberry.ai.remote.factory.GeminiServiceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiApiModule {

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService {
        return GeminiServiceFactory.create()
    }

}

