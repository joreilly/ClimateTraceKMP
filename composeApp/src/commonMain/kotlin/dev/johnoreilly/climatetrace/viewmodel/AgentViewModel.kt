package dev.johnoreilly.climatetrace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.johnoreilly.climatetrace.agent.A2uiEvent
import dev.johnoreilly.climatetrace.agent.A2uiRenderer
import dev.johnoreilly.climatetrace.agent.AgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Define message types for the chat
sealed class Message {
    data class UserMessage(val text: String) : Message()
    data class AgentMessage(val text: String) : Message()
    data class SystemMessage(val text: String) : Message()
    data class ErrorMessage(val text: String) : Message()
    data class ToolCallMessage(val text: String) : Message()
    data class ResultMessage(val text: String) : Message()
    /** Native UI the agent rendered with A2UI (Android only). */
    data class UiMessage(val surfaceId: String) : Message()
}

// Define UI state for the agent demo screen
data class AgentDemoUiState(
    val messages: List<Message> = listOf(Message.SystemMessage("Hi, I'm an agent that can help you")),
    val inputText: String = "Get emissions for Germany for 2025",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isChatEnded: Boolean = false,

    // For handling user responses when agent asks a question
    val userResponseRequested: Boolean = false,
    val currentUserResponse: String? = null,
)

class AgentViewModel(
    private val agentProvider: AgentProvider,
    val a2uiRenderer: A2uiRenderer,
) : ViewModel() {
    // UI state
    private val _uiState = MutableStateFlow(
        AgentDemoUiState(
            messages = listOf(Message.SystemMessage(agentProvider.description))
        )
    )
    val uiState: StateFlow<AgentDemoUiState> = _uiState.asStateFlow()

    init {
        // Show each surface the agent creates as a message in the chat.
        viewModelScope.launch {
            val shown = mutableSetOf<String>()
            a2uiRenderer.surfaceIds.collect { ids ->
                val newIds = ids.filter { shown.add(it) }
                if (newIds.isNotEmpty()) {
                    _uiState.update { state ->
                        val uiMessages = newIds.map { id -> Message.UiMessage(id) }
                        // The reply's text is shown before the surface it describes has been created
                        // (the a2ui block is processed asynchronously), so keep the UI above that summary.
                        val last = state.messages.lastOrNull()
                        val messages = if (last is Message.AgentMessage) {
                            state.messages.dropLast(1) + uiMessages + last
                        } else {
                            state.messages + uiMessages
                        }
                        state.copy(messages = messages)
                    }
                }
            }
        }

        // A tap on agent-rendered UI is sent to the agent as the user's next reply.
        viewModelScope.launch {
            a2uiRenderer.events.collect { event ->
                when (event) {
                    is A2uiEvent.UserAction -> if (_uiState.value.userResponseRequested) {
                        a2uiCorrections.value = 0
                        _uiState.update {
                            it.copy(
                                messages = it.messages + Message.SystemMessage("UI action: ${event.description}"),
                                isLoading = true,
                                userResponseRequested = false,
                                currentUserResponse = "UI action: ${event.description}"
                            )
                        }
                    }
                    is A2uiEvent.Error -> {
                        _uiState.update { it.copy(messages = it.messages + Message.ErrorMessage(event.description)) }
                        // Errors can arrive while the agent is still working on its reply, so they're
                        // queued and sent as its next input once it's waiting for one.
                        pendingA2uiErrors.update { it + event.description }
                        if (_uiState.value.userResponseRequested) sendPendingA2uiErrors()
                    }
                }
            }
        }
    }

    private val pendingA2uiErrors = MutableStateFlow<List<String>>(emptyList())
    private val a2uiCorrections = MutableStateFlow(0)

    /**
     * Sends any A2UI errors to the agent (as its next input) so it can correct the UI. Capped so a
     * model that keeps getting it wrong doesn't loop forever; the errors are still shown in the chat.
     */
    private fun sendPendingA2uiErrors() {
        val errors = pendingA2uiErrors.getAndUpdate { emptyList() }
        if (errors.isEmpty() || a2uiCorrections.value >= MAX_A2UI_CORRECTIONS) return
        a2uiCorrections.update { it + 1 }
        _uiState.update {
            it.copy(
                isLoading = true,
                userResponseRequested = false,
                currentUserResponse = "A2UI error: ${errors.joinToString("; ")}"
            )
        }
    }

    // Update input text
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    // Send user message and start agent processing
    fun sendMessage() {
        val userInput = _uiState.value.inputText.trim()
        if (userInput.isEmpty()) return
        a2uiCorrections.value = 0

        // If agent is waiting for a response to a question
        if (_uiState.value.userResponseRequested) {
            // Add user message to chat and update current response
            _uiState.update {
                it.copy(
                    messages = it.messages + Message.UserMessage(userInput),
                    inputText = "",
                    isLoading = true,
                    userResponseRequested = false,
                    currentUserResponse = userInput
                )
            }
        } else { // Initial message flow - add user message and start agent
            _uiState.update {
                it.copy(
                    messages = it.messages + Message.UserMessage(userInput),
                    inputText = "",
                    isInputEnabled = false,
                    isLoading = true
                )
            }

            // Start the agent processing
            viewModelScope.launch {
                runAgent(userInput)
            }
        }
    }

    // Run the agent
    private suspend fun runAgent(userInput: String) {
        withContext(Dispatchers.Default) {
            try {
                // Create and run the agent using the factory
                val agent = agentProvider.provideAgent(
                    onToolCallEvent = { message ->
                        // Add tool call messages to the chat
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(
                                    messages = it.messages + Message.ToolCallMessage(message)
                                )
                            }
                        }
                    },
                    onErrorEvent = { errorMessage ->
                        // Handle agent errors
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(
                                    messages = it.messages + Message.ErrorMessage(errorMessage),
                                    isInputEnabled = true,
                                    isLoading = false
                                )
                            }
                        }
                    },
                    onAssistantMessage = { message ->
                        // Handle agent asking user a question
                        _uiState.update {
                            it.copy(
                                messages = it.messages + Message.AgentMessage(message),
                                isInputEnabled = true,
                                isLoading = false,
                                userResponseRequested = true
                            )
                        }
                        sendPendingA2uiErrors()

                        // Wait for user response
                        val userResponse = _uiState
                            .first { it.currentUserResponse != null }
                            .currentUserResponse
                            ?: throw IllegalArgumentException("User response is null")

                        // Update the state to reset current response
                        _uiState.update {
                            it.copy(
                                currentUserResponse = null
                            )
                        }

                        // Return it to the agent
                        userResponse
                    },
                )

                // Run the agent
                val result = agent.run(userInput)

                // Update UI with final state and mark chat as ended
                _uiState.update {
                    it.copy(
                        messages = it.messages +
                                Message.ResultMessage(result) +
                                Message.SystemMessage("The agent has stopped."),
                        isInputEnabled = false,
                        isLoading = false,
                        isChatEnded = true
                    )
                }
            } catch (e: Exception) {
                // Handle errors
                _uiState.update {
                    it.copy(
                        messages = it.messages + Message.ErrorMessage("Error: ${e.message}"),
                        isInputEnabled = true,
                        isLoading = false
                    )
                }
            }
        }
    }

    // Restart the chat
    fun restartChat() {
        _uiState.update {
            AgentDemoUiState(
                messages = listOf(Message.SystemMessage(agentProvider.description))
            )
        }
    }

    private companion object {
        const val MAX_A2UI_CORRECTIONS = 2
    }
}
