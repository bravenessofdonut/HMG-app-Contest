package com.braveberry.voice.data.datasource

import com.braveberry.voice.data.model.TtsOptionsDataModel
import com.braveberry.voice.data.model.TtsPlaybackDataModel
import kotlinx.coroutines.flow.Flow

/**
 * 실패 규칙: 일시적 장애는 IOException, 인증 실패는 SecurityException,
 * 지원하지 않는 언어·형식은 UnsupportedOperationException으로 던짐.
 */
interface TtsRemoteDataSource {

    val options: TtsOptionsDataModel

    fun speak(
        text: String,
        voice: String,
        language: String
    ): Flow<TtsPlaybackDataModel>

    fun speakStream(
        tokens: Flow<String>,
        voice: String,
        language: String
    ): Flow<TtsPlaybackDataModel>
}
