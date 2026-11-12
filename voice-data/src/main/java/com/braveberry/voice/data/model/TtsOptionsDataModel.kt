package com.braveberry.voice.data.model

data class TtsOptionsDataModel(
    val voices: List<String>,
    val defaultVoice: String,
    val languages: List<String>
)
