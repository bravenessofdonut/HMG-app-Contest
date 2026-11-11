package com.braveberry.voice.remote.mapper

import com.braveberry.voice.remote.model.UnsupportedAudioFormatException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import java.io.IOException

/**
 * 같은 문장을 즉시 다시 요청해서 회복될 수 있는 실패인지 판단함.
 * 그 밖의 실패(인증·App Check, 할당량, 콘텐츠 차단, 형식 오류 등)는 재시도해도 같은 결과이므로 재시도하지 않음.
 */
internal fun Throwable.isRetryableGeminiFailure(): Boolean = when (this) {
    is RequestTimeoutException,
    is ServerException -> true       // 4xx/5xx를 구분할 수 없음. 1회 재시도 비용이 작아 재시도 대상으로 둠.
    // SDK는 연결 오류를 UnknownException으로 감싸므로 원인 체인을 확인함.
    else -> causes().any { it is IOException }
}

/**
 * 최종 합성 실패를 DataSource 계약의 표준 예외로 변환함. Firebase 타입이 data로 새지 않도록 원인으로만 보존함.
 * 인증 실패는 SecurityException, 재생할 수 없는 형식은 UnsupportedOperationException, 나머지는 IOException.
 */
internal fun Throwable.toTtsDataSourceException(): Exception = when {
    causes().any { it is InvalidAPIKeyException } ->
        SecurityException("Gemini TTS authentication failed", this)
    causes().any { it is UnsupportedAudioFormatException } ->
        UnsupportedOperationException("Unsupported Gemini TTS audio format", this)
    else -> IOException("Gemini TTS synthesis failed", this)
}

private fun Throwable.causes(): Sequence<Throwable> =
    generateSequence(this) { current -> current.cause?.takeIf { it !== current } }.take(10)
