package agui

import com.agui.core.types.BaseEvent
import com.agui.core.types.RunErrorEvent
import com.agui.core.types.RunFinishedEvent
import com.agui.core.types.RunStartedEvent
import com.agui.core.types.TextMessageContentEvent
import com.agui.core.types.TextMessageEndEvent
import com.agui.core.types.TextMessageStartEvent
import com.agui.core.types.ToolCallArgsEvent
import com.agui.core.types.ToolCallEndEvent
import com.agui.core.types.ToolCallResultEvent
import com.agui.core.types.ToolCallStartEvent
import com.google.adk.kt.events.Event as AdkEvent
import com.google.adk.kt.types.FunctionCall
import com.google.adk.kt.types.FunctionResponse
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Translates the ADK event stream into the AG-UI protocol event stream.
 *
 * The two models line up closely — both are a flat stream of deltas over one agent run — but they
 * differ in three ways that this mapping has to reconcile:
 *
 *  1. AG-UI brackets a run with `RUN_STARTED` / `RUN_FINISHED`; ADK has no equivalent, so the
 *     brackets are added here.
 *  2. AG-UI text is an explicitly opened and closed message (`TEXT_MESSAGE_START` → `CONTENT`* →
 *     `END`), while ADK just emits parts. A message is opened lazily on the first text part and
 *     closed when the turn moves on. This matters: the client's `EventVerifier` rejects
 *     `RUN_FINISHED` while any message or tool call is still open.
 *  3. When streaming, ADK emits partial events carrying deltas and then a final non-partial event
 *     repeating the whole text. Forwarding both would duplicate every answer in the UI, so the
 *     aggregate is used only to close the message. See [emitText].
 */
fun Flow<AdkEvent>.asAgUiEvents(threadId: String, runId: String): Flow<BaseEvent> = flow {
    emit(RunStartedEvent(threadId = threadId, runId = runId))

    val translator = AgUiTranslator(this)
    try {
        this@asAgUiEvents.collect { adkEvent ->
            // RUN_ERROR is terminal in AG-UI: nothing may follow it. The upstream flow is still
            // drained so the runner can finish and release the session.
            if (!translator.errored) translator.translate(adkEvent)
        }
    } catch (e: Exception) {
        translator.closeOpenMessage()
        emit(RunErrorEvent(message = e.message ?: e::class.simpleName ?: "Agent run failed"))
        translator.errored = true
    }

    if (!translator.errored) {
        translator.closeOpenMessage()
        emit(RunFinishedEvent(threadId = threadId, runId = runId))
    }
}

private class AgUiTranslator(private val out: FlowCollector<BaseEvent>) {
    var errored = false

    private var openMessageId: String? = null
    private var streamedDelta = false
    private val emittedToolCallIds = mutableSetOf<String>()

    suspend fun translate(event: AdkEvent) {
        if (event.errorCode != null || event.errorMessage != null) {
            closeOpenMessage()
            out.emit(
                RunErrorEvent(
                    message = event.errorMessage ?: "Agent run failed",
                    code = event.errorCode,
                )
            )
            errored = true
            return
        }

        for (part in event.content?.parts.orEmpty()) {
            val text = part.text
            val functionCall = part.functionCall
            val functionResponse = part.functionResponse
            when {
                // Thought parts are the model's internal reasoning. AG-UI models these with its
                // REASONING_* events; for now only the user-visible stream is forwarded.
                part.thought == true -> Unit
                text != null -> emitText(text, partial = event.partial)
                functionCall != null -> emitToolCall(functionCall)
                functionResponse != null -> emitToolResult(functionResponse)
            }
        }
    }

    private suspend fun emitText(text: String, partial: Boolean) {
        when {
            partial -> {
                if (text.isEmpty()) return // TextMessageContentEvent rejects an empty delta.
                val messageId = openMessageId ?: newId().also {
                    openMessageId = it
                    out.emit(TextMessageStartEvent(messageId = it))
                }
                out.emit(TextMessageContentEvent(messageId = messageId, delta = text))
                streamedDelta = true
            }
            // The non-partial event that follows a stream of deltas repeats the full text, so it
            // only marks the end of the message.
            streamedDelta -> closeOpenMessage()
            // Non-streaming turn: the whole message arrives in one event.
            text.isNotEmpty() -> {
                val messageId = newId()
                out.emit(TextMessageStartEvent(messageId = messageId))
                out.emit(TextMessageContentEvent(messageId = messageId, delta = text))
                out.emit(TextMessageEndEvent(messageId = messageId))
            }
        }
    }

    private suspend fun emitToolCall(call: FunctionCall) {
        val toolCallId = call.id ?: newId()
        // ADK surfaces the same function call twice while streaming — once on a partial event and
        // again on the aggregate that follows — so forwarding both would show the tool call twice
        // in the UI. Ids are unique per call, so the second sighting is always a repeat.
        if (!emittedToolCallIds.add(toolCallId)) return

        closeOpenMessage() // A tool call ends the assistant message that introduced it.
        out.emit(ToolCallStartEvent(toolCallId = toolCallId, toolCallName = call.name))
        // ADK hands over fully-formed arguments, so the whole JSON object goes in one ARGS event
        // rather than being streamed token by token as an LLM would produce it.
        out.emit(ToolCallArgsEvent(toolCallId = toolCallId, delta = call.args.toJsonObject().toString()))
        out.emit(ToolCallEndEvent(toolCallId = toolCallId))
    }

    private suspend fun emitToolResult(response: FunctionResponse) {
        // AG-UI ties every result to the tool call it answers; a response without an id can't be
        // attributed, so it is dropped rather than invented.
        val toolCallId = response.id ?: return
        out.emit(
            ToolCallResultEvent(
                messageId = newId(),
                toolCallId = toolCallId,
                content = response.response.toJsonObject().toString(),
                role = "tool",
            )
        )
    }

    suspend fun closeOpenMessage() {
        openMessageId?.let { out.emit(TextMessageEndEvent(messageId = it)) }
        openMessageId = null
        streamedDelta = false
    }

    private fun newId() = UUID.randomUUID().toString()
}

/**
 * ADK models tool arguments and responses as `Map<String, Any?>`, which kotlinx.serialization can
 * only encode through ADK's own contextual serializer. Converting by hand keeps this module off
 * that framework-internal API.
 */
private fun Map<String, Any?>.toJsonObject(): JsonObject =
    JsonObject(mapValues { (_, value) -> value.toJsonElement() })

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}
