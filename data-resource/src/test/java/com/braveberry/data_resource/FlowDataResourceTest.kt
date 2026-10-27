package com.braveberry.data_resource

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowDataResourceTest {
    @Test fun wrapsSuccessWithLoading() = runTest {
        val events = flowOf(2, 3).asDataResourceFlow().toList()
        assertTrue(events.first() is DataResource.Loading)
        assertEquals(2, (events[1] as DataResource.Success).data)
        assertEquals(3, (events[2] as DataResource.Success).data)
    }

    @Test fun translatesOrdinaryUpstreamExceptionToError() = runTest {
        val events = flow<Int> { throw IllegalStateException("offline") }.asDataResourceFlow().toList()
        assertTrue(events.last() is DataResource.Error)
    }

    @Test fun doesNotConvertCancellationToRecoverableError() = runTest {
        try {
            flow<Int> { throw CancellationException("cancel") }.asDataResourceFlow().toList()
            throw AssertionError("Cancellation should propagate")
        } catch (_: CancellationException) {
            // cooperative cancellation
        }
    }
}
