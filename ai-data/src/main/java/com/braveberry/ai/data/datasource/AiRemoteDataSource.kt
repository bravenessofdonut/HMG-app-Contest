package com.braveberry.ai.data.datasource

import com.braveberry.ai.data.model.GuideRequestDataModel
import kotlinx.coroutines.flow.Flow

interface AiRemoteDataSource {

    fun generateGuide(
        request: GuideRequestDataModel
    ): Flow<String>
}