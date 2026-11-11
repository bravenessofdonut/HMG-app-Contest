package com.braveberry.voice.remote.player

import kotlinx.coroutines.flow.Flow

internal interface PcmAudioPlayer {
    suspend fun playStream(pcmFlow: Flow<ByteArray>)
    fun stop()
    fun release()
}
