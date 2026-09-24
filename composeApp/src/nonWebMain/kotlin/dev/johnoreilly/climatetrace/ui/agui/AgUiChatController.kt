package dev.johnoreilly.climatetrace.ui.agui

import com.agui.client.AgUiAgent
import com.agui.core.types.RunErrorEvent
import com.agui.core.types.TextMessageContentEvent
import com.agui.core.types.ToolCallArgsEvent
import com.agui.core.types.ToolCallResultEvent
import com.agui.core.types.ToolCallStartEvent
import dev.johnoreilly.climatetrace.viewmodel.AgentDemoUiState
import dev.johnoreilly.climatetrace.viewmodel.Message
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the shared agent chat UI from an AG-UI endpoint, translating protocol events into the
 * [Message] model [dev.johnoreilly.climatetrace.ui.AgentScreenContent] already renders.
 *
 * Note this uses the plain [AgUiAgent] rather than `StatefulAgUiAgent`: the server maps an AG-UI
 * `threadId` onto an ADK session, so the conversation history lives there and each turn only needs
 * to carry the new message. Replaying client-side history as well would just duplicate it.
 */
internal class AgUiChatController(
    private val url: String,
    private val scope: CoroutineScope,
) {
    private val agent = AgUiAgent(url)

    /** One thread for the life of the screen; [restart] starts a fresh one. */
    private var threadId = newThreadId()

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<AgentDemoUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty() || _uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                messages = it.messages + Message.UserMessage(prompt),
                inputText = "",
                isInputEnabled = false,
                isLoading = true,
            )
        }
        scope.launch { runAgent(prompt) }
    }

    fun restart() {
        threadId = newThreadId()
        _uiState.value = initialState()
    }

    fun close() = agent.close()

    private suspend fun runAgent(prompt: String) {
        try {
            agent.sendMessage(message = prompt, threadId = threadId).collect { event ->
                when (event) {
                    // Deltas are appended to the assistant bubble as they arrive, so the answer
                    // types itself out rather than appearing all at once. TEXT_MESSAGE_START/END
                    // need no handling — the bubble is opened by the first delta.
                    is TextMessageContentEvent -> appendAssistantDelta(event.delta)
                    is ToolCallStartEvent -> append(Message.ToolCallMessage(event.toolCallName))
                    is ToolCallArgsEvent -> appendToolCallArgs(event.delta)
                    is ToolCallResultEvent -> append(Message.ResultMessage(event.content.truncate()))
                    is RunErrorEvent -> append(Message.ErrorMessage(event.message))
                    else -> Unit
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Most often the server isn't running, so name the endpoint rather than just the error.
            append(Message.ErrorMessage("Could not reach $url: ${e.message ?: e::class.simpleName}"))
        } finally {
            _uiState.update { it.copy(isLoading = false, isInputEnabled = true) }
        }
    }

    private fun append(message: Message) =
        _uiState.update { it.copy(messages = it.messages + message) }

    private fun appendAssistantDelta(delta: String) = _uiState.update { state ->
        val last = state.messages.lastOrNull()
        val messages = if (last is Message.AgentMessage) {
            state.messages.dropLast(1) + Message.AgentMessage(last.text + delta)
        } else {
            state.messages + Message.AgentMessage(delta)
        }
        state.copy(messages = messages)
    }

    private fun appendToolCallArgs(delta: String) = _uiState.update { state ->
        val last = state.messages.lastOrNull()
        if (last !is Message.ToolCallMessage) return@update state
        state.copy(
            messages = state.messages.dropLast(1) +
                Message.ToolCallMessage("${last.text} ${delta.truncate()}")
        )
    }

    private fun initialState() = AgentDemoUiState(
        messages = listOf(Message.SystemMessage("Connected to the ADK agent over AG-UI at $url")),
        inputText = "Get emissions for Germany for 2025",
    )

    private fun newThreadId() = "climatetrace-${Random.nextLong().toULong().toString(16)}"
}

private fun String.truncate(max: Int = 200) = if (length <= max) this else take(max) + "…"
