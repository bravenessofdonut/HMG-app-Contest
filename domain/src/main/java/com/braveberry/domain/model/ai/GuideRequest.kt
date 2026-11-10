package com.braveberry.domain.model.ai

data class GuideRequest(
    val placeName: String,
    val region: String,
    val language: String
)