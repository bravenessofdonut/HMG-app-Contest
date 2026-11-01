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
    @Test fun onlySuccessStartsTheNextOperation() = runTest {
        val cause = ResourceFailure.ServiceUnavailable(java.io.IOException("offline"))
        val calls = mutableListOf<Int>()
        val results = flowOf<DataResource<Int>>(
            DataResource.loading(), DataResource.loading(1), DataResource.success(2), DataResource.error(cause)
        ).flatMapDataResource { value ->
            calls += value
            flowOf(DataResource.success(value.toString()))
        }.toList()

        assertEquals(listOf(2), calls)
        assertEquals(4, results.size)
        assertTrue(results[0] is DataResource.Loading && results[1] is DataResource.Loading)
        assertEquals("2", (results[2] as DataResource.Success).data)
        assertEquals(cause, (results[3] as DataResource.Error).failure)
    }

    @Test fun collectingDoesNotReportCancellationAsFailure() = runTest {
        val cancel = CancellationException("stop")
        try {
            flow<DataResource<Int>> { throw cancel }.collectDataResource(
                onSuccess = { throw AssertionError("Unexpected value") },
                onError = { throw AssertionError("Unexpected failure", it) }
            )
            throw AssertionError("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals(cancel, expected)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun programmingErrorEscapesTheResourceBoundary() = runTest {
        flow<Int> { throw IllegalArgumentException("invalid mapping") }.asDataResourceFlow().toList()
        Unit
    }

    @Test fun wrapsSuccessWithLoading() = runTest {
        val events = flowOf(2, 3).asDataResourceFlow().toList()
        assertTrue(events.first() is DataResource.Loading)
        assertEquals(2, (events[1] as DataResource.Success).data)
        assertEquals(3, (events[2] as DataResource.Success).data)
    }

    @Test fun translatesClassifiedServiceFailureToError() = runTest {
        val events = flow<Int> { throw ResourceFailure.ServiceUnavailable(java.io.IOException("offline")) }.asDataResourceFlow().toList()
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
