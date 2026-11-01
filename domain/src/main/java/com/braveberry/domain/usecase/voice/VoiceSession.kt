package com.braveberry.domain.usecase.voice

import com.braveberry.domain.model.voice.EngineStatus
import com.braveberry.domain.model.voice.ListeningPhase
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.domain.model.voice.VoiceSessionState
import com.braveberry.domain.repository.VoiceRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** 명령과 SDK 이벤트를 같은 메인 스레드에서 처리함. 엔진은 첫 연결 요청 때 생성함. */
class VoiceSession(
    private val openConnection: () -> VoiceRepository,
    private val scope: CoroutineScope
) {
    private val current = MutableStateFlow(VoiceSessionState())
    val state = current.asStateFlow()

    private var connection: VoiceRepository? = null
    private var observer: Job? = null
    private var nextTurn: Turn? = null
    private data class Turn(val speaker: String, val text: String)

    private var screens = 0

    fun attachScreen() {
        screens++
        connect()
    }

    fun detachScreen() {
        check(screens > 0)
        screens--
        if (screens == 0) {
            stopListening()
            releaseIfIdle()
        }
    }

    private fun releaseIfIdle() {
        if (screens == 0 && !current.value.speaking) disconnect()
    }

    fun connect() {
        if (screens == 0 || connection != null) return
        val engine = openConnection()
        connection = engine
        current.value = VoiceSessionState()
        observer = scope.launch {
            engine.events.catch { cause ->
                if (cause !is IOException) throw cause
                if (connection === engine) {
                    disconnect()
                    current.value = VoiceSessionState(disconnected = true)
                }
            }.collect { event ->
                if (connection === engine) handle(event)
            }
        }
        engine.initialize()
    }

    fun speak(text: String, speaker: String) {
        if (text.isBlank() || !state.value.canSpeak) return
        play(Turn(speaker, text.trim()), role = null)
    }

    fun previewDialogue(aText: String, bText: String, aSpeaker: String, bSpeaker: String) {
        if (!state.value.canSpeak || aText.isBlank() || bText.isBlank()) return
        if (aSpeaker.isBlank() || bSpeaker.isBlank() || aSpeaker == bSpeaker) return
        nextTurn = Turn(bSpeaker, bText.trim())
        play(Turn(aSpeaker, aText.trim()), role = "A")
    }

    private fun play(turn: Turn, role: String?) {
        val engine = connection ?: return
        if (turn.speaker.isNotBlank() && !engine.selectSpeaker(turn.speaker)) {
            nextTurn = null
            current.value = current.value.copy(speaking = false, previewTurn = null)
            return
        }
        current.value = current.value.copy(speaking = true, previewTurn = role)
        engine.speak(turn.text)
    }

    fun stopSpeaking() {
        if (!state.value.speaking || state.value.stopping) return
        nextTurn = null
        current.value = current.value.copy(stopping = true, previewTurn = null)
        connection?.stopSpeaking()
    }

    fun startListening() {
        val s = state.value
        if (s.disconnected || s.speaking || s.stopping || s.recognitionActive ||
            s.sttStatus != EngineStatus.READY) return
        current.value = s.copy(listeningPhase = ListeningPhase.REQUESTED)
        connection?.startListening()
    }

    fun stopListening() {
        if (state.value.recognitionActive) connection?.stopListening()
    }

    private fun handle(event: VoiceEvent) {
        val before = current.value
        val updated = before.applyEvent(event)
        when (event) {
            VoiceEvent.SpeechDone, VoiceEvent.SpeechStopped -> {
                val next = nextTurn
                nextTurn = null
                current.value = updated.copy(stopping = false, previewTurn = null)
                if (event == VoiceEvent.SpeechDone && !before.stopping && next != null) play(next, "B")
            }
            is VoiceEvent.Failed -> {
                if (event.engine == VoiceEvent.Engine.TTS) {
                    nextTurn = null
                    current.value = updated.copy(stopping = false, previewTurn = null)
                } else current.value = updated
            }
            else -> current.value = updated
        }
        releaseIfIdle()
    }

    fun disconnect() {
        observer?.cancel()
        observer = null
        nextTurn = null
        val old = connection
        connection = null
        old?.release()
        current.value = VoiceSessionState()
    }
}
