package com.braveberry.navi.data.mapper

import com.braveberry.domain.model.navi.NaviCommand
import com.braveberry.domain.model.navi.NaviEvent
import com.braveberry.domain.model.navi.NaviEventType
import com.braveberry.domain.model.navi.NaviPlace
import com.braveberry.navi.data.model.NaviInstruction
import com.braveberry.navi.data.model.NaviPlaceData
import com.braveberry.navi.data.model.NaviSignal

internal fun NaviPlace.toData() = NaviPlaceData(latitude, longitude, poiId, poiName, poiSubId, address, poiProvider)

internal fun NaviCommand.toInstruction(): NaviInstruction = when (this) {
    is NaviCommand.RequestRoute -> NaviInstruction.Route(destination.toData(), waypoints.map { it.toData() })
    is NaviCommand.ChangeRouteOption -> when (option) {
        com.braveberry.domain.model.navi.NaviRouteOption.FREE -> NaviInstruction.FreeRoute
    }
    NaviCommand.CancelRoute -> NaviInstruction.Cancel
    is NaviCommand.AddFirstWaypoint -> NaviInstruction.AddFirstWaypoint(waypoint.toData())
    NaviCommand.RemoveFirstWaypoint -> NaviInstruction.RemoveFirstWaypoint
    NaviCommand.ReRoute -> NaviInstruction.ReRoute
    NaviCommand.GetCurrentLocation -> NaviInstruction.CurrentLocation
    NaviCommand.GetRouteState -> NaviInstruction.RouteState
    NaviCommand.GetBookmarks -> NaviInstruction.Bookmarks
    NaviCommand.GetRecentDestinations -> NaviInstruction.RecentDestinations
    NaviCommand.GetDestination -> NaviInstruction.Destination
    NaviCommand.GetWaypoints -> NaviInstruction.Waypoints
    NaviCommand.GetTbt -> NaviInstruction.Tbt
    NaviCommand.GetChargerOperators -> NaviInstruction.ChargerOperators
    NaviCommand.OpenRouteOptionScreen -> NaviInstruction.RouteOptionScreen
    is NaviCommand.RequestTrip -> NaviInstruction.Trip(destination.toData())
}

internal fun NaviSignal.toDomain(): NaviEvent = NaviEvent(
    type = NaviEventType.valueOf(type.name),
    detail = detail,
    speedLimitKph = speedLimitKph
)
