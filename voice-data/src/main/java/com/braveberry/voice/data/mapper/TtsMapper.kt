package com.braveberry.voice.data.mapper

import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.RemoteTtsPlayback
import com.braveberry.voice.data.model.TtsOptionsDataModel
import com.braveberry.voice.data.model.TtsPlaybackDataModel

fun TtsPlaybackDataModel.toDomain(): RemoteTtsPlayback =
    when (this) {
        TtsPlaybackDataModel.SPEAKING -> RemoteTtsPlayback.SPEAKING
        TtsPlaybackDataModel.DONE -> RemoteTtsPlayback.DONE
        TtsPlaybackDataModel.STOPPED -> RemoteTtsPlayback.STOPPED
    }

fun TtsOptionsDataModel.toDomain(): RemoteTtsOptions =
    RemoteTtsOptions(
        speakers = voices,
        defaultSpeaker = defaultVoice,
        languages = languages
    )
