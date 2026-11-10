package com.braveberry.ai.data.mapper

import com.braveberry.ai.data.model.GuideRequestDataModel
import com.braveberry.domain.model.ai.GuideRequest

fun GuideRequest.toDataModel(): GuideRequestDataModel {
    return GuideRequestDataModel(
        placeName = placeName,
        region = region,
        language = language
    )
}