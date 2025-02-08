package androidx.navigation3

import androidx.compose.runtime.Composable

/**
 * Class that wraps a [NavEntry] within another [NavEntry].
 *
 * This provides a nesting mechanism for [NavEntry]s that allows properly nested content.
 *
 * @param navEntry the [NavEntry] to wrap
 */
public open class NavEntryWrapper<T : Any>(public val navEntry: NavEntry<T>) :
    NavEntry<T>(navEntry.key, navEntry.featureMap, navEntry.content) {
    override val key: T
        get() = navEntry.key

    override val featureMap: Map<String, Any>
        get() = navEntry.featureMap

    override val content: @Composable (T) -> Unit
        get() = navEntry.content
}
