package com.braveberry.domain.usecase.ai

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.ai.GuideRequest
import com.braveberry.domain.model.ai.GuideStreamModel
import com.braveberry.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GenerateGuideUseCase @Inject constructor(
    private val repository: AiRepository
) {
    operator fun invoke(
        request: GuideRequest
    ): Flow<DataResource<GuideStreamModel>> {
        return repository.generateGuide(request)
    }
}


