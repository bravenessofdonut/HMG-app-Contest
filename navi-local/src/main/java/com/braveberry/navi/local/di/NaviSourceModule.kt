package com.braveberry.navi.local.di

import com.braveberry.navi.data.datasource.NaviDataSource
import com.braveberry.navi.local.gateway.NaviGatewayFactory
import com.braveberry.navi.local.gateway.PleosNaviGatewayFactory
import com.braveberry.navi.local.impl.NaviDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NaviSourceModule {
    @Binds
    abstract fun bindDataSource(impl: NaviDataSourceImpl): NaviDataSource

    @Binds
    abstract fun bindGatewayFactory(impl: PleosNaviGatewayFactory): NaviGatewayFactory
}
