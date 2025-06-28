import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val openAIApiKey = ""
val apiKeyGoogle = ""



@OptIn(ExperimentalUuidApi::class)
suspend fun runKoogAgent() {
    val agent = AIAgent(
        executor = simpleOpenAIExecutor(openAIApiKey),
        //executor = simpleGoogleAIExecutor(apiKeyGoogle),
        llmModel = OpenAIModels.Chat.GPT4o,
        //llmModel = GoogleModels.Gemini1_5Pro,
        toolRegistry = createToolSetRegistry()
    ) {
        handleEvents {
            onToolCall { tool: ai.koog.agents.core.tools.Tool<*, *>, toolArgs: ai.koog.agents.core.tools.Tool.Args ->
                println("Tool called: tool ${tool.name}, args $toolArgs")
            }

            onAgentRunError { strategyName: String, sessionUuid: Uuid?, throwable: Throwable ->
                println("An error occurred: ${throwable.message}\n${throwable.stackTraceToString()}")
            }

            onAgentFinished { strategyName, result ->
                println("Agent (strategy = $strategyName) finished with result: $result")
            }
        }
    }

    agent.run(
        """
        Get emission data for France, Germany for 2023 and 2024.
        Use units of millions for the emissions data.
        Also get best estimate of population for each country and show per-capita emissions.
        """.trimIndent()
    )
}


suspend fun createToolSetRegistry(): ToolRegistry {
    val processClimateTrace = ProcessBuilder("java", "-jar",
        "./mcp-server/build/libs/serverAll.jar", "--stdio"
    ).start()
    val transportClimateTrace = McpToolRegistryProvider.defaultStdioTransport(processClimateTrace)
    val toolRegistryClimateTrace = McpToolRegistryProvider.fromTransport(transportClimateTrace)

    val localToolSetRegistry = ToolRegistry { tools(ClimateTraceTool().asTools()) }

    // Can use either local toolset or one based on MCP server
    //return toolRegistryClimateTrace
    return localToolSetRegistry
}