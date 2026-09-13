package adk

import com.google.adk.kt.webserver.AdkServerConfig
import com.google.adk.kt.webserver.dev.AdkDevServer

fun main() {
    AdkDevServer(
        AdkServerConfig.inMemory(ClimateTraceAgent.initAgent(), port = 8081)
    ).start(wait = true)
}
