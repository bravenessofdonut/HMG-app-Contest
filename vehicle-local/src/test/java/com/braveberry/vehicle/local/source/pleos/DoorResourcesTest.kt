package com.braveberry.vehicle.local.source.pleos

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DoorResourcesTest {
    @Test fun failureAfterRegistrationReleasesInReverseOrder() = runTest {
        val calls = mutableListOf<String>()
        val result = runCatching {
            withDoorResources({ calls += "release" }) { registered ->
                registered { calls += "unregister" }
                throw IOException("initial read")
            }
        }
        assertEquals("initial read", result.exceptionOrNull()?.message)
        assertEquals(listOf("unregister", "release"), calls)
    }

    @Test fun cancelledSubscriptionReleasesExactlyOnce() = runTest {
        val calls = mutableListOf<String>()
        val ready = CompletableDeferred<Unit>()
        val job = launch {
            withDoorResources({ calls += "release" }) { registered ->
                registered { calls += "unregister" }
                ready.complete(Unit)
                awaitCancellation()
            }
        }
        ready.await()
        job.cancelAndJoin()
        assertEquals(listOf("unregister", "release"), calls)
    }
}
