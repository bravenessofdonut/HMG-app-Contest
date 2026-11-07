package com.braveberry.voice.local.model

import java.util.Locale

/** Pleos TextToSpeech 3.3.1 문서의 speaker ID와 지원 합성 locale을 관리함. */
internal object PleosSpeakerCatalog {
    // TTS SDK 지원 locale: ko-KR, en-US, es-US/es-MX, fr-CA
    val languageTags: Map<String, String> = linkedMapOf(
        "seoyoon" to "ko-KR",
        "eunwoo" to "ko-KR",
        "haeun" to "ko-KR",
        "sanghyeok" to "ko-KR",
        "lyra" to "en-US",
        "finley" to "en-US",
        "iris" to "es-US",
        "arlo" to "es-US",
        "sabine" to "fr-CA",
        "noah" to "fr-CA"
    )

    val locales: Map<String, Locale> = languageTags.mapValues { (_, tag) ->
        Locale.forLanguageTag(tag)
    }
}
