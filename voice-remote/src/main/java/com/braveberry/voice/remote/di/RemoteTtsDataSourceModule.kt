package com.braveberry.voice.remote.di

import com.braveberry.voice.data.datasource.AudioOutputDataSource
import com.braveberry.voice.data.datasource.SpeechSynthesisDataSource
import com.braveberry.voice.remote.impl.AudioTrackOutputDataSourceImpl
import com.braveberry.voice.remote.impl.GeminiSpeechSynthesisDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

/** 합성 SDK를 교체할 때는 이 바인딩만 바꿈. data의 재생 규칙은 그대로 유지됨. */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal abstract class RemoteTtsDataSourceModule {

    @Binds
    abstract fun bindSpeechSynthesisDataSource(
        impl: GeminiSpeechSynthesisDataSourceImpl
    ): SpeechSynthesisDataSource

    @Binds
    abstract fun bindAudioOutputDataSource(
        impl: AudioTrackOutputDataSourceImpl
    ): AudioOutputDataSource
}
