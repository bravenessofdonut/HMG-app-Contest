package com.braveberry.voice.data.di

import com.braveberry.domain.repository.RemoteTtsRepository
import com.braveberry.voice.data.impl.RemoteTtsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteTtsRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRemoteTtsRepository(
        impl: RemoteTtsRepositoryImpl
    ): RemoteTtsRepository
}
