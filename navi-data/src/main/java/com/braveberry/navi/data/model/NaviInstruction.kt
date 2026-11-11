package com.braveberry.navi.data.model

data class NaviPlaceData(
    val latitude: Double,
    val longitude: Double,
    val poiId: String,
    val poiName: String,
    val poiSubId: String,
    val address: String
)

sealed interface NaviInstruction {
    data class Route(val destination: NaviPlaceData, val waypoints: List<NaviPlaceData>) : NaviInstruction
    data object FreeRoute : NaviInstruction
    data object Cancel : NaviInstruction
    data class AddFirstWaypoint(val waypoint: NaviPlaceData) : NaviInstruction
    data object RemoveFirstWaypoint : NaviInstruction
    data object ReRoute : NaviInstruction
    data object CurrentLocation : NaviInstruction
    data object RouteState : NaviInstruction
    data object Bookmarks : NaviInstruction
    data object RecentDestinations : NaviInstruction
    data object Destination : NaviInstruction
    data object Waypoints : NaviInstruction
    data object Tbt : NaviInstruction
    data object ChargerOperators : NaviInstruction
    data object RouteOptionScreen : NaviInstruction
    data class Trip(val destination: NaviPlaceData) : NaviInstruction
}
