package com.braveberry.ai.remote.api

import com.braveberry.ai.remote.model.GuideRequestDto
import com.braveberry.ai.remote.model.GuideResponseDto
import kotlinx.coroutines.flow.Flow

interface GeminiApiService {

    suspend fun generateGuide(
        request: GuideRequestDto
    ): Flow<GuideResponseDto>
}
