package adk

import adk.ClimateTraceAgent.Companion.initAgent
import com.google.adk.kt.agents.BaseAgent
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.events.Event
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Content
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

const val USER_ID = "MainUser"
const val NAME = "ClimateTrace Agent"

class ClimateTraceAgent {
    companion object {
        fun initAgent(): BaseAgent {
            // Falls back to the GOOGLE_API_KEY / GEMINI_API_KEY environment variables.
            val model = Gemini(name = "gemini-2.5-flash")
            return LlmAgent(
                name = NAME,
                model = model,
                description = "Agent to answer climate emissions related questions.",
                instruction = Instruction("You are an agent that provides climate emissions related information. Use 3 letter country codes."),
                tools = ClimateTraceTool().generatedTools(),
            )
        }
    }
}

fun main() = runBlocking {
    val runner = InMemoryRunner(initAgent())
    val session = runner.sessionService.createSession(SessionKey(NAME, USER_ID, null))

    val prompt =
        """
            You have data up to and including 2025.
            Get emission data for Germany and France in 2025.
            Use units of millions for the emissions data.
            Show result in a grid or decreasing order of emissions.
            """.trimIndent()

    val userMsg = Content.fromText("user", prompt)
    val events = runner.runAsync(USER_ID, session.key.id!!, newMessage = userMsg)

    events.collect { event: Event ->
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
