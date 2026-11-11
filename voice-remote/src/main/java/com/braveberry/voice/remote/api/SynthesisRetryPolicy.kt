package com.braveberry.voice.remote.api

import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import java.io.IOException

/**
 * 같은 문장을 즉시 다시 요청해서 회복될 수 있는 실패인지 판단함.
 * 그 밖의 실패(인증·App Check, 할당량, 콘텐츠 차단, 형식 오류 등)는 재시도해도 같은 결과이므로 재시도하지 않음.
 */
internal object SynthesisRetryPolicy {
    fun isRetryable(error: Throwable): Boolean = when (error) {
        is RequestTimeoutException,
        is ServerException,          // 4xx/5xx를 구분할 수 없음. 1회 재시도 비용이 작아 재시도 대상으로 둠.
        is EmptyAudioException -> true
        // SynthesisTimeoutException(IOException) 포함. SDK는 연결 오류를 UnknownException으로 감싸므로 원인 체인을 확인함.
        else -> error.causes().any { it is IOException }
    }

    private fun Throwable.causes(): Sequence<Throwable> =
        generateSequence(this) { current -> current.cause?.takeIf { it !== current } }.take(10)
}

/** Gemini 응답이 제한시간 안에 오지 않음. 오디오를 내보내기 전이면 재시도 대상임. */
internal class SynthesisTimeoutException(timeoutMs: Long, cause: Throwable) :
    IOException("No Gemini TTS response within ${timeoutMs}ms", cause)

/** 재시도 판단이 끝난 최종 합성 실패. 세부 원인은 cause로 로그에 남김. */
internal class SynthesisException(cause: Throwable) : Exception("Gemini TTS synthesis failed", cause)
