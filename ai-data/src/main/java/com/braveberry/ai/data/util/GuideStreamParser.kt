
package com.braveberry.ai.data.parser

import com.braveberry.domain.model.ai.GuideStreamModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val PLACE_PREFIX = "PLACE:"

/**
 * AI 스트리밍 응답에서 첫 줄의 관광지명과 설명 청크를 분리한다.
 *
 * PLACE 헤더가 없는 응답은 줄바꿈을 기다리지 않고
 * 일반 텍스트로 스트리밍한다.
 */
fun Flow<String>.parseGuideStream(): Flow<GuideStreamModel> = flow {
    val buffer = StringBuilder()
    var headerParsed = false

    collect { chunk ->
        if (chunk.isEmpty()) return@collect

        // 헤더 처리 완료 후에는 청크를 그대로 전달
        if (headerParsed) {
            emit(GuideStreamModel(textChunk = chunk))
            return@collect
        }

        buffer.append(chunk)
        val text = buffer.toString()
        val newLineIndex = text.indexOf('\n')

        if (newLineIndex >= 0) {
            val header = text.substring(0, newLineIndex).trim()

            val placeName = if (header.startsWith(PLACE_PREFIX)) {
                header.removePrefix(PLACE_PREFIX)
                    .trim()
                    .takeIf { it.isNotBlank() }
            } else {
                null
            }

            if (placeName != null) {
                emit(GuideStreamModel(placeName = placeName))
            } else {
                // PLACE 형식이 아니면 일반 텍스트로 처리
                emit(
                    GuideStreamModel(
                        textChunk = text.substring(0, newLineIndex + 1)
                    )
                )
            }

            val remaining = text.substring(newLineIndex + 1)
            if (remaining.isNotEmpty()) {
                emit(GuideStreamModel(textChunk = remaining))
            }

            buffer.clear()
            headerParsed = true

        } else if (
            !PLACE_PREFIX.startsWith(text) &&
            !text.startsWith(PLACE_PREFIX)
        ) {
            // PLACE 헤더가 될 수 없다면 즉시 스트리밍
            emit(GuideStreamModel(textChunk = text))
            buffer.clear()
            headerParsed = true
        }
    }

    // 줄바꿈 없이 스트림이 종료된 경우
    if (!headerParsed && buffer.isNotEmpty()) {
        val text = buffer.toString()

        val placeName = if (text.startsWith(PLACE_PREFIX)) {
            text.removePrefix(PLACE_PREFIX)
                .trim()
                .takeIf { it.isNotBlank() }
        } else {
            null
        }

        if (placeName != null) {
            emit(GuideStreamModel(placeName = placeName))
        } else {
            emit(GuideStreamModel(textChunk = text))
        }
    }
}
