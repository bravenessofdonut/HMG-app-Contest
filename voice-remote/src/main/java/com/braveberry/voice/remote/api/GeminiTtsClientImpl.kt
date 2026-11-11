package com.braveberry.voice.remote.api

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

@Suppress("OPT_IN_USAGE", "OPT_IN_USAGE_ERROR")
internal class GeminiTtsClientImpl @Inject constructor() : GeminiTtsClient {

    private companion object {
        const val MODEL_NAME = "gemini-3.1-flash-tts-preview"
    }

    private val models = ConcurrentHashMap<Pair<String, String>, GenerativeModel>()

    private fun modelFor(voiceName: String, languageCode: String): GenerativeModel =
        models.getOrPut(voiceName to languageCode) {
            val config = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO)
                speechConfig = SpeechConfig(
                    voice = Voice(voiceName),
                    languageCode = languageCode
                )
            }
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(modelName = MODEL_NAME, generationConfig = config)
        }


    override fun generateSpeechStream(
        text: String,
        voiceName: String,
        languageCode: String
    ): Flow<ByteArray> = flow {
        modelFor(voiceName, languageCode)
            .generateContentStream(text)
            .collect { response ->
                response.candidates.firstOrNull()?.content?.parts
                    ?.filterIsInstance<InlineDataPart>()   // 오디오 part를 전부 처리
                    // 오디오가 아닌 part는 건너뛰고, 재생할 수 없는 형식은 잡음 대신 실패로 처리함.
                    ?.filter { GeminiAudioFormat.isPlayableAudio(it.mimeType) }
                    ?.forEach { emit(it.inlineData) }
            }
    }
}
