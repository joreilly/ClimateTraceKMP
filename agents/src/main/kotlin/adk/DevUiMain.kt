package adk

import com.google.adk.kt.artifacts.InMemoryArtifactService
import com.google.adk.kt.sessions.InMemorySessionService
import com.google.adk.kt.webserver.AdkWebServer
import com.google.adk.kt.webserver.loaders.SingleAgentLoader
import com.google.adk.kt.webserver.telemetry.ApiServerSpanExporter

fun main() {
    AdkWebServer(
        port = 8081,
        sessionService = InMemorySessionService(),
        artifactService = InMemoryArtifactService(),
        agentLoader = SingleAgentLoader(ClimateTraceAgent.initAgent()),
        apiServerSpanExporter = ApiServerSpanExporter(),
    ).start(wait = true)
}
