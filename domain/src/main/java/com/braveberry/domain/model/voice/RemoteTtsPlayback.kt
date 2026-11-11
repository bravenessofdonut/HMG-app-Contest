package com.braveberry.domain.model.voice

/** Remote TTS 요청 하나의 재생 진행 상태. */
enum class RemoteTtsPlayback {
    SPEAKING,
    DONE,
    /** 다른 요청이 이 재생을 대체함. */
    STOPPED
}
