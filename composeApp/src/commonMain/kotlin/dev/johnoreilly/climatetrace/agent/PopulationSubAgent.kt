package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.createAgentTool
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository

/**
 * Creates a sub-agent tool for population data lookup.
 *
 * This follows the agent-as-a-tool pattern where a sub-agent runs inside a tool
 * provided to the main agent. The sub-agent has access to GetCountryTool and
 * GetPopulationTool, keeping the population lookup context separate from the
 * main agent's context.
 */
fun createPopulationAgentTool(
    climateTraceRepository: ClimateTraceRepository
): Tool<*, *> {
    val toolRegistry = ToolRegistry {
        tool(GetPopulationTool(climateTraceRepository))
    }

    val strategy = functionalStrategy<String, String> { input ->
        var assistantMessage = requestLLM(input)

        repeat(10) {
            if (getToolCalls(assistantMessage).isEmpty()) {
                return@functionalStrategy getTextParts(assistantMessage).joinToString("") { it.text }
            }

            val pendingCalls = getToolCalls(assistantMessage)
            val results = executeTools(pendingCalls, parallelTools = true)
            assistantMessage = sendToolResults(results)
        }

        getTextParts(assistantMessage).joinToString("") { it.text }
            .ifBlank { "Could not determine population data." }
    }

    val agentConfig = AIAgentConfig(
        prompt = prompt("populationAgent") {
            system(
                """
                You are a specialist agent for looking up population data for countries.
                Return the population data clearly and concisely.
                """.trimIndent()
            )
        },
        model = getLLModel(),
        maxAgentIterations = 10
    )

    val agent = AIAgent(
        promptExecutor = getPromptExecutor(),
        strategy = strategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry,
    )

    return AIAgentService
        .fromAgent(agent)
        .createAgentTool(
            agentName = "PopulationAgent",
            agentDescription = "A sub-agent that retrieves population data for a country. " +
                    "Provide a country name or country code and it will return the population. " +
                    "Prefer using this tool for population queries rather than looking up population data directly.",
            inputDescription = "Country name or country code to look up population for"
        )
}
