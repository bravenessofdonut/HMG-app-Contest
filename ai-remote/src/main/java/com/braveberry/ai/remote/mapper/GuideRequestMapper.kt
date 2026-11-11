package com.braveberry.ai.remote.mapper

import com.braveberry.ai.data.model.GuideRequestDataModel
import com.braveberry.ai.remote.model.GuideRequestDto

fun GuideRequestDataModel.toDto(): GuideRequestDto =
    GuideRequestDto(
        region = region,
        language = language
    )