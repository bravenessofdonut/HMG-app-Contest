package com.braveberry.voice.data.di

import com.braveberry.domain.repository.VoiceRepository
import com.braveberry.domain.usecase.voice.VoiceSession
import com.braveberry.domain.usecase.voice.VoiceSessionOwner
import com.braveberry.voice.data.impl.VoiceSessionImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object VoiceSessionModule {
    @Provides
    @ActivityRetainedScoped
    fun provideRuntime(engines: Provider<VoiceRepository>, lifecycle: ActivityRetainedLifecycle): VoiceSessionImpl {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val session = VoiceSessionImpl(engines::get, scope)
        lifecycle.addOnClearedListener {
            session.shutdown()
            scope.cancel()
        }
        return session
    }

    @Provides
    fun provideSession(runtime: VoiceSessionImpl): VoiceSession = runtime

    @Provides
    fun provideSessionOwner(runtime: VoiceSessionImpl): VoiceSessionOwner = runtime
}
