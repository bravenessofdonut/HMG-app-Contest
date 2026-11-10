package com.braveberry.location.local.gateway

import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityRequest

/** SDK 타입과 테스트 코드 사이의 경계. 인스턴스는 구독마다 생성함. */
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
