package com.braveberry.voice.remote.impl

import android.util.Log
import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.domain.repository.RemoteTtsRepository
import com.braveberry.voice.remote.api.EmptyAudioException
import com.braveberry.voice.remote.api.GeminiTtsClient
import com.braveberry.voice.remote.api.SynthesisException
import com.braveberry.voice.remote.api.SynthesisRetryPolicy
import com.braveberry.voice.remote.api.SynthesisTimeoutException
import com.braveberry.voice.remote.model.GeminiSpeakerCatalog
import com.braveberry.voice.remote.player.PcmAudioPlayer
import com.braveberry.voice.remote.source.RemoteVoiceEventInbox
import com.braveberry.voice.remote.util.TtsTextSanitizer
import com.braveberry.voice.remote.util.toSpeechChunks
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Gemini Remote TTS를 제공하는 RemoteTtsRepository 구현체.
 */
internal class RemoteTtsRepositoryImpl(
    private val client: GeminiTtsClient,
    private val player: PcmAudioPlayer,
    dispatcher: CoroutineDispatcher
) : RemoteTtsRepository {

    @Inject
    constructor(client: GeminiTtsClient, player: PcmAudioPlayer) :
        this(client, player, Dispatchers.Default)

    private companion object {
        const val TAG = "RemoteTtsRepository"
        const val PCM_BUFFER_CHUNKS = 64      // 재생 중 다음 문장을 미리 합성할 수 있는 한도
        const val MAX_SYNTH_RETRY = 1
        const val RETRY_DELAY_MS = 300L
        // 문장별 Gemini 응답 대기 한도. 첫 오디오 청크까지와 청크 사이 간격에 적용함.
        // 네트워크 타임아웃 임시 값.
        const val SYNTH_RESPONSE_TIMEOUT_MS = 8_000L
        // 중지 요청 후 재생 코루틴 종료 대기 한도. pause+flush로 정상 해제되면 즉시 끝나는 구간임.
        const val STOP_JOIN_TIMEOUT_MS = 1_000L
    }

    private sealed interface Command {
        class Speak(val tokens: Flow<String>, val voice: String, val language: String) : Command
        class Finished(val job: Job) : Command   // 재생 코루틴 종료 통지 (루프로 회송)
        data object Stop : Command
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val inbox = RemoteVoiceEventInbox()
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val released = AtomicBoolean(false)

    private var playback: Job? = null

    override val events: Flow<VoiceEvent> = inbox.events

    override val options = RemoteTtsOptions(
        speakers = GeminiSpeakerCatalog.speakers,
        defaultSpeaker = GeminiSpeakerCatalog.defaultSpeaker,
        languages = GeminiSpeakerCatalog.supportedLanguages
    )

    init {
        scope.launch { runLoop() }
    }

    private fun fail(engine: VoiceEvent.Engine, cause: Throwable) {
        if (cause is CancellationException) throw cause
        val reason = if (cause is SynthesisException) "SYNTHESIS_FAILED" else "PLAYBACK_FAILED"
        // 세부 원인은 로그로만 남김.
        Log.w(TAG, "Remote TTS failed ($engine): $reason", cause)
        inbox.offer(VoiceEvent.Failed(engine, reason))
    }

    private suspend fun runLoop() {
        try {
            for (cmd in commands) {
                if (released.get()) break
                try {
                    when (cmd) {
                        is Command.Speak -> {
                            stopPlayback()
                            val job = scope.launch { play(cmd) }
                            job.invokeOnCompletion { commands.trySend(Command.Finished(job)) }
                            playback = job
                        }
                        is Command.Finished -> if (playback === cmd.job) playback = null
                        Command.Stop -> stopPlayback()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fail(VoiceEvent.Engine.TTS, e)
                }
            }
        } finally {
            withContext(NonCancellable) {
                runCatching { stopPlayback() }
                    .onFailure { Log.w(TAG, "stopPlayback failed on release", it) }
                runCatching { player.release() }
                    .onFailure { Log.w(TAG, "player.release failed", it) }
                inbox.close()
                scope.cancel()
            }
        }
    }

    /**
     * 중지 과정의 실패가 다음 명령을 막지 않도록 함.
     * player.stop() 예외는 로그만 남기고, join은 제한시간 후 진행함.
     */
    private suspend fun stopPlayback() {
        val job = playback ?: return
        playback = null
        val wasActive = job.isActive
        job.cancel()
        stopPlayerSafely()          // 블로킹 write를 먼저 깨워 취소 반응을 빠르게 함
        // AudioTrack.write는 취소에 반응하지 않으므로, 깨어나지 않으면 루프 전체가 멈출 수 있음.
        val joined = withTimeoutOrNull(STOP_JOIN_TIMEOUT_MS.milliseconds) { job.join() } != null
        if (!joined) Log.w(TAG, "Playback did not stop within ${STOP_JOIN_TIMEOUT_MS}ms; continuing")
        stopPlayerSafely()          // 안전망: join 사이에 세션이 생겼어도 정리
        if (wasActive) inbox.offer(VoiceEvent.SpeechStopped)
    }

    private fun stopPlayerSafely() {
        try {
            player.stop()
        } catch (e: Exception) {
            Log.w(TAG, "player.stop failed; continuing", e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun play(cmd: Command.Speak) {
        inbox.offer(VoiceEvent.Speaking)
        try {
            val pcm = cmd.tokens
                .buffer(Channel.UNLIMITED)        // LLM 수신이 TTS 속도에 막히지 않도록 분리
                .toSpeechChunks()
                .map(TtsTextSanitizer::clean)
                .filter { it.isNotBlank() }
                .flatMapConcat { synthesize(it, cmd.voice, cmd.language) }   // 순서 보장
                .buffer(PCM_BUFFER_CHUNKS)        // 재생 중 다음 문장 선합성 + 백프레셔
            player.playStream(pcm)
            inbox.offer(VoiceEvent.SpeechDone)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(VoiceEvent.Engine.TTS, e)
        }
    }

    @OptIn(FlowPreview::class)
    private fun synthesize(sentence: String, voice: String, language: String): Flow<ByteArray> = flow {
        var emitted = false
        var attempt = 0
        while (true) {
            try {
                try {
                    client.generateSpeechStream(sentence, voice, language)
                        .timeout(SYNTH_RESPONSE_TIMEOUT_MS.milliseconds)
                        .collect {
                            if (it.isEmpty()) return@collect   // 0바이트 청크는 오디오로 치지 않음
                            emitted = true
                            emit(it)
                        }
                } catch (e: TimeoutCancellationException) {
                    throw SynthesisTimeoutException(SYNTH_RESPONSE_TIMEOUT_MS, e)
                }
                // 응답이 정상 종료됐어도 오디오가 없으면 소리 없이 성공 처리되지 않게 함.
                if (!emitted) throw EmptyAudioException()
                return@flow
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 이미 재생된 오디오가 있으면 같은 문장이 두 번 읽히므로 재시도하지 않음.
                if (emitted || !SynthesisRetryPolicy.isRetryable(e) || attempt >= MAX_SYNTH_RETRY) {
                    throw SynthesisException(e)
                }
                attempt++
                delay(RETRY_DELAY_MS.milliseconds)
            }
        }
    }

    override fun speak(text: String, voiceName: String, language: String) {
        if (text.isBlank()) return
        enqueue(flowOf(text), voiceName, language)
    }

    override fun speakStream(tokens: Flow<String>, voiceName: String, language: String) {
        enqueue(tokens, voiceName, language)
    }

    override fun stop() {
        if (released.get()) return
        commands.trySend(Command.Stop)
    }

    override fun release() {
        if (!released.compareAndSet(false, true)) return
        commands.close()
    }

    private fun enqueue(tokens: Flow<String>, voiceName: String, language: String) {
        if (released.get()) {
            inbox.offer(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "TTS_NOT_READY"))
            return
        }
        // 텍스트와 다른 언어로 합성되지 않도록 대체 언어 없이 실패시킴.
        if (!GeminiSpeakerCatalog.isSupportedLanguage(language)) {
            inbox.offer(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "UNSUPPORTED_LANGUAGE: $language"))
            return
        }
        val voice = voiceName.takeIf { GeminiSpeakerCatalog.isSupported(it) }
            ?: GeminiSpeakerCatalog.defaultSpeaker
        commands.trySend(Command.Speak(tokens, voice, language))
    }
}
