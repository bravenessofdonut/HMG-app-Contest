
package com.braveberry.ai.remote.api

import com.braveberry.ai.remote.model.GuideRequestDto
import com.braveberry.ai.remote.model.GuideResponseDto
import com.google.firebase.ai.TemplateGenerativeModel
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(PublicPreviewAPI::class)
class FirebaseGeminiApiService(
    private val model: TemplateGenerativeModel
) : GeminiApiService {

    override suspend fun generateGuide(
        request: GuideRequestDto
    ): Flow<GuideResponseDto> =
        model.generateContentStream(
            templateId = "guide-v1",
            inputs = mapOf(
                "placeName" to request.placeName,
                "placeDescription" to request.region,
                "language" to request.language
            )
        ).map { response ->
            GuideResponseDto(
                text = response.text.orEmpty()
            )
        }
}