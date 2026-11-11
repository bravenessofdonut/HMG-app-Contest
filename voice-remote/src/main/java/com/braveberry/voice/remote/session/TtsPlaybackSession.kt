package com.braveberry.voice.remote.session

import com.braveberry.voice.data.model.TtsPlaybackDataModel
import com.braveberry.voice.remote.api.GeminiTtsApiService
import com.braveberry.voice.remote.mapper.isRetryableGeminiFailure
import com.braveberry.voice.remote.mapper.toTtsDataSourceException
import com.braveberry.voice.remote.model.GeminiAudioFormat
import com.braveberry.voice.remote.model.TtsSpeechRequestDto
import com.braveberry.voice.remote.player.PcmAudioPlayer
import com.braveberry.voice.remote.util.TtsTextSanitizer
import com.braveberry.voice.remote.util.toSpeechChunks
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Remote TTS 재생 세션. 진행 중인 재생 하나를 소유하고, 새 요청이 오면 이전 재생을 대체함.
 * 문장 분할, 문장별 응답 타임아웃, 재시도, 출력까지의 재생 파이프라인을 담당함.
 * 재생은 반환된 Flow를 수집하는 동안 진행됨.
 */
@Singleton
class TtsPlaybackSession internal constructor(
    private val api: GeminiTtsApiService,
    private val player: PcmAudioPlayer,
    dispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(api: GeminiTtsApiService, player: PcmAudioPlayer) :
        this(api, player, Dispatchers.Default)

    private companion object {
        const val TAG = "TtsPlaybackSession"
        const val PCM_BUFFER_CHUNKS = 64      // 재생 중 다음 문장을 미리 합성할 수 있는 한도
        const val MAX_SYNTH_RETRY = 1
        const val RETRY_DELAY_MS = 300L
        // 문장별 Gemini 응답 대기 한도. 첫 오디오 청크까지와 청크 사이 간격에 적용함.
        // 네트워크 타임아웃 임시 값.
        const val SYNTH_RESPONSE_TIMEOUT_MS = 8_000L
        // 재생 교체 시 이전 재생 종료 대기 한도. pause+flush로 정상 해제되면 즉시 끝나는 구간임.
        const val STOP_JOIN_TIMEOUT_MS = 1_000L
    }

    private val logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val lock = Mutex()
    private var active: Deferred<Unit>? = null   // lock 안에서만 접근

    /** voice와 language는 호출 측에서 검증된 값이어야 함. */
    fun speak(
        tokens: Flow<String>,
        voice: String,
        language: String
    ): Flow<TtsPlaybackDataModel> = flow {
        val playback = lock.withLock {
            stopActive()            // 새 요청이 진행 중인 재생을 대체함
            scope.async { play(tokens, voice, language) }.also { active = it }
        }

        var replaced = false
        try {
            emit(TtsPlaybackDataModel.SPEAKING)
            playback.await()
            emit(TtsPlaybackDataModel.DONE)
        } catch (e: CancellationException) {
            // 수집이 취소된 경우는 그대로 전파함. 수집 중인데 재생만 취소됐다면 다른 요청이 대체한 것임.
            if (!currentCoroutineContext().isActive) throw e
            replaced = true
        } finally {
            // 수집이 끝났는데 재생이 남아 있으면(취소, 하위 수집 실패) 함께 멈춤.
            if (!replaced && playback.isActive) {
                withContext(NonCancellable) {
                    lock.withLock { if (active === playback) stopActive() }
                }
            }
        }
        if (replaced) emit(TtsPlaybackDataModel.STOPPED)
    }

    /**
     * lock 안에서만 호출함. 중지 과정의 실패가 다음 요청을 막지 않도록 함.
     * player.stop() 예외는 로그만 남기고, join은 제한시간 후 진행함.
     */
    private suspend fun stopActive() {
        val job = active ?: return
        active = null
        if (!job.isActive) return
        job.cancel()
        stopPlayerSafely()          // 블로킹 write를 먼저 깨워 취소 반응을 빠르게 함
        // AudioTrack.write는 취소에 반응하지 않으므로, 깨어나지 않으면 다음 요청이 멈출 수 있음.
        val joined = withTimeoutOrNull(STOP_JOIN_TIMEOUT_MS.milliseconds) { job.join() } != null
        if (!joined) logger.warning("Playback did not stop within ${STOP_JOIN_TIMEOUT_MS}ms; continuing")
        stopPlayerSafely()          // 안전망: join 사이에 세션이 생겼어도 정리
    }

    private fun stopPlayerSafely() {
        try {
            player.stop()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "player.stop failed; continuing", e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun play(tokens: Flow<String>, voice: String, language: String) {
        val pcm = tokens
            .buffer(Channel.UNLIMITED)        // LLM 수신이 TTS 속도에 막히지 않도록 분리
            .toSpeechChunks()
            .map(TtsTextSanitizer::clean)
            .filter { it.isNotBlank() }
            .flatMapConcat { synthesize(it, voice, language) }   // 순서 보장
            .buffer(PCM_BUFFER_CHUNKS)        // 재생 중 다음 문장 선합성 + 백프레셔
        try {
            player.play(pcm, GeminiAudioFormat.SAMPLE_RATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 세부 원인은 로그로 남기고, 분류는 data 계층이 표준 예외 타입으로 수행함.
            logger.log(Level.WARNING, "Remote TTS failed", e)
            throw e
        }
    }

    @OptIn(FlowPreview::class)
    private fun synthesize(sentence: String, voice: String, language: String): Flow<ByteArray> = flow {
        val request = TtsSpeechRequestDto(text = sentence, voice = voice, language = language)
        var emitted = false
        var attempt = 0
        while (true) {
            try {
                try {
                    api.generateSpeech(request)
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
                val retryable = e is EmptyAudioException || e.isRetryableGeminiFailure()
                // 이미 재생된 오디오가 있으면 같은 문장이 두 번 읽히므로 재시도하지 않음.
                if (emitted || !retryable || attempt >= MAX_SYNTH_RETRY) {
                    throw e.toTtsDataSourceException()
                }
                attempt++
                delay(RETRY_DELAY_MS.milliseconds)
            }
        }
    }
}
