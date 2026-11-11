package com.braveberry.voice.remote.impl

import com.braveberry.voice.data.datasource.TtsRemoteDataSource
import com.braveberry.voice.data.model.TtsOptionsDataModel
import com.braveberry.voice.data.model.TtsPlaybackDataModel
import com.braveberry.voice.remote.model.GeminiSpeakerCatalog
import com.braveberry.voice.remote.session.TtsPlaybackSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class TtsRemoteDataSourceImpl @Inject constructor(
    private val session: TtsPlaybackSession
) : TtsRemoteDataSource {

    override val options = TtsOptionsDataModel(
        voices = GeminiSpeakerCatalog.speakers,
        defaultVoice = GeminiSpeakerCatalog.defaultSpeaker,
        languages = GeminiSpeakerCatalog.supportedLanguages
    )

    override fun speak(
        text: String,
        voice: String,
        language: String
    ): Flow<TtsPlaybackDataModel> = speakStream(flowOf(text), voice, language)

    override fun speakStream(
        tokens: Flow<String>,
        voice: String,
        language: String
    ): Flow<TtsPlaybackDataModel> = flow {
        // 텍스트와 다른 언어로 합성되지 않도록 대체 언어 없이 실패시킴.
        if (!GeminiSpeakerCatalog.isSupportedLanguage(language)) {
            throw UnsupportedOperationException("Unsupported language: $language")
        }
        val resolvedVoice = voice.takeIf { GeminiSpeakerCatalog.isSupported(it) }
            ?: GeminiSpeakerCatalog.defaultSpeaker

        emitAll(session.speak(tokens, resolvedVoice, language))
    }
}
