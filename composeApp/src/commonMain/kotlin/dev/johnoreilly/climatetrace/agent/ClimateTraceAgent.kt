package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.asAssistantMessage
import ai.koog.agents.core.agent.containsToolCalls
import ai.koog.agents.core.agent.executeMultipleTools
import ai.koog.agents.core.agent.extractToolCalls
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.agent.requestLLM
import ai.koog.agents.core.agent.requestLLMMultiple
import ai.koog.agents.core.agent.sendMultipleToolResults
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository


expect fun getLLModel(): LLModel
expect fun getPromptExecutor(apiKey: String = ""): PromptExecutor

class ClimateTraceAgent(private val climateTraceRepository: ClimateTraceRepository) {
    private val apiKeyGoogle = ""

    suspend fun createAgent() = AIAgent<String, String>(
            promptExecutor = getPromptExecutor(apiKeyGoogle),
            llmModel = getLLModel(),
            toolRegistry = createToolSetRegistry(climateTraceRepository),
            strategy = functionalStrategy { input ->
                println("Calling LLM with Input = $input")
                var responses = requestLLMMultiple(input)

                while (responses.containsToolCalls()) {
                    val pendingCalls = extractToolCalls(responses)
                    println("Pending Calls")
                    println(pendingCalls.map { "${it.tool} ${it.content}" })
                    val results = executeMultipleTools(pendingCalls, parallelTools = true)
                    responses = sendMultipleToolResults(results)
                }

                val draft = responses.single().asAssistantMessage().content
                requestLLM("Improve and clarify: $draft").asAssistantMessage().content
            },
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
        )



    suspend fun runAgent(prompt: String): String {
        val agent = createAgent()
        println("Running agent")
        val output = agent.run(prompt)
        println("Result = $output")
        return output
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
