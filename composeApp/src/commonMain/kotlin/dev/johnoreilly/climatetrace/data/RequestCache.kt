package dev.johnoreilly.climatetrace.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Remembers the result of a request per key for the life of the process, and shares one
// in-flight request between everyone asking for the same key at the same time. Failures are
// not remembered: the next caller tries again.
//
// Requests run in `scope` rather than the caller's, so one caller being cancelled (the user
// moving on to another screen) doesn't fail the request for the others waiting on it.
internal class RequestCache<K, V>(private val scope: CoroutineScope) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<K, Deferred<Result<V>>>()

    suspend fun get(key: K, fetch: suspend () -> V): V {
        val entry = mutex.withLock {
            entries.getOrPut(key) { scope.async { runCatching { fetch() } } }
        }
        val result = entry.await()
        if (result.isFailure) {
            mutex.withLock { if (entries[key] === entry) entries.remove(key) }
        }
        return result.getOrThrow()
    }
}
