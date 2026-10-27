package com.braveberry.vehicle.local.source.pleos

import android.content.Context
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

/** Pleos Vehicle SDK 2.3.2: 앞좌석 운전석 도어의 현재값 + 갱신 리스너. */
class PleosDoorSource @Inject constructor(@ApplicationContext private val context: Context) {
    fun observe(): Flow<Int?> = callbackFlow {
        val vehicle = Vehicle(context)
        var registered = false
        try {
            vehicle.initialize()
            val door = vehicle.getDoor()
            val listener = object : DoorPositionListener {
                override fun onPositionUpdated(pos: Int?) { trySend(pos) }
                override fun onFailed(e: Exception) { close(e) }
            }
            val area = DoorArea(DoorValue.ROW_1_LEFT)
            door.registerDoorPosition(area = area, listener = listener)
            registered = true
            door.getCurrentDoorPosition(
                area = area,
                onComplete = { pos: Int? -> trySend(pos); Unit },
                onFailed = { e: Exception -> close(e); Unit }
            )
            awaitClose {
                if (registered) runCatching {
                    door.unregisterDoorPosition(area = area, listener = listener)
                }
                runCatching { vehicle.release() }
            }
        } catch (e: Exception) {
            runCatching { vehicle.release() }
            close(e)
        }
    }.flowOn(Dispatchers.IO)
}
