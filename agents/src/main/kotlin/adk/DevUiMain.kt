package adk

import adk.ClimateTraceAgent.Companion.initAgent
import com.google.adk.kt.artifacts.InMemoryArtifactService
import com.google.adk.kt.sessions.InMemorySessionService
import com.google.adk.kt.webserver.AdkWebServer
import com.google.adk.kt.webserver.loaders.SingleAgentLoader
import com.google.adk.kt.webserver.telemetry.ApiServerSpanExporter

fun main() {
    val server = AdkWebServer(
        sessionService = InMemorySessionService(),
        artifactService = InMemoryArtifactService(),
        agentLoader = SingleAgentLoader(initAgent()),
        apiServerSpanExporter = ApiServerSpanExporter(),
    )
    server.start(wait = true)
}
