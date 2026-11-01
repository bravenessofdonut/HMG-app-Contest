package com.braveberry.domain.usecase.voice

/** 세션을 사용하는 쪽의 lease 계약. 화면 개수 및 SDK 해제 정책은 구현 모듈이 소유함. */
interface VoiceSessionOwner {
    fun acquire(): VoiceSession
    fun release()
}
