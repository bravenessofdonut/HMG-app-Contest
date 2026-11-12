package com.braveberry.navi.data.di

import com.braveberry.domain.repository.NaviRepository
import com.braveberry.navi.data.impl.NaviRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NaviRepositoryModule {
    @Binds
    abstract fun bindNaviRepository(impl: NaviRepositoryImpl): NaviRepository
}
