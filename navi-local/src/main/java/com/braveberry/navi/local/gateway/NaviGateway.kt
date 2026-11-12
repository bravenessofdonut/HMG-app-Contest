package com.braveberry.navi.local.gateway

import com.braveberry.navi.data.model.NaviInstruction
import com.braveberry.navi.data.model.NaviSignal

internal fun interface NaviGatewayFactory {
    fun create(): NaviGateway
}

internal interface NaviGateway {
    fun initialize(onEvent: (NaviSignal) -> Unit)
    fun execute(instruction: NaviInstruction)
    fun release()
}
