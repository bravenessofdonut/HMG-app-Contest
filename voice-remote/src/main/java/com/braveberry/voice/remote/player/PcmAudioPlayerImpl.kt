package com.braveberry.voice.remote.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/** AudioTrack으로 PCM 16-bit mono를 출력함. */
class PcmAudioPlayerImpl @Inject constructor() : PcmAudioPlayer {

    private class Session(val track: AudioTrack, val sampleRateHz: Int) {
        @Volatile var stopped = false
    }

    @Volatile private var current: Session? = null

    private companion object {
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_FRAME = 2                 // 16bit mono
        const val DRAIN_POLL_MS = 20L
        const val DRAIN_MARGIN_MS = 500L
    }

    override suspend fun play(pcm: Flow<ByteArray>, sampleRateHz: Int) {
        withContext(Dispatchers.IO) {
            stop()                                    // 방어: 이전 세션 정리
            val session = Session(createTrack(sampleRateHz), sampleRateHz)
            current = session
            val track = session.track

            var framesWritten = 0L
            var carry: Byte? = null                   // 청크 경계에서 남은 홀수 바이트

            try {
                track.play()
                pcm.collect { chunk ->
                    this@withContext.ensureActive()
                    val data = carry?.let { byteArrayOf(it) + chunk } ?: chunk
                    val usable = data.size and 1.inv()
                    carry = if (usable < data.size) data[usable] else null
                    if (usable > 0) {
                        writeFully(session, data, usable)
                        framesWritten += usable / BYTES_PER_FRAME
                    }
                }
                if (!session.stopped) awaitDrained(session, framesWritten)
            } finally {
                if (current === session) current = null
                session.stopped = true
                runCatching { track.pause(); track.flush() }   // 남은 소리 즉시 중단
                runCatching { track.release() }
            }
        }
    }

    /** 블로킹 write. 오류는 예외로, 중단 요청은 취소로 전환한다. */
    private fun writeFully(session: Session, data: ByteArray, length: Int) {
        var off = 0
        while (off < length) {
            if (session.stopped) throw CancellationException("PCM playback stopped")
            val n = session.track.write(data, off, length - off)
            if (n < 0) {
                if (session.stopped) throw CancellationException("PCM playback stopped")
                throw IOException("AudioTrack.write failed: $n")
            }
            off += n
        }
    }

    /** 쓴 프레임을 재생 헤드가 모두 지나갈 때까지 대기한다. */
    private suspend fun awaitDrained(session: Session, totalFrames: Long) {
        if (totalFrames <= 0) return
        val remainingFrames = totalFrames - playedFrames(session.track)
        val timeoutMs = remainingFrames.coerceAtLeast(0) * 1000 / session.sampleRateHz + DRAIN_MARGIN_MS
        withTimeoutOrNull(timeoutMs.milliseconds) {
            while (!session.stopped && playedFrames(session.track) < totalFrames) {
                delay(DRAIN_POLL_MS.milliseconds)
            }
        }
    }

    private fun playedFrames(track: AudioTrack): Long =
        runCatching { track.playbackHeadPosition.toLong() and 0xFFFFFFFFL }
            .getOrDefault(Long.MAX_VALUE)             // 이미 release됐으면 종료로 간주

    private fun createTrack(sampleRateHz: Int): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(sampleRateHz, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBuffer, sampleRateHz * BYTES_PER_FRAME)   // 약 1초
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /** 블로킹 write를 깨우기 위해 pause+flush만 한다. release는 재생 코루틴이 담당. */
    override fun stop() {
        val s = current ?: return
        s.stopped = true
        runCatching { s.track.pause(); s.track.flush() }
    }
}
