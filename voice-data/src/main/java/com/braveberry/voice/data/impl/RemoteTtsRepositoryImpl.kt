package com.braveberry.voice.data.impl

import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.data_resource.asDataResourceFlow
import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.RemoteTtsPlayback
import com.braveberry.domain.repository.RemoteTtsRepository
import com.braveberry.voice.data.datasource.TtsRemoteDataSource
import com.braveberry.voice.data.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class RemoteTtsRepositoryImpl @Inject constructor(
    private val datasource: TtsRemoteDataSource
) : RemoteTtsRepository {

    override val options: RemoteTtsOptions
        get() = datasource.options.toDomain()

    override fun speak(
        text: String,
        voiceName: String,
        language: String
    ): Flow<DataResource<RemoteTtsPlayback>> =
        datasource.speak(text, voiceName, language)
            .map { it.toDomain() }
            .asDataResourceFlow(::classifyTtsFailure)

    override fun speakStream(
        tokens: Flow<String>,
        voiceName: String,
        language: String
    ): Flow<DataResource<RemoteTtsPlayback>> =
        datasource.speakStream(tokens, voiceName, language)
            .map { it.toDomain() }
            .asDataResourceFlow(::classifyTtsFailure)
}

private fun classifyTtsFailure(cause: Exception): ResourceFailure? =
    when (cause) {
        is SecurityException -> ResourceFailure.PermissionRequired(cause)
        is UnsupportedOperationException -> ResourceFailure.Unsupported(cause)
        is IOException -> ResourceFailure.ServiceUnavailable(cause)
        else -> null
    }
