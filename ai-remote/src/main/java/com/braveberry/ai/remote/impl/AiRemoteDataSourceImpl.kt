package com.braveberry.ai.remote.impl

import com.braveberry.ai.data.datasource.AiRemoteDataSource
import com.braveberry.ai.data.model.GuideRequestDataModel
import com.braveberry.ai.remote.api.GeminiApiService
import com.braveberry.ai.remote.mapper.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AiRemoteDataSourceImpl @Inject constructor(
    private val api: GeminiApiService
) : AiRemoteDataSource {

    override fun generateGuide(
        request: GuideRequestDataModel
    ): Flow<String> = flow {
        api.generateGuide(request.toDto())
            .collect { response ->
                emit(response.text)
            }
    }
}