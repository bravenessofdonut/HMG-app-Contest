package com.braveberry.domain.usecase.navi

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.navi.NaviCommand
import com.braveberry.domain.model.navi.NaviEvent
import com.braveberry.domain.model.navi.NaviEventType
import com.braveberry.domain.model.navi.NaviPlace
import com.braveberry.domain.repository.NaviRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class NaviUseCaseTest {
    private val destination = NaviPlace(37.5, 127.0, "poi", "목적지")

    @Test fun commandsAreDelegatedWithoutChanges() = runTest {
        val repository = FakeRepository()
        val command = NaviCommand.RequestRoute(destination)
        assertSame(repository.success, ExecuteNaviCommandUseCase(repository)(command).toList().single())
        assertSame(command, repository.received)
        assertSame(repository.success, StartNaviUseCase(repository)().toList().single())
        assertSame(repository.success, StopNaviUseCase(repository)().toList().single())
        assertEquals(1, repository.started)
        assertEquals(1, repository.stopped)
    }

    @Test fun eventFlowIsNotModified() = runTest {
        val repository = FakeRepository()
        assertSame(repository.event, ObserveNaviEventsUseCase(repository)().toList().single())
    }

    @Test fun invalidCoordinatesAndUnsupportedWaypointCountAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NaviPlace(Double.NaN, 127.0, "poi", "목적지")
        }
        assertThrows(IllegalArgumentException::class.java) {
            NaviCommand.RequestRoute(destination, listOf(destination, destination))
        }
    }

    private class FakeRepository : NaviRepository {
        val success: DataResource<Unit> = DataResource.success(Unit)
        val event: DataResource<NaviEvent> = DataResource.success(
            NaviEvent(NaviEventType.ROUTING_COMPLETED)
        )
        var received: NaviCommand? = null
        var started = 0
        var stopped = 0
        override fun start(): Flow<DataResource<Unit>> { started++; return flowOf(success) }
        override fun stop(): Flow<DataResource<Unit>> { stopped++; return flowOf(success) }
        override fun execute(command: NaviCommand): Flow<DataResource<Unit>> {
            received = command
            return flowOf(success)
        }
        override fun observeEvents(): Flow<DataResource<NaviEvent>> = flowOf(event)
    }
}
