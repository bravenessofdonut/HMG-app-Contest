package com.braveberry.voice.local.di

import com.braveberry.voice.data.datasource.VoiceDataSource
import com.braveberry.voice.local.impl.PleosVoiceDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class VoiceDataSourceModule {
    @Binds
    @ViewModelScoped
    abstract fun bindVoiceDataSource(impl: PleosVoiceDataSourceImpl): VoiceDataSource
}
