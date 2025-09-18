package adk

import adk.ClimateTraceAgent.Companion.initAgent
import com.google.adk.web.AdkWebServer

fun main() {
    AdkWebServer.start(initAgent())
}
