package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.agent.AIAgent

/**
 * Interface for agent factory
 */
interface AgentProvider {
    val description: String

    suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String>
}
