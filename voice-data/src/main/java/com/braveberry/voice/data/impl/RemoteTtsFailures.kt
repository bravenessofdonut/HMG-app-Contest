package com.braveberry.voice.data.impl

import java.io.IOException

/** 문장별 응답이 제한시간 안에 오지 않음. 오디오를 내보내기 전이면 재시도 대상임. */
internal class SynthesisTimeoutException(timeoutMs: Long, cause: Throwable) :
    IOException("No TTS response within ${timeoutMs}ms", cause)

/** 한 문장의 응답이 끝났지만 오디오 바이트를 받지 못함. 재시도 대상임. */
internal class EmptyAudioException : Exception("TTS response contained no audio")

/** 재시도 판단이 끝난 최종 합성 실패. 세부 원인은 cause로 로그에 남김. */
internal class SynthesisException(cause: Throwable) : Exception("TTS synthesis failed", cause)

/**
 * 같은 문장을 즉시 다시 요청해서 회복될 수 있는 실패인지 판단함.
 * SDK별 예외 분류는 DataSource 구현이 IOException으로 변환해 전달함.
 */
internal fun Throwable.isRetryableSynthesisFailure(): Boolean =
    this is IOException || this is EmptyAudioException
