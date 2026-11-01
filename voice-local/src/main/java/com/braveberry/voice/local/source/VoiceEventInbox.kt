package com.braveberry.voice.local.source

import com.braveberry.domain.model.voice.VoiceEvent
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow

internal class VoiceEventInbox(capacity: Int = 32) {
    private val queue = Channel<VoiceEvent>(capacity)
    private val meter = Channel<VoiceEvent.SttRms>(Channel.CONFLATED)
    val events: Flow<VoiceEvent> = merge(queue.receiveAsFlow(), meter.receiveAsFlow()).buffer(0)

    fun offer(event: VoiceEvent) {
        if (event is VoiceEvent.SttRms) {
            meter.trySend(event)
            return
        }
        val sent = queue.trySend(event)
        if (sent.isSuccess || sent.isClosed) return
        // 진행 신호 유실을 정상 재생으로 숨기지 않음. 세션이 재연결 가능한 실패로 전환함.
        queue.close(IOException("Voice callback queue overflow"))
    }

    fun close() {
        queue.close()
        meter.close()
    }
}
