package com.braveberry.voice.remote.util

internal object TtsTextSanitizer {
    private val link = Regex("""\[([^\]]*)]\([^)]*\)""")
    private val url = Regex("""https?://\S+""")
    private val heading = Regex("""(?m)^\s{0,3}#{1,6}\s*""")
    private val listMarker = Regex("""(?m)^\s*(?:[-*+]|\d+\.)\s+""")
    private val emphasis = Regex("""[*_~`]+""")
    private val symbols = Regex("""[\p{So}\uFE0F\u200D|]""")   // 이모지, 표 구분자
    private val spaces = Regex("""\s+""")

    fun clean(raw: String): String = raw
        .replace(link, "$1")
        .replace(url, "")
        .replace(heading, "")
        .replace(listMarker, "")
        .replace(emphasis, "")
        .replace(symbols, " ")
        .replace(spaces, " ")
        .trim()
}
