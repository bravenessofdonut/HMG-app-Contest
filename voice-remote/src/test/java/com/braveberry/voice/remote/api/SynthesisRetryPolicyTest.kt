package com.braveberry.voice.remote.api

import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.UnknownException
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SynthesisRetryPolicyTest {

    @Test
    fun transientFailuresAreRetried() {
        listOf(
            SynthesisTimeoutException(8_000, RuntimeException()),
            firebaseError<ServerException>(),
            EmptyAudioException(),
            IOException("reset"),
            // SDK가 연결 오류를 감싸서 던지는 실제 형태.
            firebaseError<UnknownException>(SocketTimeoutException())
        ).forEach { assertTrue(it.javaClass.simpleName, SynthesisRetryPolicy.isRetryable(it)) }
    }

    @Test
    fun permanentFailuresAreNotRetried() {
        listOf(
            firebaseError<InvalidAPIKeyException>(),
            firebaseError<QuotaExceededException>(),
            UnsupportedAudioFormatException("audio/L16;rate=16000"),
            firebaseError<UnknownException>(IllegalStateException()),
            IllegalArgumentException()
        ).forEach { assertFalse(it.javaClass.simpleName, SynthesisRetryPolicy.isRetryable(it)) }
    }
}
