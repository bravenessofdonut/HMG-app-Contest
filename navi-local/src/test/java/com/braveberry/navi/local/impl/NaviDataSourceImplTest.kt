package com.braveberry.navi.local.impl

import com.braveberry.navi.data.model.NaviInstruction
import com.braveberry.navi.data.model.NaviSignal
import com.braveberry.navi.data.model.NaviSignalType
import com.braveberry.navi.local.gateway.NaviGateway
import com.braveberry.navi.local.gateway.NaviGatewayFactory
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NaviDataSourceImplTest {
    @Test fun startIsIdempotentAndStopReleasesOnce() {
        val gateway = FakeGateway()
        val factory = FakeFactory(gateway)
        val source = NaviDataSourceImpl(factory)
        source.start()
        source.start()
        source.stop()
        source.stop()
        assertEquals(1, factory.created)
        assertEquals(1, gateway.initializations)
        assertEquals(1, gateway.releases)
    }

    @Test fun commandsRequireStartedSessionAndArePassedThrough() {
        val gateway = FakeGateway()
        val source = NaviDataSourceImpl(FakeFactory(gateway))
        assertThrows(IllegalStateException::class.java) { source.execute(NaviInstruction.Cancel) }
        source.start()
        source.execute(NaviInstruction.Cancel)
        assertSame(NaviInstruction.Cancel, gateway.lastCommand)
        source.stop()
        assertThrows(IllegalStateException::class.java) { source.execute(NaviInstruction.ReRoute) }
    }

    @Test fun initializationFailureReleasesAndCanRetry() {
        val gateway = FakeGateway().apply { initializeError = SecurityException("permission") }
        val source = NaviDataSourceImpl(FakeFactory(gateway))
        assertThrows(SecurityException::class.java) { source.start() }
        assertEquals(1, gateway.releases)
        gateway.initializeError = null
        source.start()
        assertEquals(2, gateway.initializations)
        source.stop()
        assertEquals(2, gateway.releases)
    }

    @Test fun eventsAreDeliveredToActiveCollectorsOnly() = runTest {
        val gateway = FakeGateway()
        val source = NaviDataSourceImpl(FakeFactory(gateway))
        source.start()
        val awaited = async(start = CoroutineStart.UNDISPATCHED) { source.events().first() }
        gateway.emit(NaviSignal(NaviSignalType.ROUTE_CANCELLED))
        assertEquals(NaviSignalType.ROUTE_CANCELLED, awaited.await().type)
        source.stop()
        assertFalse(gateway.active)
    }

    @Test fun subsequentStartUsesNewGatewayInstance() {
        val one = FakeGateway()
        val two = FakeGateway()
        var next = 0
        val source = NaviDataSourceImpl(NaviGatewayFactory { if (next++ == 0) one else two })
        source.start()
        source.stop()
        source.start()
        assertEquals(1, one.releases)
        assertEquals(1, two.initializations)
        source.stop()
    }

    private class FakeFactory(private val gateway: FakeGateway) : NaviGatewayFactory {
        var created = 0
        override fun create(): NaviGateway { created++; return gateway }
    }

    private class FakeGateway : NaviGateway {
        var initializations = 0
        var releases = 0
        var initializeError: Exception? = null
        var lastCommand: NaviInstruction? = null
        var callback: ((NaviSignal) -> Unit)? = null
        val active get() = callback != null
        override fun initialize(onEvent: (NaviSignal) -> Unit) {
            initializations++
            initializeError?.let { throw it }
            callback = onEvent
        }
        override fun execute(instruction: NaviInstruction) { lastCommand = instruction }
        fun emit(event: NaviSignal) { callback?.invoke(event) }
        override fun release() { releases++; callback = null }
    }
}
