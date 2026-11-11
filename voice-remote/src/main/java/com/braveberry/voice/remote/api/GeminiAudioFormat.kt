package com.braveberry.voice.remote.api

/**
 * Gemini TTS 응답 part의 형식 확인.
 * 출력은 Raw PCM 16-bit LE, mono, 24kHz로 고정돼 있음 (Firebase AI Logic 문서). 샘플레이트만 재생기와 맞는지 확인함.
 */
internal object GeminiAudioFormat {
    const val SAMPLE_RATE = 24_000
    private val rateParam = Regex("""rate\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)

    /**
     * @return 오디오 part면 true, 텍스트 등 오디오가 아닌 part면 false.
     * @throws UnsupportedAudioFormatException 샘플레이트가 달라 잡음·속도 이상이 생길 때. rate가 없으면 24kHz로 간주함.
     */
    fun isPlayableAudio(mimeType: String): Boolean {
        if (!mimeType.trim().startsWith("audio/", ignoreCase = true)) return false
        val rate = rateParam.find(mimeType)?.groupValues?.get(1)?.toIntOrNull()
        if (rate != null && rate != SAMPLE_RATE) throw UnsupportedAudioFormatException(mimeType)
        return true
    }
}

/** 재생기가 처리할 수 없는 오디오 형식. 같은 요청에는 같은 형식이 오므로 재시도하지 않음. */
internal class UnsupportedAudioFormatException(mimeType: String) :
    Exception("Unsupported Gemini audio format: $mimeType")

/** 한 문장의 응답이 끝났지만 오디오 바이트를 받지 못함. */
internal class EmptyAudioException : Exception("Gemini TTS response contained no audio")
