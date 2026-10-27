package com.braveberry.vehicle.local.source.aaos

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
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
        try {
            check(context.checkSelfPermission(SPEED_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                "차량 속도 접근 권한이 없음: CAR_SPEED"
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
                ?: error("AAOS Car Service 연결에 실패함")
            val service = carType.getField("PROPERTY_SERVICE").get(null) as String
            manager = carType.getMethod("getCarManager", String::class.java)
                .invoke(car, service) ?: error("CarPropertyManager가 없음")

            callback = Proxy.newProxyInstance(listenerType.classLoader, arrayOf(listenerType)) { proxy, method, args ->
                when (method.name) {
                    "hashCode" -> return@newProxyInstance System.identityHashCode(proxy)
                    "equals" -> return@newProxyInstance (proxy === args?.firstOrNull())
                    "toString" -> return@newProxyInstance "SpeedPropertyListener"
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
                    }.onFailure { close(it) }
                }
                null
            }
            val registered = mgrType.getMethod(
                "registerCallback", listenerType, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType
            ).invoke(manager, callback, id, 5f) as Boolean
            check(registered) { "PERF_VEHICLE_SPEED를 구독할 수 없음" }
            // 구독 후 첫 읽기. 초기값이 일시적으로 없더라도 업데이트 구독은 유지함.
            try {
                val value = mgrType.getMethod(
                    "getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                ).invoke(manager, id, 0) as Float
                trySend(value)
            } catch (_: ReflectiveOperationException) {
                // 즉시 읽기 실패는 콜백으로 오는 첫 값을 기다림.
            } catch (_: IllegalStateException) {
                // 일시적으로 조회할 수 없더라도 구독 유지.
            }
            awaitClose {
                runCatching {
                    mgrType.getMethod("unregisterCallback", listenerType).invoke(manager, callback)
                }
                runCatching { carType.getMethod("disconnect").invoke(car) }
            }
        } catch (t: Throwable) {
            val cause = (t as? InvocationTargetException)?.targetException ?: t
            close(cause)
            val managerToClose = manager
            val callbackToClose = callback
            val typeToClose = callbackType
            if (managerToClose != null && callbackToClose != null && typeToClose != null) runCatching {
                managerToClose.javaClass.getMethod("unregisterCallback", typeToClose)
                    .invoke(managerToClose, callbackToClose)
            }
            val carToClose = car
            if (carToClose != null) runCatching {
                carToClose.javaClass.getMethod("disconnect").invoke(carToClose)
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
    }
}
