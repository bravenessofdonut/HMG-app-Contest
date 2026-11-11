package com.braveberry.domain.model.voice

/** Remote TTS에서 선택 가능한 화자와 언어(BCP-47). 값의 원본은 구현 모듈의 카탈로그임. */
data class RemoteTtsOptions(
    val speakers: List<String>,
    val defaultSpeaker: String,
    val languages: List<String>
)
