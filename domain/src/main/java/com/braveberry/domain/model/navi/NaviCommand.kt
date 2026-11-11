package com.braveberry.domain.model.navi

data class NaviPlace(
    val latitude: Double,
    val longitude: Double,
    val poiId: String,
    val poiName: String,
    val poiSubId: String = "0",
    val address: String = ""
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid latitude" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid longitude" }
    }
}

enum class NaviRouteOption { FREE }

sealed interface NaviCommand {
    data class RequestRoute(
        val destination: NaviPlace,
        val waypoints: List<NaviPlace> = emptyList()
    ) : NaviCommand {
        init {
            // 샘플에서 확인된 첫 번째 경유지만 지원함.
            require(waypoints.size <= 1) { "Only the first waypoint is supported" }
        }
    }

    data class ChangeRouteOption(val option: NaviRouteOption) : NaviCommand
    data object CancelRoute : NaviCommand
    data class AddFirstWaypoint(val waypoint: NaviPlace) : NaviCommand
    data object RemoveFirstWaypoint : NaviCommand
    data object ReRoute : NaviCommand
    data object GetCurrentLocation : NaviCommand
    data object GetRouteState : NaviCommand
    data object GetBookmarks : NaviCommand
    data object GetRecentDestinations : NaviCommand
    data object GetDestination : NaviCommand
    data object GetWaypoints : NaviCommand
    data object GetTbt : NaviCommand
    data object GetChargerOperators : NaviCommand
    data object OpenRouteOptionScreen : NaviCommand
    data class RequestTrip(val destination: NaviPlace) : NaviCommand
}
