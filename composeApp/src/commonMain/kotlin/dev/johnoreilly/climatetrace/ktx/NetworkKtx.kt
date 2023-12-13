package dev.johnoreilly.climatetrace.ktx

import androidx.compose.runtime.MutableState

suspend fun <T> performAsyncOperation(
    isLoadingState: MutableState<Boolean>,
    operation: suspend () -> T,
    onSuccess: (T) -> Unit,
    onError: (() -> Unit)? = null
) {
    isLoadingState.value = true
    try {
        val result = operation()
        onSuccess(result)
    } catch (e: Exception) {
        e.printStackTrace()
        onError?.invoke()
    } finally {
        isLoadingState.value = false
    }
}