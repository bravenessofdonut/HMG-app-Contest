package com.braveberry.voice.data.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val FENCE = "```"
private const val CODE_NOTICE = "코드는 화면을 참고하세요."

/**
 * LLM 토큰 스트림을 낭독 단위 청크로 변환.
 * - 문장 경계(. ! ? ！ ？ 。 … 줄바꿈)에서 자른다. '.'은 뒤가 공백일 때만 경계로 본다(3.14와 같은 소수점 케이스 고려).
 * - 첫 청크는 짧게(firstMinChars) 내보내 첫 소리 지연을 줄인다.
 * - 이후 청크는 minChars 이상이 되도록 짧은 문장을 병합한다.
 * - 코드 블록(```)은 읽지 않고 안내 문구로 대체한다.
 * - maxChars를 넘는 문장은 쉼표/공백에서 강제로 끊는다.
 */
internal fun Flow<String>.toSpeechChunks(
    firstMinChars: Int = 12,
    minChars: Int = 40,
    maxChars: Int = 300
): Flow<String> = flow {
    val buf = StringBuilder()
    val pending = StringBuilder()
    var inCode = false
    var isFirst = true

    suspend fun flush(force: Boolean) {
        val text = pending.toString().trim()
        if (text.isEmpty()) {
            pending.setLength(0)
            return
        }
        if (force || text.length >= (if (isFirst) firstMinChars else minChars)) {
            pending.setLength(0)
            isFirst = false
            emit(text)
        }
    }

    suspend fun drain(final: Boolean) {
        while (true) {
            if (inCode) {
                val close = buf.indexOf(FENCE)
                if (close < 0) {
                    // 닫는 펜스가 토큰 경계에서 쪼개질 수 있어 마지막 2글자만 보존
                    if (final) buf.setLength(0)
                    else if (buf.length > 2) buf.delete(0, buf.length - 2)
                    return
                }
                buf.delete(0, close + FENCE.length)
                inCode = false
                pending.append(' ').append(CODE_NOTICE).append(' ')
                continue
            }

            val fence = buf.indexOf(FENCE)
            val boundary = findBoundary(buf, final)
            when {
                fence >= 0 && (boundary < 0 || fence < boundary) -> {
                    pending.append(buf, 0, fence)
                    buf.delete(0, fence + FENCE.length)
                    inCode = true
                    flush(force = true)          // 코드 앞 문장은 바로 읽기
                }
                boundary >= 0 -> {
                    pending.append(buf, 0, boundary + 1)
                    buf.delete(0, boundary + 1)
                    flush(force = false)
                }
                buf.length > maxChars -> {
                    val cut = forcedCut(buf, maxChars)
                    pending.append(buf, 0, cut + 1)
                    buf.delete(0, cut + 1)
                    flush(force = true)
                }
                else -> return
            }
        }
    }

    collect { token ->
        buf.append(token)
        drain(final = false)
    }
    drain(final = true)
    if (!inCode) pending.append(buf)
    flush(force = true)
}

private fun findBoundary(buf: CharSequence, final: Boolean): Int {
    for (i in buf.indices) {
        when (buf[i]) {
            // 일본어·중국어 전각 느낌표·물음표(！？) 포함
            '!', '?', '！', '？', '。', '…', '\n' -> return i
            '.' -> {
                val next = buf.getOrNull(i + 1)
                if (next == null) {
                    if (final) return i          // 스트림 끝이면 확정, 아니면 다음 토큰을 기다림
                } else if (next.isWhitespace()) {
                    return i
                }
            }
        }
    }
    return -1
}

private fun forcedCut(buf: CharSequence, max: Int): Int {
    for (i in max - 1 downTo max / 2) {
        val c = buf[i]
        // 일본어 쉼표(、) 포함
        if (c == ',' || c == '，' || c == '、' || c == ' ') return i
    }
    return max - 1
}
