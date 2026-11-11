package com.braveberry.navi.local.impl

import com.braveberry.navi.data.datasource.NaviDataSource
import com.braveberry.navi.data.model.NaviInstruction
import com.braveberry.navi.data.model.NaviSignal
import com.braveberry.navi.local.gateway.NaviGateway
import com.braveberry.navi.local.gateway.NaviGatewayFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
internal class NaviDataSourceImpl @Inject constructor(
    private val factory: NaviGatewayFactory
) : NaviDataSource {
    private var gateway: NaviGateway? = null

    // SDK 콜백 스레드를 막지 않도록 제한된 버퍼만 사용함.
    // 구독 전 이벤트는 보관하지 않으므로 관찰을 시작한 후 명령을 보내야 함.
    private val messages = MutableSharedFlow<NaviSignal>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Synchronized
    override fun start() {
        if (gateway != null) return
        val created = factory.create()
        try {
            created.initialize { messages.tryEmit(it) }
            gateway = created
        } catch (error: Exception) {
            try { created.release() } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
            throw error
        }
    }

    @Synchronized
    override fun stop() {
        val current = gateway ?: return
        gateway = null
        current.release()
    }

    @Synchronized
    override fun execute(instruction: NaviInstruction) {
        val current = gateway ?: throw IllegalStateException("NaviHelper is not initialized")
        current.execute(instruction)
    }

    override fun events() = messages.asSharedFlow()
}
