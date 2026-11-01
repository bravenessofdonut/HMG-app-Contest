package com.braveberry.voice.data.impl

import com.braveberry.domain.repository.VoiceRepository
import com.braveberry.voice.data.datasource.VoiceDataSource
import javax.inject.Inject

class VoiceRepositoryImpl @Inject constructor(
    private val source: VoiceDataSource
) : VoiceRepository {
    override val events get() = source.events
    override fun initialize() = source.initialize()
    override fun speak(text: String) = source.speak(text)
    override fun selectSpeaker(value: String) = source.selectSpeaker(value)
    override fun stopSpeaking() = source.stopSpeaking()
    override fun startListening() = source.startListening()
    override fun stopListening() = source.stopListening()
    override fun release() = source.release()
}
