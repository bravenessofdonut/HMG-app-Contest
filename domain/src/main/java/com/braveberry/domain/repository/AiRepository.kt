package com.braveberry.domain.repository

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.ai.GuideRequest
import kotlinx.coroutines.flow.Flow

interface AiRepository {
     fun generateGuide(
        request: GuideRequest
    ): Flow<DataResource<String>>
}