import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.server.mcp



fun main() {
    val server = configureMcpServer()

    val port = System.getenv().getOrDefault("PORT", "8080").toInt()
    embeddedServer(CIO, port, host = "0.0.0.0") {
        install(SSE)

        routing {
            mcp {
                server
            }

            get("/hi") {
                call.respond("hello!!")

            }
        }
    }.start(wait = true)
}
