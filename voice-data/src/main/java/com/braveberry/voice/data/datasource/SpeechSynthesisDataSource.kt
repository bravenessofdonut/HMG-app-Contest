package com.braveberry.voice.data.datasource

import kotlinx.coroutines.flow.Flow

/**
 * 텍스트를 PCM 오디오로 바꾸는 음성 합성 SDK와 data 사이의 계약.
 * SDK를 교체해도 재생 순서·타임아웃·재시도 규칙(data)은 바뀌지 않도록 SDK 타입을 노출하지 않음.
 */
interface SpeechSynthesisDataSource {
    val voices: List<String>
    val defaultVoice: String
    /** BCP-47 언어 코드 (예: "en-US"). */
    val languages: List<String>
    /** synthesize가 내보내는 PCM 16-bit mono의 샘플레이트. */
    val sampleRateHz: Int

    /**
     * 한 문장을 합성해 PCM 청크로 내보냄.
     * 실패 규칙: 같은 요청을 다시 보내 회복될 수 있으면 IOException, 그 밖의 실패는 다른 예외로 던짐.
     */
    fun synthesize(text: String, voice: String, language: String): Flow<ByteArray>
}
