package com.braveberry.navi.local.gateway

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.data.CameraAlert
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.ChargerOperator
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RecentDestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.data.TripRequest
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.braveberry.navi.data.model.NaviInstruction
import com.braveberry.navi.data.model.NaviPlaceData
import com.braveberry.navi.data.model.NaviSignal
import com.braveberry.navi.data.model.NaviSignalType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class PleosNaviGatewayFactory @Inject constructor(
    @param:ApplicationContext private val context: Context
) : NaviGatewayFactory {
    override fun create(): NaviGateway = PleosNaviGateway(NaviHelper(context.applicationContext))
}

// SDK 초기화 상태와 콜백 연결을 한 인스턴스에서 관리함.
internal class PleosNaviGateway(private val helper: NaviHelper) : NaviGateway, NaviHelperEventListener {
    @Volatile private var callback: ((NaviSignal) -> Unit)? = null
    private var listenerAdded = false
    private var lastSpeedLimit: Int? = null

    override fun initialize(onEvent: (NaviSignal) -> Unit) {
        check(callback == null) { "NaviHelper already initialized" }
        helper.initialize()
        callback = onEvent
        helper.addListener(this)
        listenerAdded = true
        lastSpeedLimit = null
    }

    override fun execute(instruction: NaviInstruction) {
        check(callback != null) { "NaviHelper not initialized" }
        when (instruction) {
            is NaviInstruction.Route -> {
                if (instruction.waypoints.isEmpty()) {
                    helper.requestRoute(instruction.destination.toRouteInfo())
                } else {
                    require(instruction.waypoints.size == 1) { "Only first waypoint is supported" }
                    helper.requestRouteWithWaypoints(
                        RouteWithWaypointsRequest(
                            destination = instruction.destination.toRouteInfo(),
                            waypoints = instruction.waypoints.map { it.toFirstWaypointInfo() }
                        )
                    )
                }
            }
            NaviInstruction.FreeRoute -> helper.changeRouteOption(RouteOption.FREE)
            NaviInstruction.Cancel -> helper.cancelRoute()
            is NaviInstruction.AddFirstWaypoint -> helper.addWaypoint(instruction.waypoint.toFirstWaypointInfo())
            NaviInstruction.RemoveFirstWaypoint -> helper.removeWaypoint(WaypointIndex.FIRST)
            NaviInstruction.ReRoute -> helper.requestReRoute()
            NaviInstruction.CurrentLocation -> helper.getCurrentLocationInfo()
            NaviInstruction.RouteState -> helper.getRouteStateInfo()
            NaviInstruction.Bookmarks -> helper.getBookmarkInfo()
            NaviInstruction.RecentDestinations -> helper.getRecentDestinationInfo()
            NaviInstruction.Destination -> helper.getDestinationInfo()
            NaviInstruction.Waypoints -> helper.getWaypointInfo()
            NaviInstruction.Tbt -> helper.getTBTInfo()
            NaviInstruction.ChargerOperators -> helper.getChargerOperatorInfo()
            NaviInstruction.RouteOptionScreen -> helper.requestNavigationScreen(NavigationScreenRequest.RouteOptionList)
            is NaviInstruction.Trip -> helper.requestTrip(instruction.destination.toTripRequest())
        }
    }

    override fun release() {
        callback = null
        try {
            if (listenerAdded) helper.removeListener(this)
        } finally {
            listenerAdded = false
            lastSpeedLimit = null
            helper.release()
        }
    }

    private fun NaviPlaceData.toRouteInfo() = RouteInfo(
        latitude = latitude,
        longitude = longitude,
        poiId = poiId,
        poiName = poiName,
        poiSubId = poiSubId,
        address = address
    )

    private fun NaviPlaceData.toFirstWaypointInfo() = RequestWaypointInfo(
        latitude = latitude,
        longitude = longitude,
        waypointIndex = WaypointIndex.FIRST,
        poiId = poiId,
        poiName = poiName,
        poiSubId = poiSubId,
        address = address
    )

    private fun NaviPlaceData.toTripRequest() = TripRequest(
        latitude = latitude,
        longitude = longitude,
        poiId = poiId,
        poiName = poiName,
        poiSubId = poiSubId
    )

    private fun send(type: NaviSignalType, detail: String? = null, speedLimit: Int? = null) {
        callback?.invoke(NaviSignal(type, detail, speedLimit))
    }

    override fun onRouteStarted(routeStartInfo: RouteStartInfo) =
        send(NaviSignalType.ROUTE_STARTED, routeStartInfo.toString())

    @Deprecated("Deprecated by NaviHelperEventListener")
    override fun onRouteEnded() = send(NaviSignalType.ROUTE_ENDED)

    override fun onRoutingCompleted() = send(NaviSignalType.ROUTING_COMPLETED)
    override fun onReRouteEnded() = send(NaviSignalType.REROUTE_ENDED)
    override fun onRouteOptionChanged(routeOption: RouteOption) =
        send(NaviSignalType.ROUTE_OPTION_CHANGED, routeOption.toString())

    override fun onRouteCancelled() = send(NaviSignalType.ROUTE_CANCELLED)
    override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) =
        send(NaviSignalType.WAYPOINT_CHANGED, waypointChangedInfo.toString())

    override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) =
        send(NaviSignalType.DESTINATION_ARRIVED, destinationArrivedInfo.toString())

    override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo) =
        send(NaviSignalType.WAYPOINT_ARRIVED, waypointArrivedInfo.toString())

    override fun onError(errorCode: NaviErrorCode) = send(NaviSignalType.ERROR, errorCode.toString())
    override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) =
        send(NaviSignalType.CURRENT_LOCATION, currentLocationInfo.toString())

    override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) =
        send(NaviSignalType.ROUTE_STATE, routeStateInfo.toString())

    override fun onDestinationInfo(destinationInfo: DestinationInfo) =
        send(NaviSignalType.DESTINATION_INFO, destinationInfo.toString())

    override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) =
        send(NaviSignalType.WAYPOINT_INFO, waypointInfoList.toString())

    override fun onRecentDestinationInfo(recentDestinationInfoList: List<RecentDestinationInfo>) =
        send(NaviSignalType.RECENT_DESTINATIONS, recentDestinationInfoList.toString())

    override fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>) =
        send(NaviSignalType.BOOKMARKS, bookmarkInfoList.toString())

    override fun onCurrentRoadSpeedLimit(speedLimit: Int) {
        if (lastSpeedLimit == speedLimit) return
        lastSpeedLimit = speedLimit
        send(NaviSignalType.SPEED_LIMIT, speedLimit = speedLimit)
    }

    override fun onCameraAlert(cameraAlertList: List<CameraAlert>) =
        send(NaviSignalType.CAMERA_ALERT, cameraAlertList.toString())

    override fun onDrivingInfo(drivingInfo: DrivingInfo) =
        send(NaviSignalType.DRIVING_INFO, drivingInfo.toString())

    override fun onBookMarkChanged() = send(NaviSignalType.BOOKMARK_CHANGED)
    override fun onDestinationChanged() = send(NaviSignalType.DESTINATION_CHANGED)
    override fun onNavigationStatus(status: NavigationStatus) =
        send(NaviSignalType.NAVIGATION_STATUS, status.toString())

    override fun onTBTInfo(tbtInfoList: List<TBTInfo>) =
        send(NaviSignalType.TBT_INFO, tbtInfoList.toString())

    override fun onChargerOperatorInfo(chargerOperatorList: List<ChargerOperator>) =
        send(NaviSignalType.CHARGER_OPERATORS, chargerOperatorList.toString())

    override fun onCautionPointInfo(cautionPointList: List<CautionPoint>) =
        send(NaviSignalType.CAUTION_POINTS, cautionPointList.toString())

    override fun onTripInfo(tripInfo: TripInfo) = send(NaviSignalType.TRIP_INFO, tripInfo.toString())
}
