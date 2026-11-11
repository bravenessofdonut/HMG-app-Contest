package com.braveberry.voice.data.impl

import android.util.Log
import com.braveberry.domain.model.voice.VoiceEvent
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal class RemoteVoiceEventInbox(capacity: Int = 32) {
    private val queue = Channel<VoiceEvent>(capacity)

    /** 단일 소비자 전제. collector가 여러 개면 이벤트가 분배된다. */
    val events: Flow<VoiceEvent> = queue.receiveAsFlow()

    fun offer(event: VoiceEvent) {
        val sent = queue.trySend(event)
        if (sent.isSuccess || sent.isClosed) return
        Log.e(TAG, "Remote voice queue overflow: dropped ${event::class.simpleName}")
        queue.close(IOException("Remote voice callback queue overflow"))
    }

    fun close() {
        queue.close()
    }

    private companion object {
        const val TAG = "RemoteTtsEventInbox"
    }
}
