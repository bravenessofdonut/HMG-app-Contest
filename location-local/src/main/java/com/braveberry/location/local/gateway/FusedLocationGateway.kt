package com.braveberry.location.local.gateway

import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityRequest

/** SDK 구현과 테스트 대역 사이의 경계임. 구독마다 인스턴스 생성함. */
internal fun interface FusedLocationGatewayFactory {
    fun create(): FusedLocationGateway
}

internal interface FusedLocationGateway {
    fun isSupported(): Boolean
    fun hasPermission(): Boolean
    fun initialize()
    fun release()
    fun startLocationUpdates(callback: LocationCallback)
    fun stopLocationUpdates()
    fun requestLastLocation(callback: LocationCallback)
    fun addProximity(request: ProximityRequest, callback: ProximityCallback)
    fun removeProximity(alertId: String)
}

internal interface LocationCallback {
    fun onChanged(reading: LocationReading?)
    fun onError(error: Exception)
}

internal interface ProximityCallback {
    fun onRegistered(alertId: String)
    fun onEntered(alertId: String)
    fun onExited(alertId: String)
    fun onError(error: Exception)
}
