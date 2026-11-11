package com.braveberry.voice.data.impl

import android.util.Log
import com.braveberry.domain.model.voice.RemoteTtsOptions
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.domain.repository.RemoteTtsRepository
import com.braveberry.voice.data.datasource.AudioOutputDataSource
import com.braveberry.voice.data.datasource.SpeechSynthesisDataSource
import com.braveberry.voice.data.util.TtsTextSanitizer
import com.braveberry.voice.data.util.toSpeechChunks
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
 * Remote TTS 재생 세션을 운영하는 RemoteTtsRepository 구현체.
 * 명령 처리 순서, 문장 분할, 타임아웃, 재시도를 담당하고 합성·출력은 DataSource에 맡김.
 */
internal class RemoteTtsRepositoryImpl(
    private val synthesizer: SpeechSynthesisDataSource,
    private val output: AudioOutputDataSource,
    dispatcher: CoroutineDispatcher
) : RemoteTtsRepository {

    @Inject
    constructor(synthesizer: SpeechSynthesisDataSource, output: AudioOutputDataSource) :
        this(synthesizer, output, Dispatchers.Default)

    private companion object {
        const val TAG = "RemoteTtsRepository"
        const val PCM_BUFFER_CHUNKS = 64      // 재생 중 다음 문장을 미리 합성할 수 있는 한도
        const val MAX_SYNTH_RETRY = 1
        const val RETRY_DELAY_MS = 300L
        // 문장별 응답 대기 한도. 첫 오디오 청크까지와 청크 사이 간격에 적용함.
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
        speakers = synthesizer.voices,
        defaultSpeaker = synthesizer.defaultVoice,
        languages = synthesizer.languages
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
                runCatching { output.release() }
                    .onFailure { Log.w(TAG, "output.release failed", it) }
                inbox.close()
                scope.cancel()
            }
        }
    }

    /**
     * 중지 과정의 실패가 다음 명령을 막지 않도록 함.
     * output.stop() 예외는 로그만 남기고, join은 제한시간 후 진행함.
     */
    private suspend fun stopPlayback() {
        val job = playback ?: return
        playback = null
        val wasActive = job.isActive
        job.cancel()
        stopOutputSafely()          // 블로킹 출력을 먼저 깨워 취소 반응을 빠르게 함
        // 블로킹 출력은 취소에 반응하지 않을 수 있으므로, 깨어나지 않으면 루프 전체가 멈출 수 있음.
        val joined = withTimeoutOrNull(STOP_JOIN_TIMEOUT_MS.milliseconds) { job.join() } != null
        if (!joined) Log.w(TAG, "Playback did not stop within ${STOP_JOIN_TIMEOUT_MS}ms; continuing")
        stopOutputSafely()          // 안전망: join 사이에 세션이 생겼어도 정리
        if (wasActive) inbox.offer(VoiceEvent.SpeechStopped)
    }

    private fun stopOutputSafely() {
        try {
            output.stop()
        } catch (e: Exception) {
            Log.w(TAG, "output.stop failed; continuing", e)
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
            output.play(pcm, synthesizer.sampleRateHz)
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
                    synthesizer.synthesize(sentence, voice, language)
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
                if (emitted || !e.isRetryableSynthesisFailure() || attempt >= MAX_SYNTH_RETRY) {
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
        if (language !in options.languages) {
            inbox.offer(VoiceEvent.Failed(VoiceEvent.Engine.TTS, "UNSUPPORTED_LANGUAGE: $language"))
            return
        }
        val voice = voiceName.takeIf { it in options.speakers } ?: options.defaultSpeaker
        commands.trySend(Command.Speak(tokens, voice, language))
    }
}
