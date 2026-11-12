package com.braveberry.domain.repository

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.RemoteTtsPlayback
import kotlinx.coroutines.flow.Flow

/**
 * Remote TTS를 제공하는 도메인 리포지토리 인터페이스. 합성 SDK는 data 계층 뒤에서 교체됨.
 * language는 BCP-47 태그 (예: "en-US", "ja-JP", "cmn-CN").
 * 재생은 반환된 Flow를 수집하는 동안 진행되고, 수집을 취소하면 멈춤.
 */
interface RemoteTtsRepository {
    val options: RemoteTtsOptions

    fun speak(
        text: String,
        voiceName: String,
        language: String
    ): Flow<DataResource<RemoteTtsPlayback>>

    fun speakStream(
        tokens: Flow<String>,
        voiceName: String,
        language: String
    ): Flow<DataResource<RemoteTtsPlayback>>
}
