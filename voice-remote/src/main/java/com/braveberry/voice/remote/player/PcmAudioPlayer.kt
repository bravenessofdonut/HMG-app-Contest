package com.braveberry.voice.remote.player

import kotlinx.coroutines.flow.Flow

interface PcmAudioPlayer {
    /** PCM 16-bit mono를 끝까지 재생하고 반환함. 출력 실패는 IOException으로 던짐. */
    suspend fun play(pcm: Flow<ByteArray>, sampleRateHz: Int)

    /** 진행 중인 재생을 즉시 멈춤. 블로킹 중인 play를 깨우는 용도이며 다른 스레드에서 호출될 수 있음. */
    fun stop()
}
