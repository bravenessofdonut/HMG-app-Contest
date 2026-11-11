package com.braveberry.location.local.gateway

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import android.content.Context
import android.content.pm.PackageManager
import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class PleosFusedLocationGatewayFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : FusedLocationGatewayFactory {
    override fun create(): FusedLocationGateway = PleosFusedLocationGateway(context)
}

/** Pleos FusedLocation 1.1.3 API 사용함. 리플렉션 사용하지 않음. */
private class PleosFusedLocationGateway(context: Context) : FusedLocationGateway {
    private val applicationContext = context.applicationContext
    private val sdk = FusedLocation(applicationContext)
    private var locationListener: FusedLocationListener? = null

    override fun isSupported() = sdk.isSupported()

    override fun hasPermission() = applicationContext.checkSelfPermission(
        "pleos.car.permission.FUSED_LOCATION"
    ) == PackageManager.PERMISSION_GRANTED

    override fun initialize() = sdk.initialize()
    override fun release() = sdk.release()

    override fun startLocationUpdates(callback: LocationCallback) {
        val listener = callback.toSdkListener()
        locationListener = listener
        sdk.registerFusedLocationCallback(listener)
    }

    override fun stopLocationUpdates() {
        val listener = locationListener ?: return
        locationListener = null
        sdk.unregisterFusedLocationCallback(listener)
    }

    override fun requestLastLocation(callback: LocationCallback) =
        sdk.getLastKnownLocation(callback.toSdkListener())

    override fun addProximity(request: ProximityRequest, callback: ProximityCallback) {
        sdk.addProximityAlert(
            ProximityAlertRequest(
                latitude = request.latitude,
                longitude = request.longitude,
                radiusMeters = request.radiusMeters,
                transitionTypes = setOf(ProximityTransition.ENTER, ProximityTransition.EXIT),
            ),
            object : ProximityAlertListener {
                override fun onRegistered(alertId: String) = callback.onRegistered(alertId)
                override fun onTransition(event: ProximityAlertEvent) {
                    when (event.transition) {
                        ProximityTransition.ENTER -> callback.onEntered(event.alertId)
                        ProximityTransition.EXIT -> callback.onExited(event.alertId)
                        else -> Unit
                    }
                }
                override fun onError(exception: Exception) = callback.onError(exception)
            }
        )
    }

    override fun removeProximity(alertId: String) = sdk.removeProximityAlert(alertId)

    private fun LocationCallback.toSdkListener() = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            this@toSdkListener.onChanged(location?.let {
                LocationReading(it.latitude, it.longitude, it.altitude, it.heading,
                    it.confidence, it.timestampMillis)
            })
        }
        override fun onError(exception: Exception) = this@toSdkListener.onError(exception)
    }
}
