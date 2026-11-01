package com.braveberry.voice.data.di

import com.braveberry.domain.repository.VoiceRepository
import com.braveberry.voice.data.impl.VoiceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/** 하나의 VoiceViewModel에서 하나의 음성 세션을 사용함. */
@Module
@InstallIn(ViewModelComponent::class)
internal abstract class VoiceRepositoryModule {
    @Binds
    @ViewModelScoped
    abstract fun bindVoiceRepository(impl: VoiceRepositoryImpl): VoiceRepository
}
