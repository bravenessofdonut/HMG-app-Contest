package com.braveberry.voice.remote.model

data class TtsSpeechRequestDto(
    val text: String,
    val voice: String,
    val language: String
)
