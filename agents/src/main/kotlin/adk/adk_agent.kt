package adk

import adk.ClimateTraceAgent.Companion.initAgent
import com.google.adk.agents.BaseAgent
import com.google.adk.agents.LlmAgent
import com.google.adk.events.Event
import com.google.adk.models.Gemini
import com.google.adk.runner.InMemoryRunner
import com.google.adk.tools.AgentTool
import com.google.adk.tools.BaseTool
import com.google.adk.tools.GoogleSearchTool
import com.google.adk.tools.LongRunningFunctionTool
import com.google.adk.tools.mcp.McpToolset
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.Part
import io.modelcontextprotocol.client.transport.ServerParameters
import io.reactivex.rxjava3.functions.Consumer
import kotlin.jvm.optionals.getOrNull


const val USER_ID = "MainUser"
const val NAME = "ClimateTrace Agent"


class ClimateTraceAgent {
    companion object {
        @JvmStatic
        fun initAgent(): BaseAgent {
            val apiKeyGoogle = ""

            val climateTools = McpToolset(
                ServerParameters
                    .builder("java")
                    .args("-jar", "/Users/joreilly/dev/github/ClimateTraceKMP/mcp-server/build/libs/serverAll.jar", "--stdio")
                    .build()
            ).loadTools().join()

//            val getCountriesTool = LongRunningFunctionTool.create(ClimateTraceTool::class.java, "getCountries")
//            val getEmissionsTool = LongRunningFunctionTool.create(ClimateTraceTool::class.java, "getEmissions")
//            val climateTools = listOf(getCountriesTool, getEmissionsTool, GoogleSearchTool())


            val model = Gemini(
                "gemini-2.0-flash",
                Client.builder()
                    .apiKey(apiKeyGoogle)
                    .build()
            )


            val searchAgent = LlmAgent.builder()
                .name("SearchAgent")
                .model(model)
                .description("Google Search agent")
                .instruction("You're a specialist in Google Search")
                .tools(GoogleSearchTool())
                .build()

            val climateAgent = LlmAgent.builder()
                .name("ClimateAgent")
                .model(model)
                .description("Agent to answer climate emissions related questions.")
                .instruction("You are an agent that provides climate emissions related information. Use 3 letter country codes.")
                .tools(climateTools)
                .build()

            return LlmAgent.builder()
                .name(NAME)
                .description("Agent to answer climate emissions related questions.")
                //.instruction("You are an agent that provides climate emissions related information. Use 3 letter country codes. Use SearchAgent for population data.")
                .instruction("""
                    You are an agent that provides climate emissions related information.
            
                    Use `ClimateAgent` for emission data.
                    Use `SearchAgent` for population data.
                    """)

                .model(model)
                //.subAgents(searchAgent, climateAgent)
                .tools(AgentTool.create(searchAgent), AgentTool.create(climateAgent))
                .build()
        }
    }
}


fun main() {
    val runner = InMemoryRunner(initAgent())
    val session = runner
        .sessionService()
        .createSession(NAME, USER_ID)
        .blockingGet()

    val prompt =
        """
            Get emission data for EU countries in 2024.
            Use units of millions for the emissions data.
            Show result in a grid or decreasing order of emissions.
            """.trimIndent()

    val userMsg = Content.fromParts(Part.fromText(prompt))
    val events = runner.runAsync(USER_ID, session.id(), userMsg)

    events.blockingForEach(Consumer { event: Event ->
        event.content().get().parts().getOrNull()?.forEach { part ->
            part.text().getOrNull()?.let { println(it) }
            part.functionCall().getOrNull()?.let { println(it) }
            part.functionResponse().getOrNull()?.let { println(it) }
        }
        if (event.errorCode().isPresent || event.errorMessage().isPresent) {
            println("error: ${event.errorCode().get()}, ${event.errorMessage().get()}")
        }
    })
}

