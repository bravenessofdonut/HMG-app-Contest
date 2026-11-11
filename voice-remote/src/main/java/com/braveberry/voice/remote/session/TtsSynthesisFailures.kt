package com.braveberry.voice.remote.session

import java.io.IOException

/** 문장별 응답이 제한시간 안에 오지 않음. 오디오를 내보내기 전이면 재시도 대상임. */
internal class SynthesisTimeoutException(timeoutMs: Long, cause: Throwable) :
    IOException("No Gemini TTS response within ${timeoutMs}ms", cause)

/** 한 문장의 응답이 끝났지만 오디오 바이트를 받지 못함. 재시도 대상임. */
internal class EmptyAudioException : Exception("Gemini TTS response contained no audio")
