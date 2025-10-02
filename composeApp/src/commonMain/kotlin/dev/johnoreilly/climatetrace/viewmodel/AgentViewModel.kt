package dev.johnoreilly.climatetrace.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import dev.johnoreilly.climatetrace.agent.ClimateTraceAgent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


data class AgentUIState(
    val prompt: String = "",
    val result: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,

    // For handling user responses when agent asks a question
    val userResponseRequested: Boolean = false,
    val currentUserResponse: String? = null,
)

sealed interface AgentEvents {
    data class SetPrompt(val prompt: String): AgentEvents
    data object RunAgent: AgentEvents
}

open class AgentViewModel : ViewModel(), KoinComponent {
    private val climateTraceAgent: ClimateTraceAgent by inject()

    private val events = MutableSharedFlow<AgentEvents>(extraBufferCapacity = 20)

    val uiState: StateFlow<AgentUIState> = viewModelScope.coroutineScope.launchMolecule(mode = RecompositionMode.Immediate) {
        agentPresenter(events)
    }


    fun updatePrompt(prompt: String) = events.tryEmit(AgentEvents.SetPrompt(prompt))
    fun runAgent() = events.tryEmit(AgentEvents.RunAgent)

    @Composable
    fun agentPresenter(events: Flow<AgentEvents>): AgentUIState {
        var prompt by remember { mutableStateOf(
            """
            Get per capita emission data for Spain, France, Germany, and Italy for the year 2024. 
            Show results in a table and include full country name, population, and total emissions.
            Show in decreasing order of per capita emissions.
            """.trimIndent()
        )}
        var isLoading by remember { mutableStateOf(false)}
        var result by remember { mutableStateOf("")}

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    is AgentEvents.RunAgent -> {
                        isLoading = true
                        result = climateTraceAgent.runAgent(prompt)
                        isLoading = false
                    }

                    is AgentEvents.SetPrompt -> {
                        prompt = event.prompt
                    }
                }
            }
        }

        return AgentUIState(
            prompt = prompt,
            result = result,
            isLoading = isLoading
        )
    }
}

