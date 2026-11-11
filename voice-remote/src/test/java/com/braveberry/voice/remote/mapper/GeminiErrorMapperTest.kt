package com.braveberry.voice.remote.mapper

import com.braveberry.voice.remote.model.UnsupportedAudioFormatException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.UnknownException
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** data는 IOException만 재시도하므로, 회복 가능한 Firebase 실패만 IOException이 돼야 함. */
class GeminiErrorMapperTest {

    @Test
    fun transientFailuresBecomeIOException() {
        listOf(
            firebaseError<ServerException>(),
            // SDK가 연결 오류를 감싸서 던지는 실제 형태.
            firebaseError<UnknownException>(SocketTimeoutException())
        ).forEach { original ->
            val mapped = original.toSynthesisError()
            assertTrue(original.javaClass.simpleName, mapped is IOException)
            assertSame(original, mapped.cause)   // 원인 보존
        }
    }

    @Test
    fun ioExceptionPassesThrough() {
        val original = IOException("reset")
        assertSame(original, original.toSynthesisError())
    }

    @Test
    fun permanentFailuresStayNonIOException() {
        listOf(
            firebaseError<InvalidAPIKeyException>(),
            firebaseError<QuotaExceededException>(),
            UnsupportedAudioFormatException("audio/L16;rate=16000"),
            firebaseError<UnknownException>(IllegalStateException()),
            IllegalArgumentException()
        ).forEach { original ->
            val mapped = original.toSynthesisError()
            assertFalse(original.javaClass.simpleName, mapped is IOException)
            assertSame(original, mapped)
        }
    }
}
