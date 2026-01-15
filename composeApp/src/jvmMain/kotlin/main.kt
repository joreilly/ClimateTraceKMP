import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgent.Companion.invoke
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.dsl.extension.asAssistantMessage
import ai.koog.agents.core.dsl.extension.containsToolCalls
import ai.koog.agents.core.dsl.extension.executeMultipleTools
import ai.koog.agents.core.dsl.extension.extractToolCalls
import ai.koog.agents.core.dsl.extension.requestLLMMultiple
import ai.koog.agents.core.dsl.extension.sendMultipleToolResults
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.ToolRegistry.Companion.invoke
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.prompt.dsl.prompt
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.johnoreilly.climatetrace.agent.CurrentDatetimeTool
import dev.johnoreilly.climatetrace.agent.ExitTool
import dev.johnoreilly.climatetrace.agent.GetAssetEmissionsTool
import dev.johnoreilly.climatetrace.agent.GetCountryTool
import dev.johnoreilly.climatetrace.agent.GetEmissionsTool
import dev.johnoreilly.climatetrace.agent.GetPopulationTool
import dev.johnoreilly.climatetrace.agent.getLLModel
import dev.johnoreilly.climatetrace.agent.getPromptExecutor
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.di.initKoin

/*
val koin = initKoin(enableNetworkLogs = true).koin

suspend fun main() {
    println("hello")

    val climateTraceRepository = koin.get<ClimateTraceRepository>()

    val agent: ClimateTraceAgent = ClimateTraceAgent(climateTraceRepository)

    //agent.runAgent("What were the emissions for the UK in 2024?")

    agent.runAgent("compare the per-capita emissions of the UK and France in 2024")
}
*/

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ClimateTraceKMP") {
        App()
    }
}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}


suspend fun provideAgent(
    onToolCallEvent: suspend (String) -> Unit,
    onErrorEvent: suspend (String) -> Unit,
    onAssistantMessage: suspend (String) -> String,
): AIAgent<String, String> {

    val strategy = functionalStrategy<String, String> { initialInput ->
        var inputMessage = initialInput
        var lastAssistantMessage = ""

        repeat(50) { // align with agentConfig.maxAgentIterations
            println("Calling LLM with Input = $inputMessage")
            var responses = requestLLMMultiple(inputMessage)

            // Resolve tools until none left, mirroring graph strategy
            while (responses.containsToolCalls()) {
                val pendingCalls = extractToolCalls(responses)
                println("Pending Calls")
                println(pendingCalls.map { "${it.tool} ${it.content}" })

                val results = executeMultipleTools(pendingCalls, parallelTools = true)

                // Finish condition: if ExitTool is called, return its result directly
                if (results.size == 1 && results.first().tool == ExitTool.name) {
                    return@functionalStrategy results.first().result!!.toString()
                }

                // Send tool results back to LLM
                responses = sendMultipleToolResults(results)
            }

            // No more tool calls: deliver assistant message to UI and get possible user follow-up
            lastAssistantMessage = responses.first().asAssistantMessage().content
            val userReply = onAssistantMessage(lastAssistantMessage)

            // If user provides no reply, consider conversation finished and return assistant response
            if (userReply.isBlank()) {
                return@functionalStrategy lastAssistantMessage
            }

            // Prepare for next loop iteration with user's reply
            inputMessage = userReply
        }

        // Max iterations reached; return last assistant message
        lastAssistantMessage
    }


    val agentConfig = AIAgentConfig(
        prompt = prompt("climateTrace") {
            system(
                """                
                    You an AI assistant specialising in providing information about global climate emissions.
                    Use 3 letter country codes.
                    The year is currently 2025.
                
                    Use the tools at your disposal to:
                    1. Look up country codes from country names
                    2. Get climate emission information.
                    3. Get cause of emissions using asset emission information (GetAssetEmissionsTool)
                    4. Get population data.
                    5. Get current date and time.
                
                    Pass the list of country codes and the year to the GetEmissionsTool tool to get climate emission information.
                    Use units of millions for the emissions data.                    
                    """.trimIndent(),
            )
        },
        model = getLLModel(),
        maxAgentIterations = 50
    )

    // Return the agent
    return AIAgent(
        promptExecutor = getPromptExecutor(),
        //strategy = strategy,
        agentConfig = agentConfig,
    ) {

        // Install platform-specific features (actualized per target)
        install(OpenTelemetry) {

        }




        install(EventHandler) {
            onToolCallStarting { ctx ->
                onToolCallEvent("Tool ${ctx.tool.name}, args ${ctx.toolArgs}")
            }


            onAgentExecutionFailed { ctx ->
                onErrorEvent("${ctx.throwable.message}")
            }



            onAgentCompleted { _ ->
                // Skip finish event handling
            }

        }
    }
}



