package com.braveberry.navi.local.gateway

import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest
import ai.pleos.playground.navi.helper.NaviHelper
import com.braveberry.navi.data.model.NaviInstruction
import com.braveberry.navi.data.model.NaviPlaceData
import com.braveberry.navi.data.model.NaviSignalType
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class PleosNaviGatewayTest {
    private val helper = mockk<NaviHelper>(relaxed = true)
    private val gateway = PleosNaviGateway(helper)
    private val destination = NaviPlaceData(37.5, 127.0, "poi", "목적지", "0", "서울")

    @Test fun requestsUseDocumentedSdkMethods() {
        gateway.initialize { }
        gateway.execute(NaviInstruction.Route(destination, emptyList()))
        gateway.execute(NaviInstruction.Route(destination, listOf(destination)))
        gateway.execute(NaviInstruction.FreeRoute)
        gateway.execute(NaviInstruction.AddFirstWaypoint(destination))
        gateway.execute(NaviInstruction.RemoveFirstWaypoint)
        gateway.execute(NaviInstruction.Cancel)
        gateway.execute(NaviInstruction.ReRoute)
        gateway.execute(NaviInstruction.RouteOptionScreen)
        gateway.execute(NaviInstruction.Trip(destination))
        gateway.execute(NaviInstruction.CurrentLocation)
        gateway.execute(NaviInstruction.RouteState)
        gateway.execute(NaviInstruction.Bookmarks)
        gateway.execute(NaviInstruction.RecentDestinations)
        gateway.execute(NaviInstruction.Destination)
        gateway.execute(NaviInstruction.Waypoints)
        gateway.execute(NaviInstruction.Tbt)
        gateway.execute(NaviInstruction.ChargerOperators)
        verify(exactly = 1) { helper.requestRoute(any()) }
        verify(exactly = 1) { helper.requestRouteWithWaypoints(any()) }
        verify(exactly = 1) { helper.changeRouteOption(any()) }
        verify(exactly = 1) { helper.addWaypoint(any()) }
        verify(exactly = 1) { helper.removeWaypoint(any()) }
        verify(exactly = 1) { helper.cancelRoute() }
        verify(exactly = 1) { helper.requestReRoute() }
        verify(exactly = 1) { helper.requestNavigationScreen(any()) }
        verify(exactly = 1) { helper.requestTrip(any()) }
        verify(exactly = 1) { helper.getCurrentLocationInfo() }
        verify(exactly = 1) { helper.getRouteStateInfo() }
        verify(exactly = 1) { helper.getBookmarkInfo() }
        verify(exactly = 1) { helper.getRecentDestinationInfo() }
        verify(exactly = 1) { helper.getDestinationInfo() }
        verify(exactly = 1) { helper.getWaypointInfo() }
        verify(exactly = 1) { helper.getTBTInfo() }
        verify(exactly = 1) { helper.getChargerOperatorInfo() }
        gateway.release()
        verify(exactly = 1) { helper.removeListener(gateway) }
        verify(exactly = 1) { helper.release() }
    }

    @Test fun googlePoiIdAndProviderReachWaypointSdkRequests() {
        val googleWaypoint = destination.copy(poiId = "ChIJ_test_place_id", poiProvider = "Google")
        gateway.initialize { }

        gateway.execute(NaviInstruction.Route(destination, listOf(googleWaypoint)))
        val routeRequest = slot<RouteWithWaypointsRequest>()
        verify(exactly = 1) { helper.requestRouteWithWaypoints(capture(routeRequest)) }
        assertEquals("Google", routeRequest.captured.waypoints.single().poiProvider)
        assertEquals("ChIJ_test_place_id", routeRequest.captured.waypoints.single().poiId)

        gateway.execute(NaviInstruction.AddFirstWaypoint(googleWaypoint))
        val waypointRequest = slot<RequestWaypointInfo>()
        verify(exactly = 1) { helper.addWaypoint(capture(waypointRequest)) }
        assertEquals("Google", waypointRequest.captured.poiProvider)
        assertEquals("ChIJ_test_place_id", waypointRequest.captured.poiId)
        gateway.release()
    }

    @Test fun callbacksAreForwardedAndDuplicateSpeedLimitsIgnored() {
        val received = mutableListOf<NaviSignalType>()
        gateway.initialize { received += it.type }
        gateway.onRouteCancelled()
        gateway.onRoutingCompleted()
        gateway.onCurrentRoadSpeedLimit(60)
        gateway.onCurrentRoadSpeedLimit(60)
        gateway.onCurrentRoadSpeedLimit(80)
        assertEquals(listOf(
            NaviSignalType.ROUTE_CANCELLED,
            NaviSignalType.ROUTING_COMPLETED,
            NaviSignalType.SPEED_LIMIT,
            NaviSignalType.SPEED_LIMIT
        ), received)
        gateway.release()
        gateway.onRoutingCompleted()
        assertEquals(4, received.size)
    }
}
