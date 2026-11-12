package com.braveberry.domain.repository

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.navi.NaviCommand
import com.braveberry.domain.model.navi.NaviEvent
import kotlinx.coroutines.flow.Flow

interface NaviRepository {
    fun start(): Flow<DataResource<Unit>>
    fun stop(): Flow<DataResource<Unit>>
    // 명령 호출 성공과 경로 탐색 성공은 다름. 탐색 결과는 observeEvents에서 확인함.
    fun execute(command: NaviCommand): Flow<DataResource<Unit>>
    fun observeEvents(): Flow<DataResource<NaviEvent>>
}
