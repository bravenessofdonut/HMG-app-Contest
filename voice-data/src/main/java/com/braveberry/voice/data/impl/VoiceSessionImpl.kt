package com.braveberry.voice.data.impl

import android.util.Log
import com.braveberry.domain.model.voice.EngineStatus
import com.braveberry.domain.model.voice.ListeningPhase
import com.braveberry.domain.model.voice.PlaybackPhase
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.domain.model.voice.VoiceSessionState
import com.braveberry.domain.model.voice.VoiceTurn
import com.braveberry.domain.repository.VoiceRepository
import com.braveberry.domain.usecase.voice.VoiceSession
import com.braveberry.domain.usecase.voice.VoiceSessionOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Activity-retained 음성 런타임. SDK 연결, lease 및 제한시간은 Domain의 전이 모델과 분리함.
 * 모든 외부 명령·SDK 콜백·타임아웃을 하나의 mailbox에서 순차 처리함.
 */
class VoiceSessionImpl(
    private val openConnection: () -> VoiceRepository,
    private val scope: CoroutineScope
) : VoiceSession, VoiceSessionOwner {
    private val current = MutableStateFlow(VoiceSessionState())
    override val state = current.asStateFlow()

    private sealed interface Command {
        data object Acquire : Command
        data object Release : Command
        data object Connect : Command
        data class Speak(val turn: VoiceTurn, val following: VoiceTurn? = null) : Command
        data object StopSpeech : Command
        data object StartListening : Command
        data object StopListening : Command
        data class SdkEvent(val generation: Long, val event: VoiceEvent) : Command
        data class Broken(val generation: Long, val reason: String) : Command
        data class Expired(val generation: Long, val ticket: Long, val stage: Stage) : Command
    }
    private enum class Stage { INITIALIZE, STT_INITIALIZE, START, PLAYBACK, STOP, STT_START, STT_STOP }

    private val mailbox = Channel<Command>(Channel.UNLIMITED)
    @Volatile private var shutDown = false
    private var leases = 0
    private var connection: VoiceRepository? = null
    private var eventJob: Job? = null
    private var timeoutJob: Job? = null
    private var sttTimeoutJob: Job? = null
    private var generation = 0L
    private var nextTicket = 0L
    private var currentTicket = 0L
    private var sttTicket = 0L

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            for (command in mailbox) process(command)
        }
    }

    private fun enqueue(command: Command) {
        if (shutDown) return
        mailbox.trySend(command).getOrThrow()
    }

    override fun acquire(): VoiceSession {
        enqueue(Command.Acquire)
        return this
    }

    override fun release() = enqueue(Command.Release)
    override fun connect() = enqueue(Command.Connect)

    override fun speak(text: String, speaker: String) {
        if (text.isNotBlank()) enqueue(Command.Speak(VoiceTurn(text.trim(), speaker)))
    }

    override fun previewDialogue(aText: String, bText: String, aSpeaker: String, bSpeaker: String) {
        if (aText.isBlank() || bText.isBlank()) return
        enqueue(Command.Speak(
            VoiceTurn(aText.trim(), aSpeaker, "A"),
            VoiceTurn(bText.trim(), bSpeaker, "B")
        ))
    }

    override fun stopSpeaking() = enqueue(Command.StopSpeech)
    override fun startListening() = enqueue(Command.StartListening)
    override fun stopListening() = enqueue(Command.StopListening)

    private fun process(command: Command) {
        when (command) {
            Command.Acquire -> {
                leases++
                openIfNeeded()
            }
            Command.Release -> {
                check(leases > 0) { "Unbalanced VoiceSession lease" }
                leases--
                if (leases == 0 && state.value.recognitionActive) stopRecognition()
                closeIfUnused()
            }
            Command.Connect -> if (leases > 0 && connection == null) openIfNeeded()
            is Command.Speak -> {
                val s = state.value
                if (!s.canSpeak) return
                // UI가 잘못된 값을 전달해도 다국어 팟캐스트를 생성할 수 없게 함.
                if (command.following != null && !s.canPreview(command.turn.speaker, command.following.speaker)) return
                val plan = s.playback.begin(command.turn, command.following) ?: return
                current.value = s.copy(playback = plan)
                submitTurn(command.turn)
            }
            Command.StopSpeech -> stopSpeech()
            Command.StartListening -> startRecognition()
            Command.StopListening -> stopRecognition()
            is Command.SdkEvent -> if (command.generation == generation) handleSdk(command.event)
            is Command.Broken -> if (command.generation == generation) failConnection(command.reason)
            is Command.Expired -> if (command.generation == generation) handleTimeout(command)
        }
    }

    private fun openIfNeeded() {
        if (connection != null || leases == 0) return
        val engine = try {
            openConnection()
        } catch (failure: Exception) {
            failConnection("CONNECT_FAILED: ${failure.javaClass.simpleName}")
            return
        }
        connection = engine
        val epoch = ++generation
        current.value = VoiceSessionState()
        eventJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                engine.events.collect { mailbox.send(Command.SdkEvent(epoch, it)) }
                mailbox.send(Command.Broken(epoch, "EVENT_STREAM_CLOSED"))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                mailbox.send(Command.Broken(epoch, "EVENT_STREAM_FAILED: ${failure.javaClass.simpleName}"))
            }
        }
        arm(Stage.INITIALIZE, 10_000)
        armStt(Stage.STT_INITIALIZE, 10_000)
        try {
            engine.initialize()
        } catch (failure: Exception) {
            failConnection("INITIALIZE_FAILED: ${failure.javaClass.simpleName}")
        }
    }

    private fun submitTurn(turn: VoiceTurn) {
        val engine = connection ?: return failConnection("ENGINE_MISSING")
        try {
            if (turn.speaker.isNotBlank() && !engine.selectSpeaker(turn.speaker)) {
                failConnection("SPEAKER_REJECTED")
                return
            }
            arm(Stage.START, 5_000)
            engine.speak(turn.text)
        } catch (failure: Exception) {
            failConnection("SPEAK_FAILED: ${failure.javaClass.simpleName}")
        }
    }

    private fun stopSpeech() {
        val plan = current.value.playback.requestStop() ?: return
        current.value = current.value.copy(playback = plan)
        try {
            arm(Stage.STOP, 3_000)
            connection?.stopSpeaking() ?: failConnection("ENGINE_MISSING")
        } catch (failure: Exception) {
            failConnection("STOP_FAILED: ${failure.javaClass.simpleName}")
        }
    }

    private fun startRecognition() {
        val s = current.value
        if (s.disconnected || s.speaking || s.recognitionActive || s.sttStatus != EngineStatus.READY) return
        current.value = s.copy(listeningPhase = ListeningPhase.REQUESTED)
        try {
            armStt(Stage.STT_START, 5_000)
            connection?.startListening() ?: failConnection("ENGINE_MISSING", VoiceEvent.Engine.STT)
        } catch (failure: Exception) {
            failConnection("STT_START_FAILED: ${failure.javaClass.simpleName}", VoiceEvent.Engine.STT)
        }
    }

    private fun stopRecognition() {
        if (!state.value.recognitionActive || state.value.listeningPhase == ListeningPhase.STOPPING) return
        current.value = current.value.copy(listeningPhase = ListeningPhase.STOPPING)
        try {
            armStt(Stage.STT_STOP, 3_000)
            connection?.stopListening() ?: failConnection("ENGINE_MISSING", VoiceEvent.Engine.STT)
        } catch (failure: Exception) {
            failConnection("STT_STOP_FAILED: ${failure.javaClass.simpleName}", VoiceEvent.Engine.STT)
        }
    }

    private fun handleSdk(event: VoiceEvent) {
        when (event) {
            VoiceEvent.Speaking -> {
                val plan = current.value.playback.started() ?: return
                current.value = current.value.copy(playback = plan, ttsError = null)
                val turn = (plan.phase as PlaybackPhase.Playing).turn
                // 임시 정책: 합성 언어와 문장 길이에 따라 여유 있게 설정하며 실기기로 보정함.
                arm(Stage.PLAYBACK, (15_000L + turn.text.length * 350L).coerceAtMost(180_000L))
            }
            VoiceEvent.SpeechDone -> {
                val completion = current.value.playback.finished() ?: return
                disarm()
                current.value = current.value.copy(playback = completion.completed)
                val following = completion.following
                if (following != null) {
                    val next = completion.completed.begin(following) ?: return
                    current.value = current.value.copy(playback = next)
                    submitTurn(following)
                }
            }
            VoiceEvent.SpeechStopped -> {
                val plan = current.value.playback.stopped() ?: return
                disarm()
                current.value = current.value.copy(playback = plan)
                // stop() 이후 뒤늦은 onDone이 새 발화에 속하는 것처럼 보이지 않도록 엔진 교체.
                recycleAfterStop()
            }
            is VoiceEvent.Failed -> {
                if (event.engine == VoiceEvent.Engine.TTS) {
                    failConnection("TTS: ${event.reason}")
                } else {
                    disarmStt()
                    current.value = current.value.applyEvent(event)
                }
            }
            else -> {
                // 늦거나 역순인 STT 콜백이 중지/종료 상태를 덮어쓰지 못하도록 제한함.
                if (event == VoiceEvent.SttInputReady &&
                    state.value.listeningPhase != ListeningPhase.REQUESTED) return
                if (event == VoiceEvent.Listening && state.value.listeningPhase !in
                    setOf(ListeningPhase.REQUESTED, ListeningPhase.INPUT_READY)) return
                if (event == VoiceEvent.ListeningStopped && !state.value.recognitionActive) return
                if (event == VoiceEvent.TtsReady) disarm()
                if (event == VoiceEvent.SttReady) disarmStt()
                if (event == VoiceEvent.Listening) disarmStt()
                if (event == VoiceEvent.ListeningStopped) disarmStt()
                current.value = current.value.applyEvent(event)
            }
        }
        closeIfUnused()
    }

    private fun handleTimeout(command: Command.Expired) {
        if (command.stage in setOf(Stage.STT_INITIALIZE, Stage.STT_START, Stage.STT_STOP)) {
            if (command.ticket != sttTicket) return
            if (command.stage == Stage.STT_INITIALIZE) {
                disarmStt()
                current.value = current.value.copy(sttStatus = EngineStatus.ERROR, sttError = "STT_INITIALIZE_TIMEOUT")
            } else failConnection("${command.stage}_TIMEOUT", VoiceEvent.Engine.STT)
        } else if (command.ticket == currentTicket) {
            failConnection("${command.stage}_TIMEOUT")
        }
    }

    private fun arm(stage: Stage, milliseconds: Long) {
        timeoutJob?.cancel()
        val ticket = ++nextTicket
        currentTicket = ticket
        val epoch = generation
        timeoutJob = scope.launch {
            delay(milliseconds)
            enqueue(Command.Expired(epoch, ticket, stage))
        }
    }

    private fun disarm() {
        timeoutJob?.cancel()
        timeoutJob = null
        currentTicket = ++nextTicket
    }

    private fun armStt(stage: Stage, milliseconds: Long) {
        sttTimeoutJob?.cancel()
        val ticket = ++nextTicket
        sttTicket = ticket
        val epoch = generation
        sttTimeoutJob = scope.launch {
            delay(milliseconds)
            enqueue(Command.Expired(epoch, ticket, stage))
        }
    }

    private fun disarmStt() {
        sttTimeoutJob?.cancel()
        sttTimeoutJob = null
        sttTicket = ++nextTicket
    }

    private fun recycleAfterStop() {
        closeEngine()
        if (leases > 0) openIfNeeded() else current.value = VoiceSessionState()
    }

    private fun failConnection(reason: String, engine: VoiceEvent.Engine = VoiceEvent.Engine.TTS) {
        val failed = current.value.copy(
            playback = current.value.playback.failed(reason),
            disconnected = true,
            ttsStatus = EngineStatus.ERROR,
            sttStatus = EngineStatus.ERROR,
            listening = false,
            listeningPhase = ListeningPhase.ENDED,
            ttsError = if (engine == VoiceEvent.Engine.TTS) reason else current.value.ttsError,
            sttError = if (engine == VoiceEvent.Engine.STT) reason else current.value.sttError
        )
        closeEngine()
        current.value = failed
    }

    private fun closeIfUnused() {
        if (leases != 0 || state.value.speaking || state.value.recognitionActive) return
        closeEngine()
        current.value = VoiceSessionState()
    }

    private fun closeEngine() {
        ++generation // 큐에 남은 이전 엔진의 이벤트를 모두 무효화함.
        disarm()
        disarmStt()
        eventJob?.cancel()
        eventJob = null
        val previous = connection
        connection = null
        try { previous?.release() } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            Log.w("VoiceSession", "Pleos SDK release failed; connection discarded", failure)
        }
    }

    /** ActivityRetainedLifecycle이 종료될 때 호출. 새 명령을 더 이상 처리하지 않음. */
    fun shutdown() {
        if (shutDown) return
        shutDown = true
        closeEngine()
        mailbox.close()
    }
}
