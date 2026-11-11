package com.braveberry.voice.remote.di

import com.braveberry.domain.repository.RemoteTtsRepository
import com.braveberry.voice.remote.api.GeminiTtsClient
import com.braveberry.voice.remote.api.GeminiTtsClientImpl
import com.braveberry.voice.remote.impl.RemoteTtsRepositoryImpl
import com.braveberry.voice.remote.player.AudioTrackPcmPlayer
import com.braveberry.voice.remote.player.PcmAudioPlayer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
internal abstract class VoiceRemoteModule {

    @Binds
    abstract fun bindGeminiTtsClient(
        impl: GeminiTtsClientImpl
    ): GeminiTtsClient

    @Binds
    abstract fun bindPcmAudioPlayer(
        impl: AudioTrackPcmPlayer
    ): PcmAudioPlayer
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object RemoteTtsRuntimeModule {

    /**
     * 인스턴스와 해제 시점을 같은 ActivityRetained 수명에 묶음.
     * 화면(ViewModel)은 release하지 않음. 재진입 시 해제된 인스턴스를 받는 문제를 막음.
     */
    @Provides
    @ActivityRetainedScoped
    fun provideRemoteTtsRepository(
        impl: RemoteTtsRepositoryImpl,
        lifecycle: ActivityRetainedLifecycle
    ): RemoteTtsRepository {
        lifecycle.addOnClearedListener { impl.release() }
        return impl
    }
}
