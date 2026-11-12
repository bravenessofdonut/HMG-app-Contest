package com.braveberry.voice.remote.api

import com.braveberry.voice.remote.model.GeminiAudioFormat
import com.braveberry.voice.remote.model.TtsSpeechRequestDto
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Firebase AI Logic의 Gemini TTS 호출. 화자·언어 조합마다 모델을 한 번만 생성함. */
@OptIn(PublicPreviewAPI::class)
@Singleton
class FirebaseGeminiTtsApiService @Inject constructor() : GeminiTtsApiService {

    private companion object {
        const val MODEL_NAME = "gemini-3.1-flash-tts-preview"
    }

    private val models = ConcurrentHashMap<Pair<String, String>, GenerativeModel>()

    private fun modelFor(voice: String, language: String): GenerativeModel =
        models.getOrPut(voice to language) {
            val config = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO)
                speechConfig = SpeechConfig(
                    voice = Voice(voice),
                    languageCode = language
                )
            }
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(modelName = MODEL_NAME, generationConfig = config)
        }

    override fun generateSpeech(
        request: TtsSpeechRequestDto
    ): Flow<ByteArray> = flow {
        modelFor(request.voice, request.language)
            .generateContentStream(request.text)
            .collect { response ->
                response.candidates.firstOrNull()?.content?.parts
                    ?.filterIsInstance<InlineDataPart>()   // 오디오 part를 전부 처리
                    // 오디오가 아닌 part는 건너뛰고, 재생할 수 없는 형식은 잡음 대신 실패로 처리함.
                    ?.filter { GeminiAudioFormat.isPlayableAudio(it.mimeType) }
                    ?.forEach { emit(it.inlineData) }
            }
    }
}
