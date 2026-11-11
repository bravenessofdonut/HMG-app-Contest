package com.braveberry.voice.remote.api

import kotlinx.coroutines.flow.Flow

internal interface GeminiTtsClient {
    fun generateSpeechStream(
        text: String,
        voiceName: String,
        languageCode: String
    ): Flow<ByteArray>
}
