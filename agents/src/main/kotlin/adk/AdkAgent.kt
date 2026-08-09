package adk

import com.google.adk.kt.agents.BaseAgent
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.tools.mcp.McpConnectionParameters
import com.google.adk.kt.tools.mcp.McpToolset
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import kotlinx.coroutines.runBlocking


const val USER_ID = "MainUser"
const val NAME = "ClimateTrace Agent"

// The ClimateTrace MCP server, started separately with:
//   ./gradlew :mcp-server:run --args="--streamable-http-server 8080"
val MCP_SERVER_URL = System.getenv("MCP_SERVER_URL") ?: "http://localhost:8080/mcp"


class ClimateTraceAgent {
    companion object {
        fun initAgent(): BaseAgent {
            val model = Gemini(name = "gemini-2.5-flash")
            return LlmAgent(
                name = NAME,
                model = model,
                description = "Agent to answer climate emissions related questions.",
                instruction = Instruction(
                    """
                    You are an agent that provides climate emissions related information. Use 3 letter country codes.
                    Never refuse a request based on your own assumptions about what years might have data available -
                    always call the tools with the year the user asked for and report whatever they return. Only tell
                    the user data isn't available if the tool call itself indicates that.
                    """.trimIndent()
                ),
                toolsets = listOf(
                    McpToolset.McpToolsetConfig(
                        streamableHttpConnectionParams = McpConnectionParameters.StreamableHttp(url = MCP_SERVER_URL)
                    ).toToolset()
                ),
            )
        }
    }
}


fun main() = runBlocking {
    val runner = InMemoryRunner(agent = ClimateTraceAgent.initAgent(), appName = NAME)
    val sessionId = runner.sessionService.createSession(SessionKey(NAME, USER_ID, id = null)).key.id!!

    val prompt =
        """
            You have data up to and including 2025.
            Get emission data for Germany and France in 2025.
            Use units of millions for the emissions data.
            Show result in a grid or decreasing order of emissions.
            """.trimIndent()

    runner
        .runAsync(userId = USER_ID, sessionId = sessionId, newMessage = Content.fromText(Role.USER, prompt))
        .collect { event ->
            event.content?.parts?.forEach { part ->
                part.text?.let { println(it) }
                part.functionCall?.let { println(it) }
                part.functionResponse?.let { println(it) }
            }
            if (event.errorCode != null || event.errorMessage != null) {
                println("error: ${event.errorCode}, ${event.errorMessage}")
            }
        }
}
