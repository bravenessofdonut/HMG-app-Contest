
package com.braveberry.ai.remote.impl

import com.braveberry.ai.data.datasource.AiRemoteDataSource
import com.braveberry.ai.data.model.GuideRequestDataModel
import com.braveberry.ai.remote.api.GeminiApiService
import com.braveberry.ai.remote.mapper.toDto
import com.google.firebase.ai.type.FirebaseAIException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class AiRemoteDataSourceImpl @Inject constructor(
    private val api: GeminiApiService
) : AiRemoteDataSource {

    override fun generateGuide(
        request: GuideRequestDataModel
    ): Flow<String> = flow {
        try {
            api.generateGuide(request.toDto())
                .collect { response ->
                    emit(response.text)
                }

        } catch (e: CancellationException) {
            throw e

        } catch (e: FirebaseAIException) {
            throw IOException(
                "Firebase AI generation failed",
                e
            )
        }
    }
}
