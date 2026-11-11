package com.braveberry.location.local.impl

import com.braveberry.location.data.datasource.LocationDataSource
import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityReading
import com.braveberry.location.data.model.ProximityRequest
import com.braveberry.location.local.gateway.FusedLocationGateway
import com.braveberry.location.local.gateway.FusedLocationGatewayFactory
import com.braveberry.location.local.gateway.LocationCallback
import com.braveberry.location.local.gateway.ProximityCallback
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** 구독마다 SDK 인스턴스 하나 사용함. 종료하거나 실패하면 해제함. */
internal class LocationDataSourceImpl @Inject constructor(
    private val factory: FusedLocationGatewayFactory,
) : LocationDataSource {
    override fun isSupported(): Boolean = factory.create().isSupported()

    override fun observeLocation(): Flow<LocationReading?> = locations(continuous = true)
    override fun lastKnownLocation(): Flow<LocationReading?> = locations(continuous = false)

    private fun locations(continuous: Boolean): Flow<LocationReading?> = callbackFlow {
        val gateway = factory.create()
        val initialized = AtomicBoolean(false)
        val registered = AtomicBoolean(false)
        val cleaned = AtomicBoolean(false)
        fun cleanup() {
            if (!cleaned.compareAndSet(false, true)) return
            try {
                if (registered.get()) runCatching { gateway.stopLocationUpdates() }
            } finally {
                if (initialized.get()) runCatching { gateway.release() }
            }
        }
        val callback = object : LocationCallback {
            override fun onChanged(reading: LocationReading?) {
                trySend(reading)
                if (!continuous) close()
            }
            override fun onError(error: Exception) {
                close(error.asLocationFailure())
            }
        }
        try {
            verifyReady(gateway)
            initialized.set(true)
            gateway.initialize()
            if (continuous) {
                registered.set(true) // 등록 중 실패해도 해제를 시도하도록 표시함.
                gateway.startLocationUpdates(callback)
            } else {
                gateway.requestLastLocation(callback)
            }
            awaitClose { cleanup() }
        } catch (cause: Throwable) {
            cleanup()
            throw cause.asLocationFailure()
        }
    }

    override fun observeProximity(request: ProximityRequest): Flow<ProximityReading> = callbackFlow {
        val gateway = factory.create()
        val initialized = AtomicBoolean(false)
        val cleaned = AtomicBoolean(false)
        val registeredId = AtomicReference<String?>(null)
        fun cleanup() {
            if (!cleaned.compareAndSet(false, true)) return
            try {
                registeredId.get()?.let { id -> runCatching { gateway.removeProximity(id) } }
            } finally {
                if (initialized.get()) runCatching { gateway.release() }
            }
        }
        val callback = object : ProximityCallback {
            override fun onRegistered(alertId: String) {
                if (alertId.isBlank()) {
                    close(IOException("Pleos returned an empty proximity alert ID"))
                    return
                }
                registeredId.set(alertId)
                if (cleaned.get()) {
                    // 등록 콜백 전에 취소됐다면 뒤늦게 등록된 알림도 제거함.
                    runCatching { gateway.removeProximity(alertId) }
                } else {
                    trySend(ProximityReading.Registered(alertId))
                }
            }
            override fun onEntered(alertId: String) {
                if (!cleaned.get() && alertId == registeredId.get())
                    trySend(ProximityReading.Entered(alertId))
            }
            override fun onExited(alertId: String) {
                if (!cleaned.get() && alertId == registeredId.get())
                    trySend(ProximityReading.Exited(alertId))
            }
            override fun onError(error: Exception) { close(error.asLocationFailure()) }
        }
        try {
            verifyReady(gateway)
            initialized.set(true)
            gateway.initialize()
            gateway.addProximity(request, callback)
            awaitClose { cleanup() }
        } catch (cause: Throwable) {
            cleanup()
            throw cause.asLocationFailure()
        }
    }
}

private fun verifyReady(gateway: FusedLocationGateway) {
    if (!gateway.isSupported()) throw UnsupportedOperationException("Pleos Fused Location unavailable")
    if (!gateway.hasPermission()) throw SecurityException("FUSED_LOCATION permission required")
}

private fun Throwable.asLocationFailure(): Throwable = when (this) {
    is CancellationException, is SecurityException, is UnsupportedOperationException -> this
    is IOException -> this
    is Exception -> IOException("Pleos Fused Location failed", this)
    else -> this
}
