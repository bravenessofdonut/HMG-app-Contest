package com.braveberry.vehicle.local.source.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** Pleos Vehicle SDK 2.3.2: 앞좌석 운전석 도어의 현재값 + 갱신 리스너. */
class PleosDoorSource @Inject constructor(@ApplicationContext private val context: Context) {
    fun observe(): Flow<Int?> = callbackFlow {
        val vehicle = Vehicle(context)
        withDoorResources({ vehicle.release() }) { registered ->
            try {
                vehicle.initialize()
                val door = vehicle.getDoor()
                val listener = object : DoorPositionListener {
                    override fun onPositionUpdated(pos: Int?) { trySend(pos) }
                    override fun onFailed(e: Exception) { close(callbackFailure(e)) }
                }
                val area = DoorArea(DoorValue.ROW_1_LEFT)
                door.registerDoorPosition(area = area, listener = listener)
                registered { door.unregisterDoorPosition(area = area, listener = listener) }
                door.getCurrentDoorPosition(
                    area = area,
                    onComplete = { pos: Int? -> trySend(pos); Unit },
                    onFailed = { e: Exception -> close(callbackFailure(e)); Unit }
                )
                awaitClose()
            } catch (cause: Exception) {
                throw sdkFailure(cause)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun callbackFailure(cause: Exception): Exception = when (cause) {
        is CancellationException, is SecurityException -> cause
        else -> IOException("Door callback failed", cause)
    }

    private fun sdkFailure(cause: Exception): Exception = when (cause) {
        is CancellationException, is SecurityException, is IOException -> cause
        is IllegalStateException -> IOException("Door service unavailable", cause)
        else -> cause
    }
}

internal suspend fun withDoorResources(
    release: () -> Unit,
    observe: suspend (onRegistered: (() -> Unit) -> Unit) -> Unit
) {
    var unregister: (() -> Unit)? = null
    try {
        observe { cleanup -> unregister = cleanup }
    } finally {
        try {
            unregister?.invoke()
        } finally {
            release()
        }
    }
}
