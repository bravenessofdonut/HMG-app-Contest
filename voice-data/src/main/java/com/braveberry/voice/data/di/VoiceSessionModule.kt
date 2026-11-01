package com.braveberry.voice.data.di

import com.braveberry.domain.repository.VoiceRepository
import com.braveberry.domain.usecase.voice.VoiceSession
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
    fun provideSession(engines: Provider<VoiceRepository>, lifecycle: ActivityRetainedLifecycle): VoiceSession {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val session = VoiceSession(engines::get, scope)
        lifecycle.addOnClearedListener {
            scope.cancel()
            session.disconnect()
        }
        return session
    }
}
