package adk

import com.google.adk.agents.BaseAgent
import com.google.adk.agents.LlmAgent
import com.google.adk.events.Event
import com.google.adk.models.Gemini
import com.google.adk.runner.InMemoryRunner
import com.google.adk.tools.mcp.McpToolset
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.Part
import io.modelcontextprotocol.client.transport.ServerParameters
import io.reactivex.rxjava3.functions.Consumer
import kotlin.jvm.optionals.getOrNull


const val USER_ID = "MainUser"
const val NAME = "ClimateTrace Agent"

val ROOT_AGENT: BaseAgent = initAgent()

fun initAgent(): BaseAgent {
    val apiKeyGoogle = ""

    val mcpTools = McpToolset(
        ServerParameters
            .builder("java")
            .args("-jar", "./mcp-server/build/libs/serverAll.jar", "--stdio")
            .build()
    ).loadTools().join()

    val model = Gemini(
        "gemini-1.5-pro",
        Client.builder()
            .apiKey(apiKeyGoogle)
            .build()
    )

    return LlmAgent.builder()
        .name(NAME)
        .model(model)
        .description("Agent to answer climate emissions related questions.")
        .tools(mcpTools)
        .build()
}

fun main() {
    val runner = InMemoryRunner(ROOT_AGENT)
    val session = runner
        .sessionService()
        .createSession(NAME, USER_ID)
        .blockingGet()

    val prompt =
        """
        Get emission data for France and Germany for 2023 and 2024.
        Use units of millions for the emissions data.
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