package dev.johnoreilly.climatetrace.agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Renders agent-generated UI described with the [A2UI protocol](https://a2ui.org).
 *
 * The renderer (`androidx.a2ui`) is Android-only, so the other platforms bind
 * [UnsupportedA2uiRenderer] in their Koin `dataModule()`: [isSupported] is false and the agent isn't
 * told it can show UI there, so it answers in plain text.
 */
interface A2uiRenderer {
    val isSupported: Boolean

    /** Id of the component catalog the client renders, used in `createSurface`. */
    val catalogId: String

    /** JSON Schema of that catalog, given to the LLM so it knows which components exist. */
    val catalogSchema: String

    /** Ids of the surfaces currently shown, in creation order. */
    val surfaceIds: StateFlow<List<String>>

    /**
     * User actions (e.g. button taps) and client-side errors, both of which are sent back to the
     * agent (as A2UI intends: they're the client-to-server messages).
     */
    val events: Flow<A2uiEvent>

    /** Queues a single A2UI message (JSON). Any error in it is reported on [events]. */
    fun process(messageJson: String)

    /** Reports an error that isn't in a single message (e.g. an a2ui block that isn't valid JSON). */
    fun reportError(message: String)

    /** Draws the surface with the given id. */
    @Composable
    fun Surface(surfaceId: String, modifier: Modifier)
}

sealed interface A2uiEvent {
    val description: String

    data class UserAction(override val description: String) : A2uiEvent
    data class Error(override val description: String) : A2uiEvent
}

object UnsupportedA2uiRenderer : A2uiRenderer {
    override val isSupported = false
    override val catalogId = ""
    override val catalogSchema = ""
    override val surfaceIds: StateFlow<List<String>> = MutableStateFlow(emptyList())
    override val events: Flow<A2uiEvent> = emptyFlow()
    override fun process(messageJson: String) = Unit
    override fun reportError(message: String) = Unit

    // Never called: this renderer creates no surfaces.
    @Composable
    override fun Surface(surfaceId: String, modifier: Modifier) = Unit
}
