package com.braveberry.ai.data.impl

import com.braveberry.ai.data.datasource.AiRemoteDataSource
import com.braveberry.ai.data.mapper.toDataModel
import com.braveberry.ai.data.parser.parseGuideStream
import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.data_resource.asDataResourceFlow
import com.braveberry.domain.model.ai.GuideRequest
import com.braveberry.domain.model.ai.GuideStreamModel
import com.braveberry.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val datasource: AiRemoteDataSource
) : AiRepository {

    override fun generateGuide(
        request: GuideRequest
    ): Flow<DataResource<GuideStreamModel>> =
        datasource.generateGuide(request.toDataModel())
            .parseGuideStream()
            .asDataResourceFlow(::classifyAiFailure)
}

private fun classifyAiFailure(cause: Exception): ResourceFailure? =
    when (cause) {
        is IOException ->
            ResourceFailure.ServiceUnavailable(cause)

        else -> null
    }