package com.braveberry.voice.data.di

import com.braveberry.domain.repository.RemoteTtsRepository
import com.braveberry.voice.data.impl.RemoteTtsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object RemoteTtsRepositoryModule {

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
