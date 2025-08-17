package koog

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking

val openAIApiKey = ""
val apiKeyGoogle = ""



fun main() = runBlocking {

    val model = LLModel(
        provider = LLMProvider.Ollama,
        id = "gpt-oss",
        //id = "llama3.1:8b",
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Schema.JSON.Simple,
            LLMCapability.Tools
        ),
    )


    val agent = AIAgent(
        //executor = simpleOpenAIExecutor(openAIApiKey),
        //executor = simpleGoogleAIExecutor(apiKeyGoogle),
        executor = simpleOllamaAIExecutor(),
        //llmModel = OpenAIModels.Chat.GPT4o,
        //llmModel = GoogleModels.Gemini1_5Pro,
        systemPrompt  ="""
            You an AI assistant specialising in providing information about global climate emissions.
            Pass the list of countries and the year to the get-emissions MCP tool to get climate emission information.            
            Use 3 letter country codes.
            Use units of millions for the emissions data.                    
            """,
        llmModel = model,
        toolRegistry = createToolSetRegistry()
    ) {
        handleEvents {
            onToolCall { eventContext ->
                println("Tool called: ${eventContext.tool} with args ${eventContext.toolArgs}")
            }
            onAgentRunError { eventContext ->
                println("An error occurred: ${eventContext.throwable.message}\n${eventContext.throwable.stackTraceToString()}")
            }
        }
    }


    val output = agent.run(
        """
        Get per capita emission data for France, Germany and Spain for the year 2024.
        Results should include full country name, country code, total emissions, population and per capita emissions.
        """
    )
    println("Result = $output")
}



suspend fun createToolSetRegistry(): ToolRegistry {
    val processClimateTrace = ProcessBuilder("java", "-jar",
        "./mcp-server/build/libs/serverAll.jar", "--stdio"
    ).start()
    val transportClimateTrace = McpToolRegistryProvider.defaultStdioTransport(processClimateTrace)
    val toolRegistryClimateTrace = McpToolRegistryProvider.fromTransport(transportClimateTrace)

    val localToolSetRegistry = ToolRegistry { tools(ClimateTraceTool().asTools()) }

    // Can use either local toolset or one based on MCP server
    return toolRegistryClimateTrace
    //return localToolSetRegistry
}