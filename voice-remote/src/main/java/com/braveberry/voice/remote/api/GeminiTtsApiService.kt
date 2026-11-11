package com.braveberry.voice.remote.api

import com.braveberry.voice.remote.model.TtsSpeechRequestDto
import kotlinx.coroutines.flow.Flow

interface GeminiTtsApiService {

    /** 한 문장을 합성해 PCM 16-bit mono 청크로 내보냄. */
    fun generateSpeech(
        request: TtsSpeechRequestDto
    ): Flow<ByteArray>
}
