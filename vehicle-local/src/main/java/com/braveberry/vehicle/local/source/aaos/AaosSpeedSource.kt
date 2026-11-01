package com.braveberry.vehicle.local.source.aaos

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * AAOS 공개 CarPropertyManager API를 런타임에서 연결함.
 * 기본 Android compileSdk에 android.car.jar가 없는 문제를 소스 의존성 없이 해결함.
 * reflection은 차량 플랫폼 클래스에서만 사용하고, 나머지 레이어에는 전파하지 않음.
 */
class AaosSpeedSource @Inject constructor(@ApplicationContext private val context: Context) {
    fun observe(): Flow<Float> = callbackFlow {
        var car: Any? = null
        var manager: Any? = null
        var callback: Any? = null
        var callbackType: Class<*>? = null
        var registered = false
        try {
            if (context.checkSelfPermission(SPEED_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("CAR_SPEED")
            }
            val carType = Class.forName("android.car.Car")
            val propertyType = Class.forName("android.car.VehiclePropertyIds")
            val id = propertyType.getField("PERF_VEHICLE_SPEED").getInt(null)
            val mgrType = Class.forName("android.car.hardware.property.CarPropertyManager")
            val listenerType = Class.forName(
                "android.car.hardware.property.CarPropertyManager$" + "CarPropertyEventCallback"
            )
            callbackType = listenerType

            car = carType.getMethod("createCar", Context::class.java).invoke(null, context)
                ?: throw IOException("AAOS Car Service 연결에 실패함")
            val service = carType.getField("PROPERTY_SERVICE").get(null) as String
            manager = carType.getMethod("getCarManager", String::class.java)
                .invoke(car, service) ?: throw IOException("CarPropertyManager가 없음")

            callback = Proxy.newProxyInstance(listenerType.classLoader, arrayOf(listenerType)) { proxy, method, args ->
                when (method.name) {
                    "hashCode" -> return@newProxyInstance System.identityHashCode(proxy)
                    "equals" -> return@newProxyInstance (proxy === args?.firstOrNull())
                    "toString" -> return@newProxyInstance "SpeedPropertyListener"
                }
                if (method.name == "onErrorEvent" && args?.firstOrNull() == id) {
                    close(IOException("Vehicle speed callback failed"))
                    return@newProxyInstance null
                }
                if (method.name == "onChangeEvent" && !args.isNullOrEmpty()) {
                    runCatching {
                        val value = args[0] ?: return@runCatching
                        val eventId = value.javaClass.getMethod("getPropertyId").invoke(value) as Int
                        val status = value.javaClass.getMethod("getStatus").invoke(value) as Int
                        // CarPropertyValue.STATUS_AVAILABLE == 0
                        if (eventId == id && status == 0) {
                            val speed = value.javaClass.getMethod("getValue").invoke(value) as? Number
                            if (speed != null) trySend(speed.toFloat())
                        }
                    }.onFailure { failure ->
                        close((failure as? InvocationTargetException)?.targetException ?: failure)
                    }
                }
                null
            }
            registered = mgrType.getMethod(
                "registerCallback", listenerType, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType
            ).invoke(manager, callback, id, 5f) as Boolean
            if (!registered) throw IOException("PERF_VEHICLE_SPEED 구독 실패")
            // 구독 후 첫 읽기. 초기값이 일시적으로 없더라도 업데이트 구독은 유지함.
            try {
                val value = mgrType.getMethod(
                    "getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                ).invoke(manager, id, 0) as Float
                trySend(value)
            } catch (failure: InvocationTargetException) {
                if (failure.targetException is kotlinx.coroutines.CancellationException) throw failure
                if (failure.targetException !is IllegalStateException) throw failure
            }
            awaitClose()
        } catch (failure: InvocationTargetException) {
            val cause = failure.targetException
            if (cause is kotlinx.coroutines.CancellationException) throw cause
            if (cause is IllegalStateException) throw IOException("Car service unavailable", cause)
            throw cause
        } finally {
            val managerToClose = manager
            val callbackToClose = callback
            val typeToClose = callbackType
            if (registered && managerToClose != null && callbackToClose != null && typeToClose != null) cleanup {
                managerToClose.javaClass.getMethod("unregisterCallback", typeToClose)
                    .invoke(managerToClose, callbackToClose)
            }
            val carToClose = car
            if (carToClose != null) cleanup {
                carToClose.javaClass.getMethod("disconnect").invoke(carToClose)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun cleanup(action: () -> Unit) {
        try {
            action()
        } catch (failure: ReflectiveOperationException) {
            val cause = (failure as? InvocationTargetException)?.targetException ?: failure
            if (cause is Error || cause is kotlinx.coroutines.CancellationException) throw cause
            Log.w("AaosSpeedSource", "Vehicle callback cleanup failed", cause)
        }
    }

    private companion object {
        const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
    }
}
