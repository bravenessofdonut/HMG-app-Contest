package com.braveberry.voice.remote.di

import com.braveberry.voice.data.datasource.TtsRemoteDataSource
import com.braveberry.voice.remote.api.FirebaseGeminiTtsApiService
import com.braveberry.voice.remote.api.GeminiTtsApiService
import com.braveberry.voice.remote.impl.TtsRemoteDataSourceImpl
import com.braveberry.voice.remote.player.PcmAudioPlayerImpl
import com.braveberry.voice.remote.player.PcmAudioPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsRemoteDataSourceModule {

    @Binds
    abstract fun bindTtsRemoteDataSource(
        impl: TtsRemoteDataSourceImpl
    ): TtsRemoteDataSource

    @Binds
    abstract fun bindPcmAudioPlayer(
        impl: PcmAudioPlayerImpl
    ): PcmAudioPlayer

    @Binds
    abstract fun bindGeminiTtsApiService(
        impl: FirebaseGeminiTtsApiService
    ): GeminiTtsApiService
}
