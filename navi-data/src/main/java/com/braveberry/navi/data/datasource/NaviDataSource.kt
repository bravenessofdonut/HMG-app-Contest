package com.braveberry.navi.data.datasource

import com.braveberry.navi.data.model.NaviInstruction
import com.braveberry.navi.data.model.NaviSignal
import kotlinx.coroutines.flow.Flow

// SDK의 시작·종료와 명령, 콜백 스트림 계약만 정의함.
interface NaviDataSource {
    fun start()
    fun stop()
    fun execute(instruction: NaviInstruction)
    fun events(): Flow<NaviSignal>
}
