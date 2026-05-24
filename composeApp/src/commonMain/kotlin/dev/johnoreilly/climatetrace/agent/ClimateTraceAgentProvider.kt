package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import kotlin.time.ExperimentalTime


// TODO use Koin for these and inject?
expect fun getLLModel(): LLModel
expect fun getPromptExecutor(): PromptExecutor

class ClimateTraceAgentProvider(
    private val climateTraceRepository: ClimateTraceRepository
) : AgentProvider {

    override val description: String = "Hi, I'm a climate agent. I can provide climate emission information for different countries/years."

    @OptIn(ExperimentalTime::class)
    override suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String,
    ): AIAgent<String, String> {

        val toolRegistry = ToolRegistry {
            tool(GetEmissionsTool(climateTraceRepository))
            tool(GetAssetEmissionsTool(climateTraceRepository))
            tool(createPopulationAgentTool(climateTraceRepository))
        }

        val agentConfig = AIAgentConfig(
            prompt = prompt("climateTrace") {
                system(
                    """                
                    You are an AI assistant specialising in providing information about global climate emissions.
                    The year is currently 2026.
                    You have data up to and including 2025.
                    Use units of millions of tonnes of CO2 equivalent.
                    """.trimIndent(),
                )
            },
            model = getLLModel(),
            maxAgentIterations = 50
        )

        // Return the agent
        return AIAgent(
            promptExecutor = getPromptExecutor(),
            strategy = createStrategy(onAssistantMessage),
            agentConfig = agentConfig,
            toolRegistry = toolRegistry,
        ) {
            install(EventHandler) {
                onToolCallStarting { ctx ->
                    onToolCallEvent("Tool ${ctx.toolName}, args ${ctx.toolArgs}")
                }

                onAgentExecutionFailed { ctx ->
                    onErrorEvent("${ctx.error.message}")
                }
            }
        }
    }


    /**
     * Creates the agent's conversation strategy — the core loop that coordinates
     * LLM calls, tool execution, and multi-turn interaction with the user.
     *
     * The strategy implements a two-level loop:
     *
     * **Outer loop** — runs for as long as the user keeps sending messages. After each
     * assistant response, [onAssistantMessage] suspends until the user replies; an empty
     * reply signals the end of the conversation.
     *
     * **Inner loop** — handles agentic tool use. After each LLM response, if the model
     * has requested tool calls, those tools are executed and their results are fed back
     * to the LLM. This repeats until the LLM produces a plain text response with no
     * further tool calls.
     */
    fun createStrategy(onAssistantMessage: suspend (String) -> String) =
        functionalStrategy<String, String> { initialInput ->
            var inputMessage = initialInput
            var assistantMessage = ""

            // Outer loop: continue the conversation until the user sends an empty reply.
            while (inputMessage.isNotEmpty()) {
                // Send the user input to the LLM and get the assistant response.
                println("Calling LLM with Input = $inputMessage")
                var response = requestLLM(inputMessage)

                // Inner loop: if the LLM requested tool calls, execute them and send
                // the results back. The LLM may chain multiple rounds of tool calls
                // before producing a final text answer.
                while (getToolCalls(response).isNotEmpty()) {
                    // Execute the tools and return the results
                    val results = executeTools(response)

                    // Send the tool results back to the LLM. The LLM may call more tools or return a final output
                    response = sendToolResults(results)
                }

                // No more tool calls — extract the assistant's final text response.
                assistantMessage = response.textContent()

                // Deliver the response to the UI and suspend until the user replies.
                // An empty reply will exit the outer loop.
                inputMessage = onAssistantMessage(assistantMessage)
            }
            assistantMessage
        }

}
