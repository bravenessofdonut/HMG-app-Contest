package com.braveberry.vehicle.local.source.aaos

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AaosSpeedSource @Inject constructor(@ApplicationContext private val context: Context) {
    fun observe(): Flow<Float> = flow {
        if (context.checkSelfPermission("android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("CAR_SPEED")
        }
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            throw UnsupportedOperationException("AAOS 차량 속도 미지원")
        }
        emitAll(observeCarSpeed(context))
    }.flowOn(Dispatchers.IO)
}

@Suppress("DEPRECATION") // 기존 AAOS 구독 API와 5 Hz 요청을 유지함.
private fun observeCarSpeed(context: Context): Flow<Float> = callbackFlow {
    val id = VehiclePropertyIds.PERF_VEHICLE_SPEED
    val car = Car.createCar(context) ?: throw IOException("Car Service 연결 실패")
    try {
        val manager = car.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            ?: throw IOException("CarPropertyManager 연결 실패")
        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                if (value.propertyId == id && value.status == CarPropertyValue.STATUS_AVAILABLE) {
                    (value.value as? Float)?.let { trySend(it) }
                }
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                if (propertyId == id) close(IOException("차량 속도 수신 실패"))
            }
        }
        if (!manager.registerCallback(callback, id, 5f)) throw IOException("차량 속도 구독 실패")
        try {
            try {
                trySend(manager.getFloatProperty(id, 0))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: IllegalStateException) {
                // 최초 조회값이 준비되지 않은 경우 다음 콜백을 기다림.
            } catch (_: android.car.hardware.property.PropertyNotAvailableException) {
                // 차량에서 값을 제공할 수 있게 될 때까지 구독 유지함.
            }
            awaitClose()
        } finally {
            manager.unregisterCallback(callback)
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (unsupported: IllegalArgumentException) {
        throw UnsupportedOperationException("차량 속성 미지원", unsupported)
    } catch (unavailable: IllegalStateException) {
        throw IOException("Car Service 연결 끊김", unavailable)
    } finally {
        car.disconnect()
    }
}
