package androidx.navigation3

import androidx.compose.runtime.Composable

/**
 * Interface that offers the ability to provide information to some Composable content that is
 * integrated with a [NavDisplay](reference/androidx/navigation/NavDisplay).
 *
 * Information can be provided to the entire back stack via [NavLocalProvider.ProvideToBackStack] or
 * to a single entry via [NavLocalProvider.ProvideToEntry].
 */
public interface NavLocalProvider {

    /**
     * Allows a [NavLocalProvider] to provide to the entire backstack.
     *
     * This function is called by the [NavWrapperManager] and should not be called directly.
     */
    @Composable
    public fun ProvideToBackStack(backStack: List<Any>, content: @Composable () -> Unit): Unit =
        content.invoke()

    /**
     * Allows a [NavLocalProvider] to provide information to a single entry.
     *
     * This function is called by the [NavDisplay](reference/androidx/navigation/NavDisplay) and
     * should not be called directly.
     */
    @Composable public fun <T : Any> ProvideToEntry(entry: NavEntry<T>)
}
