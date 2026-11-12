package com.braveberry.voice.remote.model

/**
 * Gemini TTS (gemini-3.1-flash-tts-preview) 화자 및 지원 언어 카탈로그.
 * Gemini 화자는 특정 언어에 묶이지 않음(Pleos와 다름). 합성 언어는 텍스트와 languageCode로 결정됨.
 */
internal object GeminiSpeakerCatalog {
    val defaultSpeaker: String = "Fenrir"

    val speakers: List<String> = listOf("Fenrir", "Aoede", "Charon", "Puck", "Kore")

    fun isSupported(speaker: String): Boolean = speaker in speakers

    /** 서비스 대상 언어. Gemini-TTS 기준 코드이며 중국어는 zh-CN이 아닌 cmn-CN(Preview)임. */
    val supportedLanguages: List<String> = listOf("en-US", "ja-JP", "cmn-CN")

    fun isSupportedLanguage(language: String): Boolean = language in supportedLanguages
}
