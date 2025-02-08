package androidx.navigation3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.ListDetailNavDisplay.IS_SUPPORTING_PANE
import androidx.window.core.layout.WindowWidthSizeClass

@Composable
fun <T : Any> ListDetailNavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    localProviders: List<NavLocalProvider> = emptyList(),
    onBack: () -> Unit = { if (backStack is MutableList) backStack.removeAt(backStack.size - 1) },
    windowWidthSizeClass: WindowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass,
    entryProvider: (key: T) -> NavEntry<T>
) {

    val isSinglePaneLayout = (windowWidthSizeClass == WindowWidthSizeClass.COMPACT)
    //BackHandler(isSinglePaneLayout && backStack.size > 1, onBack)

    NavBackStackProvider(backStack, entryProvider, localProviders) { entries ->
        val lastEntry = entries.last()
        if (isSinglePaneLayout){
            Box(modifier = modifier) {
                lastEntry.content.invoke(lastEntry.key)
            }
        } else {
            Row {

                var rightEntry : NavEntry<T>? = null
                val leftEntry : NavEntry<T>?

                val isSupportingPane =
                    lastEntry.featureMap[IS_SUPPORTING_PANE]?.equals(true) ?: false

                if (isSupportingPane){
                    // Display the penultimate entry in the left pane
                    leftEntry = entries[entries.size - 2]

                    // Display the last entry in the right pane
                    rightEntry = lastEntry
                } else {
                    // Display the last entry in the left pane
                    leftEntry = lastEntry
                }

                // Left pane
                Box(modifier = modifier.fillMaxWidth(0.5F)) {
                    leftEntry.content.invoke(leftEntry.key)
                }

                // Right pane
                Box(modifier = modifier.fillMaxWidth()) {

                    if (rightEntry == null){
                        Text("Placeholder")
                    } else {
                        rightEntry.content.invoke(rightEntry.key)
                    }
                }
            }
        }
    }
}

object ListDetailNavDisplay {
    internal const val IS_SUPPORTING_PANE = "isSupportingPane"
    fun isSupportingPane(value: Boolean) = mapOf(IS_SUPPORTING_PANE to value)
}

