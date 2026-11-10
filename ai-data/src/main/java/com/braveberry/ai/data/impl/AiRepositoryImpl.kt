package com.braveberry.ai.data.impl

import com.braveberry.ai.data.datasource.AiRemoteDataSource
import com.braveberry.ai.data.mapper.toDataModel
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.data_resource.asDataResourceFlow
import com.braveberry.domain.model.ai.GuideRequest
import com.braveberry.domain.repository.AiRepository
import jdk.internal.net.http.common.TimeSource.source
import java.io.IOException
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val datasource: AiRemoteDataSource
) : AiRepository {

    override fun generateGuide(request: GuideRequest) =
        datasource.generateGuide(request.toDataModel())
            .asDataResourceFlow(::classifyAiFailure)
}

private fun classifyAiFailure(cause: Exception): ResourceFailure? =
    when (cause) {
        //서비스&네트워크 장애만 일단
        is IOException ->
            ResourceFailure.ServiceUnavailable(cause)
        else -> null
    }