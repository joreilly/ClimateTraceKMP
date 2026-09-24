package agui

import adk.ClimateTraceAgent
import adk.MCP_SERVER_URL
import adk.NAME
import adk.USER_ID
import com.agui.core.types.AgUiJson
import com.agui.core.types.BaseEvent
import com.agui.core.types.RunAgentInput
import com.agui.core.types.UserMessage
import com.google.adk.kt.agents.RunConfig
import com.google.adk.kt.agents.StreamingMode
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.writeStringUtf8
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

/**
 * Serves the ClimateTrace ADK agent over the [AG-UI protocol](https://docs.ag-ui.com), so a
 * Compose Multiplatform client can drive it with `com.ag-ui.community:kotlin-client` instead of
 * speaking ADK's own `/run_sse` API.
 *
 * Start it with `./gradlew :agents:startAgUiServer`, having first started the MCP server:
 *
 *     ./gradlew :mcp-server:run --args="--streamable-http-server 8080"
 */
fun main() {
    val port = System.getenv("AGUI_PORT")?.toIntOrNull() ?: 8082
    val runner = InMemoryRunner(agent = ClimateTraceAgent.initAgent(), appName = NAME)
    println("AG-UI endpoint on http://localhost:$port/agui (tools from $MCP_SERVER_URL)")
    embeddedServer(Netty, port = port, host = "0.0.0.0") { routing { agUiRoute(runner) } }
        .start(wait = true)
}

/**
 * The AG-UI endpoint. One POST carries the whole run: the request is a [RunAgentInput], the
 * response is a `text/event-stream` of AG-UI events.
 *
 * SSE is written by hand rather than with Ktor's SSE plugin because this module is pinned to the
 * Ktor 2.x that `google-adk-kotlin-webserver` is built against — the same reason ADK's own
 * `RunRoutes` streams this way.
 */
fun Route.agUiRoute(runner: InMemoryRunner, appName: String = NAME, userId: String = USER_ID) {
    post("/agui") {
        val input =
            try {
                AgUiJson.decodeFromString<RunAgentInput>(call.receiveText())
            } catch (e: SerializationException) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Not a valid AG-UI RunAgentInput: ${e.message}",
                )
            }

        // An AG-UI thread maps onto an ADK session: the client sends the same threadId for the
        // whole conversation and ADK keeps that session's history server side. Only the newest
        // user message is forwarded — sending the client's full `messages` list as well would
        // duplicate the history ADK already holds.
        val sessionKey = SessionKey(appName, userId, input.threadId)
        if (runner.sessionService.getSession(sessionKey) == null) {
            runner.sessionService.createSession(sessionKey)
        }

        val newMessage =
            input.messages.filterIsInstance<UserMessage>().lastOrNull()?.let {
                Content.fromText(Role.USER, it.content)
            }

        call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
            runner
                .runAsync(
                    userId = userId,
                    sessionId = input.threadId,
                    newMessage = newMessage,
                    runConfig = RunConfig(streamingMode = StreamingMode.SSE),
                )
                .asAgUiEvents(threadId = input.threadId, runId = input.runId)
                .collect { event ->
                    writeStringUtf8("data: ${AgUiJson.encodeToString<BaseEvent>(event)}\n\n")
                    flush()
                }
        }
    }
}
