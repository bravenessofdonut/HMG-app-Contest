package com.braveberry.ai.remote.factory

import com.braveberry.ai.remote.api.GeminiApiService
import com.braveberry.ai.remote.api.FirebaseGeminiApiService
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI

object GeminiServiceFactory {

    @OptIn(PublicPreviewAPI::class)
    fun create(): GeminiApiService {

        val model = Firebase.ai(
            //Gemini Developer API 사용
            backend = GenerativeBackend.googleAI()
        ).templateGenerativeModel()

        return FirebaseGeminiApiService(model)
    }
}
