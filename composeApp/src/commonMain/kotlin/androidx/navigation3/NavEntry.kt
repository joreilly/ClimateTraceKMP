package androidx.navigation3

import androidx.compose.runtime.Composable

public open class NavEntry<T : Any>(
    public open val key: T,
    public open val featureMap: Map<String, Any> = emptyMap(),
    public open val content: @Composable (T) -> Unit
)
