package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository



class ClimateTraceAgent(private val climateTraceRepository: ClimateTraceRepository) {
    private val apiKeyGoogle = ""
    private var agent: AIAgent<String, String>? = null

    suspend fun createAgent() {
        //val executor = simpleOpenAIExecutor(openAiApiKey)
        val executor = simpleGoogleAIExecutor(apiKeyGoogle)

        agent = AIAgent(
            promptExecutor = executor,
            llmModel = GoogleModels.Gemini2_5Flash,
            //llmModel = OpenAIModels.Chat.GPT4o,
            toolRegistry = createToolSetRegistry(climateTraceRepository),
            systemPrompt  =
                """                
                You an AI assistant specialising in providing information about global climate emissions.
                Use 3 letter country codes.
            
                Use the tools at your disposal to:
                1. Look up country codes from country names
                2. Get climate emission information
                3. Get population information
            
                Pass the list of country codes and the year to the GetEmissionsTool tool to get climate emission information.
                Use units of millions for the emissions data.
                """,
        ) {
            handleEvents {
                onLLMCallStarting { ctx ->
                    println("Request to LLM")
                }

                onLLMCallCompleted { ctx ->
                    println("Response from LLM")
                    ctx.responses.forEach { println("   $it") }
                }

                onToolExecutionStarting { eventContext ->
                    println("Tool called: ${eventContext.tool} with args ${eventContext.toolArgs}")
                }
                onAgentExecutionFailed { eventContext ->
                    println("An error occurred: ${eventContext.throwable.message}\n${eventContext.throwable.stackTraceToString()}")
                }

                onNodeExecutionStarting {
                    println(it.node.name)
                }

                onAgentCompleted {
                    println("onAgentCompleted")
                }
            }
        }
    }


    suspend fun runAgent(prompt: String): String {
        agent?.let { agent ->
            println("Running agent")
            val output = agent.run(prompt)
            println("Result = $output")
            return output
        }

        return ""
    }

    private suspend fun createToolSetRegistry(climateTraceRepository: ClimateTraceRepository): ToolRegistry {
//    val processClimateTrace = ProcessBuilder("java", "-jar",
//        "./mcp-server/build/libs/serverAll.jar", "--stdio"
//    ).start()
//    val transportClimateTrace = McpToolRegistryProvider.defaultStdioTransport(processClimateTrace)
//    val toolRegistryClimateTrace = McpToolRegistryProvider.fromTransport(transportClimateTrace)
//
//    //val localToolSetRegistry = ToolRegistry { tools(ClimateTraceTool().asTools()) }

        val localToolSetRegistry = ToolRegistry {
            tool(GetCountryTool(climateTraceRepository))
            tool(GetEmissionsTool(climateTraceRepository))
            tool(GetPopulationTool(climateTraceRepository))
        }

        // Can use either local toolset or one based on MCP server
        //return toolRegistryClimateTrace
        return localToolSetRegistry
    }
}
