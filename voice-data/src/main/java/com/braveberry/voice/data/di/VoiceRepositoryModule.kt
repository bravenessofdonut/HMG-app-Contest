package com.braveberry.voice.data.di

import com.braveberry.domain.repository.VoiceRepository
import com.braveberry.voice.data.impl.VoiceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
internal abstract class VoiceRepositoryModule {
    @Binds
    abstract fun bindVoiceRepository(impl: VoiceRepositoryImpl): VoiceRepository
}
