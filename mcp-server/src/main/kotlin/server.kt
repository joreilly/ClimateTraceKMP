import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.di.initKoin
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonObject


private val koin = initKoin(enableNetworkLogs = true).koin

fun configureServer(): Server {
    val climateTraceRepository = koin.get<ClimateTraceRepository>()

    val server = Server(
        Implementation(
            name = "ClimateTrace MCP Server",
            version = "1.0.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )


    server.addTool(
        name = "get-countries",
        description = "List of countries"
    ) {
        val countries = climateTraceRepository.fetchCountries()
        CallToolResult(
            content =
                countries.map { TextContent("${it.name}, ${it.alpha3}") }
        )
    }

    server.addTool(
        name = "get-emissions",
        description = "List emission info for a particular country",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("countryCode") { put("type", JsonPrimitive("string")) }
                putJsonObject("year") { put("type", JsonPrimitive("string")) }
            },
            required = listOf("countryCode", "year")
        )

    ) { request ->
        val countryCode = request.arguments["countryCode"]
        val year = request.arguments["year"]
        if (countryCode == null || year == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("The 'countryCode' and `year` parameters are required."))
            )
        }

        val countryEmissionInfo = climateTraceRepository.fetchCountryEmissionsInfo(
            countryCode = countryCode.jsonPrimitive.content,
            year = year.jsonPrimitive.content
        )
        CallToolResult(
            content = countryEmissionInfo.map { TextContent(it.emissions.co2.toString()) }
        )
    }

    return server
}

/**
 * Runs an MCP (Model Context Protocol) server using standard I/O for communication.
 *
 * This function initializes a server instance configured with predefined tools and capabilities.
 * It sets up a transport mechanism using standard input and output for communication.
 * Once the server starts, it listens for incoming connections, processes requests,
 * and executes the appropriate tools. The server shuts down gracefully upon receiving
 * a close event.
 */
fun `run mcp server using stdio`() {
    val server = configureServer()
    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )

    runBlocking {
        server.connect(transport)
        val done = Job()
        server.onClose {
            done.complete()
        }
        done.join()
    }
}

/**
 * Launches an SSE (Server-Sent Events) MCP (Model Context Protocol) server on the specified port.
 * This server enables clients to connect via SSE for real-time communication and provides endpoints
 * for handling specific messages.
 *
 * @param port The port number on which the SSE server should be started.
 */
fun `run sse mcp server`(port: Int): Unit = runBlocking {
    val server = configureServer()
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp {
            server
        }
    }.start(wait = true)
}
