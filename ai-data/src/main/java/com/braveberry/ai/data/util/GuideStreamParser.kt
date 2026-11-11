package com.braveberry.ai.data.parser

import com.braveberry.domain.model.ai.GuideStreamModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun Flow<String>.parseGuideStream(): Flow<GuideStreamModel> = flow {
    val buffer = StringBuilder()
    var parsed = false

    collect { chunk ->
        if (parsed) {
            if (chunk.isNotEmpty()) {
                emit(GuideStreamModel(textChunk = chunk))
            }
        } else {
            buffer.append(chunk)

            val index = buffer.indexOf('\n')

            if (index >= 0) {
                val header = buffer.substring(0, index).trim()
                val placeName = header
                    .takeIf { it.startsWith("PLACE:") }
                    ?.removePrefix("PLACE:")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

                if (placeName != null) {
                    emit(GuideStreamModel(placeName = placeName))
                } else {
                    emit(
                        GuideStreamModel(
                            textChunk = buffer.substring(0, index + 1)
                        )
                    )
                }

                parsed = true

                val remaining = buffer.substring(index + 1)
                if (remaining.isNotEmpty()) {
                    emit(GuideStreamModel(textChunk = remaining))
                }

                buffer.clear()
            }
        }
    }

    if (!parsed && buffer.isNotEmpty()) {
        emit(GuideStreamModel(textChunk = buffer.toString()))
    }
}