package com.braveberry.navi.data.impl

import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.domain.model.navi.NaviCommand
import com.braveberry.domain.model.navi.NaviEventType
import com.braveberry.domain.model.navi.NaviPlace
import com.braveberry.navi.data.datasource.NaviDataSource
import com.braveberry.navi.data.model.NaviInstruction
import com.braveberry.navi.data.model.NaviSignal
import com.braveberry.navi.data.model.NaviSignalType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NaviRepositoryImplTest {
    private val point = NaviPlace(37.5, 127.0, "poi", "목적지")

    @Test fun commandReturnsLoadingThenSentAndMapsDestination() = runTest {
        val source = FakeSource()
        val repo = NaviRepositoryImpl(source)
        val events = repo.execute(NaviCommand.RequestRoute(point, listOf(point))).toList()
        assertTrue(events.first() is DataResource.Loading)
        assertTrue(events.last() is DataResource.Success)
        assertEquals(NaviInstruction.Route(point.toDataForTest(), listOf(point.toDataForTest())), source.lastCommand)
    }

    @Test fun googleProviderIsPreservedForWaypoint() = runTest {
        val source = FakeSource()
        val waypoint = point.copy(poiId = "ChIJ_test_place_id", poiProvider = "Google")
        NaviRepositoryImpl(source).execute(NaviCommand.RequestRoute(point, listOf(waypoint))).toList()

        val route = source.lastCommand as NaviInstruction.Route
        assertEquals("Google", route.waypoints.single().poiProvider)
        assertEquals("ChIJ_test_place_id", route.waypoints.single().poiId)
        assertEquals("", route.destination.poiProvider)
    }

    @Test fun serviceCommandsAreLazyAndIndependent() = runTest {
        val source = FakeSource()
        val repo = NaviRepositoryImpl(source)
        val start = repo.start()
        assertEquals(0, source.started)
        start.toList()
        repo.stop().toList()
        assertEquals(1, source.started)
        assertEquals(1, source.stopped)
    }

    @Test fun sdkCallbackIsConvertedWithoutLosingSpeedLimit() = runTest {
        val source = FakeSource()
        source.emissions = flowOf(NaviSignal(NaviSignalType.SPEED_LIMIT, speedLimitKph = 70))
        val events = NaviRepositoryImpl(source).observeEvents().toList()
        val location = (events.last() as DataResource.Success).data
        assertEquals(NaviEventType.SPEED_LIMIT, location.type)
        assertEquals(70, location.speedLimitKph)
    }

    @Test fun eventTypesStayInSyncWithDomain() {
        assertEquals(NaviEventType.entries.map { it.name }, NaviSignalType.entries.map { it.name })
    }

    @Test fun permissionsAreClassifiedAndOriginalCausePreserved() = runTest {
        val denied = SecurityException("NAVI_ROUTE")
        val source = FakeSource().apply { error = denied }
        val events = NaviRepositoryImpl(source).execute(NaviCommand.CancelRoute).toList()
        val failure = (events.last() as DataResource.Error).failure
        assertTrue(failure is ResourceFailure.PermissionRequired)
        assertSame(denied, failure.cause)
    }

    @Test fun unavailableServiceIsClassified() = runTest {
        val source = FakeSource().apply { error = IllegalStateException("not started") }
        val failure = (NaviRepositoryImpl(source).execute(NaviCommand.ReRoute).toList().last() as DataResource.Error).failure
        assertTrue(failure is ResourceFailure.ServiceUnavailable)
    }

    @Test fun cancelledFlowPropagatesCancellation() = runTest {
        val source = FakeSource().apply { emissions = flow { throw CancellationException("cancel") } }
        try {
            NaviRepositoryImpl(source).observeEvents().toList()
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) { }
    }

    private fun NaviPlace.toDataForTest() = com.braveberry.navi.data.model.NaviPlaceData(
        latitude, longitude, poiId, poiName, poiSubId, address, poiProvider
    )

    private class FakeSource : NaviDataSource {
        var started = 0
        var stopped = 0
        var lastCommand: NaviInstruction? = null
        var error: Exception? = null
        var emissions: Flow<NaviSignal> = flowOf(NaviSignal(NaviSignalType.ROUTING_COMPLETED))
        override fun start() { started++ }
        override fun stop() { stopped++ }
        override fun execute(instruction: NaviInstruction) {
            error?.let { throw it }
            lastCommand = instruction
        }
        override fun events() = emissions
    }
}
