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

class GeminiErrorMapperTest {

    @Test
    fun transientFailuresAreRetried() {
        listOf(
            firebaseError<ServerException>(),
            IOException("reset"),
            // SDK가 연결 오류를 감싸서 던지는 실제 형태.
            firebaseError<UnknownException>(SocketTimeoutException())
        ).forEach { assertTrue(it.javaClass.simpleName, it.isRetryableGeminiFailure()) }
    }

    @Test
    fun permanentFailuresAreNotRetried() {
        listOf(
            firebaseError<InvalidAPIKeyException>(),
            firebaseError<QuotaExceededException>(),
            UnsupportedAudioFormatException("audio/L16;rate=16000"),
            firebaseError<UnknownException>(IllegalStateException()),
            IllegalArgumentException()
        ).forEach { assertFalse(it.javaClass.simpleName, it.isRetryableGeminiFailure()) }
    }

    @Test
    fun authFailureBecomesSecurityException() {
        val original = firebaseError<InvalidAPIKeyException>()
        val mapped = original.toTtsDataSourceException()

        assertTrue(mapped is SecurityException)
        assertSame(original, mapped.cause)   // 원인 보존
    }

    @Test
    fun unsupportedFormatBecomesUnsupportedOperationException() {
        val mapped = UnsupportedAudioFormatException("audio/L16;rate=16000").toTtsDataSourceException()

        assertTrue(mapped is UnsupportedOperationException)
    }

    @Test
    fun otherFailuresBecomeIOException() {
        // Firebase 타입이 data로 새지 않아야 함.
        listOf(
            firebaseError<QuotaExceededException>(),
            firebaseError<ServerException>(),
            IllegalStateException()
        ).forEach { original ->
            val mapped = original.toTtsDataSourceException()
            assertTrue(original.javaClass.simpleName, mapped is IOException)
            assertSame(original, mapped.cause)
        }
    }
}
