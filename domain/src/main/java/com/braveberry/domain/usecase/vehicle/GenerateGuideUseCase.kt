package com.braveberry.domain.usecase.vehicle

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.ai.GuideRequest
import com.braveberry.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow

class GenerateGuideUseCase(
    private val repository: AiRepository
) {
     operator fun invoke(
        request: GuideRequest
    ): Flow<DataResource<String>> {
        require(request.placeName.isNotBlank()) {
            "Place name cannot be empty"
        }

        require(request.language.isNotBlank()) {
            "Language cannot be empty"
        }

        return repository.generateGuide(request)
    }
}