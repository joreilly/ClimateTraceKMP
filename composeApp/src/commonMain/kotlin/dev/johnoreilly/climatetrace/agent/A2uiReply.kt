package dev.johnoreilly.climatetrace.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/**
 * The LLM writes [A2UI](https://a2ui.org) messages in an ```a2ui block in its final reply. This pulls
 * the block out and sends its messages to the renderer, returning the rest of the reply as the text
 * to show in the chat. Any errors come back on [A2uiRenderer.events] and are sent to the agent.
 */
object A2uiReply {
    private val blockRegex = Regex("```a2ui\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)

    fun render(reply: String, renderer: A2uiRenderer): String {
        blockRegex.findAll(reply).forEach { renderBlock(it.groupValues[1].trim(), renderer) }
        return blockRegex.replace(reply, "").trim()
    }

    private fun renderBlock(block: String, renderer: A2uiRenderer) {
        val messages = try {
            Json.parseToJsonElement(block) as? JsonArray
        } catch (e: Exception) {
            null
        }
        if (messages == null) {
            renderer.reportError("the a2ui block is not a valid JSON array of A2UI messages")
            return
        }
        messages.forEach { renderer.process(it.toString()) }
    }
}

/** System prompt addition telling the LLM when and how to show UI. */
fun a2uiInstructions(renderer: A2uiRenderer) =
    """
    The user's device can display native UI. When an answer contains data that suits a visual layout
    (comparisons, rankings, several countries or years), include the UI in your final reply as A2UI
    protocol messages in a single fenced code block marked a2ui, followed by a one or two sentence text
    summary rather than repeating the data. For example:

    ```a2ui
    [{"version":"v0.9","createSurface":{"surfaceId":"<id>","catalogId":"${renderer.catalogId}"}},
     {"version":"v0.9","updateComponents":{"surfaceId":"<id>","components":[...]}}]
    ```

    The block is a JSON array of messages. Send createSurface only for a new surface (use a new
    surfaceId for each new piece of UI). To change UI that's already shown, send only
    updateComponents with its surfaceId.
    Components are a flat list; each has a unique "id" and a "component" type name plus that
    component's properties. Containers reference their children by id. Exactly one component must
    have the id "root" - it is the top of the layout.
    Give text properties as plain literal strings with numbers already formatted - don't use
    ${'$'}{...} expressions or function calls in them.
    A Button has a "child" (the id of a Text component for its label) and an "action" of the form
    {"event":{"name":"<action name>","context":{"<key>":"<literal value>"}}}.
    When the user taps a button, you'll receive a message starting "UI action:" with the event
    name and context; respond to it as a new request.
    For a Row of Cards (or other boxes that should line up) set "align": "stretch" on the Row so
    they're all the same height.
    To compare values (e.g. emissions of several countries or years) use an EmissionsChart, and
    show a CountryFlag next to country names in titles; both are described in the schema below.
    If you receive a message starting "A2UI error:", reply again with a corrected a2ui block.

    The available components and their properties are defined by this JSON Schema:
    """.trimIndent() + "\n" + renderer.catalogSchema
