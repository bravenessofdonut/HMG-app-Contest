package com.braveberry.voice.remote.impl

import com.braveberry.voice.data.datasource.SpeechSynthesisDataSource
import com.braveberry.voice.remote.mapper.toSynthesisError
import com.braveberry.voice.remote.model.GeminiAudioFormat
import com.braveberry.voice.remote.model.GeminiSpeakerCatalog
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

/** Firebase AI Logic의 Gemini TTS로 문장을 합성하는 DataSource. */
@Suppress("OPT_IN_USAGE", "OPT_IN_USAGE_ERROR")
internal class GeminiSpeechSynthesisDataSourceImpl @Inject constructor() : SpeechSynthesisDataSource {

    private companion object {
        const val MODEL_NAME = "gemini-3.1-flash-tts-preview"
    }

    override val voices = GeminiSpeakerCatalog.speakers
    override val defaultVoice = GeminiSpeakerCatalog.defaultSpeaker
    override val languages = GeminiSpeakerCatalog.supportedLanguages
    override val sampleRateHz = GeminiAudioFormat.SAMPLE_RATE

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

    override fun synthesize(text: String, voice: String, language: String): Flow<ByteArray> = flow {
        modelFor(voice, language)
            .generateContentStream(text)
            .collect { response ->
                response.candidates.firstOrNull()?.content?.parts
                    ?.filterIsInstance<InlineDataPart>()   // 오디오 part를 전부 처리
                    // 오디오가 아닌 part는 건너뛰고, 재생할 수 없는 형식은 잡음 대신 실패로 처리함.
                    ?.filter { GeminiAudioFormat.isPlayableAudio(it.mimeType) }
                    ?.forEach { emit(it.inlineData) }
            }
    }.catch { cause ->
        // Firebase 예외 타입이 data로 새지 않도록 계약의 표준 예외로 변환함.
        if (cause is CancellationException) throw cause
        throw cause.toSynthesisError()
    }
}
