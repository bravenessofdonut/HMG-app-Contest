package com.braveberry.voice.remote.mapper

import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import java.io.IOException

/**
 * Firebase AI 예외를 DataSource 계약의 표준 예외로 변환함.
 * 같은 문장을 즉시 다시 요청해서 회복될 수 있는 실패만 IOException으로 바꾸고,
 * 그 밖의 실패(인증·App Check, 할당량, 콘텐츠 차단, 형식 오류 등)는 그대로 전달함.
 */
internal fun Throwable.toSynthesisError(): Throwable = when {
    this is IOException -> this
    isTransientGeminiFailure() -> IOException("Transient Gemini TTS failure", this)
    else -> this
}

private fun Throwable.isTransientGeminiFailure(): Boolean = when (this) {
    is RequestTimeoutException,
    is ServerException -> true       // 4xx/5xx를 구분할 수 없음. 1회 재시도 비용이 작아 재시도 대상으로 둠.
    // SDK는 연결 오류를 UnknownException으로 감싸므로 원인 체인을 확인함.
    else -> causes().any { it is IOException }
}

private fun Throwable.causes(): Sequence<Throwable> =
    generateSequence(this) { current -> current.cause?.takeIf { it !== current } }.take(10)
