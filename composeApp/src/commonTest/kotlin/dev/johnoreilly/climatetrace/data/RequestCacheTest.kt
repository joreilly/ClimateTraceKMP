package dev.johnoreilly.climatetrace.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestCacheTest {
    @Test
    fun repeatedRequestsForAKeyFetchOnce() = runTest {
        val cache = RequestCache<String, Int>(backgroundScope)
        var fetches = 0

        assertEquals(1, cache.get("2025") { ++fetches })
        assertEquals(1, cache.get("2025") { ++fetches })
        assertEquals(2, cache.get("2024") { ++fetches })
        assertEquals(2, fetches)
    }

    @Test
    fun concurrentRequestsForAKeyShareOneFetch() = runTest {
        val cache = RequestCache<String, Int>(backgroundScope)
        val gate = CompletableDeferred<Unit>()
        var fetches = 0

        val callers = List(5) { async { cache.get("2025") { fetches++; gate.await(); 42 } } }
        testScheduler.runCurrent()
        gate.complete(Unit)

        assertEquals(List(5) { 42 }, callers.awaitAll())
        assertEquals(1, fetches)
    }

    @Test
    fun failuresAreNotCached() = runTest {
        val cache = RequestCache<String, Int>(backgroundScope)

        assertFailsWith<IllegalStateException> { cache.get("2025") { error("offline") } }
        assertEquals(7, cache.get("2025") { 7 })
    }

    @Test
    fun cancelledCallerDoesNotFailTheOthers() = runTest {
        val cache = RequestCache<String, Int>(backgroundScope)
        val gate = CompletableDeferred<Unit>()

        val impatient = launch { cache.get("2025") { gate.await(); 42 } }
        val patient = async { cache.get("2025") { error("should share the first fetch") } }
        testScheduler.runCurrent()
        impatient.cancel()
        gate.complete(Unit)

        assertEquals(42, patient.await())
    }
}
