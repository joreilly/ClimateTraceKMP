import dev.johnoreilly.climatetrace.agent.ClimateTraceReports
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.di.initKoin
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.putJsonObject




fun main(args: Array<String>) {
    val command = args.firstOrNull() ?: "--stdio"
    val port = args.getOrNull(1)?.toIntOrNull() ?: 8080
    when (command) {
        "--sse-server" -> `run sse mcp server`(port)
        "--streamable-http-server" -> `run streamable http mcp server`(port)
        "--stdio" -> `run mcp server using stdio`()
        else -> {
            System.err.println("Unknown command: $command")
        }
    }
}


private val koin = initKoin(enableNetworkLogs = true).koin

// JSON-schema fragments for the arguments the tools share.
private fun JsonObjectBuilder.countryCodeList() = putJsonObject("countryCodeList") {
    put("type", JsonPrimitive("array"))
    put("description", JsonPrimitive("ISO 3166-1 alpha-3 country codes, e.g. [\"IRL\", \"USA\"]"))
    putJsonObject("items") { put("type", JsonPrimitive("string")) }
}

private fun JsonObjectBuilder.string(name: String, description: String) = putJsonObject(name) {
    put("type", JsonPrimitive("string"))
    put("description", JsonPrimitive(description))
}

private fun JsonObjectBuilder.integer(name: String, description: String) = putJsonObject(name) {
    put("type", JsonPrimitive("integer"))
    put("description", JsonPrimitive(description))
}

private fun JsonObjectBuilder.boolean(name: String, description: String) = putJsonObject(name) {
    put("type", JsonPrimitive("boolean"))
    put("description", JsonPrimitive(description))
}

private fun JsonObjectBuilder.year() =
    string("year", "Four digit year, e.g. \"2025\". Call get-data-coverage to see which years have data.")

private fun JsonObjectBuilder.includeForestry() = boolean(
    "includeForestry",
    "Include the forestry and land use sector, giving totals net of land-use carbon sinks. " +
        "Default false, which matches the totals on climatetrace.org."
)

// Models don't always send the JSON type the schema asks for ("10" for 10, "true" for true).
private fun JsonObject?.string(name: String): String? = (this?.get(name) as? JsonPrimitive)?.contentOrNull
private fun JsonObject?.int(name: String): Int? = string(name)?.toIntOrNull()
private fun JsonObject?.boolean(name: String): Boolean = string(name)?.toBooleanStrictOrNull() ?: false
private fun JsonObject?.stringList(name: String): List<String>? = when (val value = this?.get(name)) {
    is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.takeIf { it.isNotEmpty() }
    // A single code sent as a bare string, or several comma-separated.
    is JsonPrimitive -> value.contentOrNull?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }
    else -> null
}

private fun text(value: String) = CallToolResult(content = listOf(TextContent(value)))
private fun missing(vararg names: String) = CallToolResult(
    content = listOf(TextContent("Missing or invalid required argument(s): ${names.joinToString()}.")),
    isError = true
)

fun configureMcpServer(): Server {
    val climateTraceRepository = koin.get<ClimateTraceRepository>()
    // Fetching and wording are shared with the in-app agent's tools.
    val reports = ClimateTraceReports(climateTraceRepository)

    val server = Server(
        Implementation(
            name = "ClimateTrace MCP Server",
            version = "1.1.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    server.addTool(
        name = "get-data-coverage",
        description = "Which years the Climate TRACE emissions data covers, whether the latest year is still " +
            "in progress, and the data release version. Call this before assuming a year has data."
    ) {
        text(reports.dataCoverage())
    }

    server.addTool(
        name = "get-countries",
        description = "List every country with its ISO 3166-1 alpha-3 code, which the other tools take as input."
    ) {
        text(climateTraceRepository.fetchCountries().joinToString("\n") { "${it.name}, ${it.id}" })
    }

    server.addTool(
        name = "get-emissions",
        description = "Total emissions for one or more countries in a year, with each country's world rank, " +
            "emissions per person, share of global emissions and change on the previous year.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                countryCodeList()
                year()
                includeForestry()
            },
            required = listOf("countryCodeList", "year")
        )
    ) { request ->
        val countryCodes = request.arguments.stringList("countryCodeList")
        val year = request.arguments.string("year")
        if (countryCodes == null || year == null) return@addTool missing("countryCodeList", "year")
        text(reports.countryEmissions(countryCodes, year, request.arguments.boolean("includeForestry")))
    }

    server.addTool(
        name = "get-country-asset-emissions",
        description = "Emissions broken down by sector and subsector (power, transportation, agriculture and so on) " +
            "for one or more countries in a year. Use get-emissions when only the total is needed.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                countryCodeList()
                year()
                includeForestry()
            },
            required = listOf("countryCodeList", "year")
        )
    ) { request ->
        val countryCodes = request.arguments.stringList("countryCodeList")
        val year = request.arguments.string("year")
        if (countryCodes == null || year == null) return@addTool missing("countryCodeList", "year")
        text(reports.sectorEmissions(countryCodes, year, request.arguments.boolean("includeForestry")))
    }

    server.addTool(
        name = "get-monthly-emissions",
        description = "Month-by-month emissions for one or more countries in a year. Use for questions about " +
            "months, seasons or the year in progress.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                countryCodeList()
                year()
                includeForestry()
            },
            required = listOf("countryCodeList", "year")
        )
    ) { request ->
        val countryCodes = request.arguments.stringList("countryCodeList")
        val year = request.arguments.string("year")
        if (countryCodes == null || year == null) return@addTool missing("countryCodeList", "year")
        text(reports.monthlyEmissions(countryCodes, year, request.arguments.boolean("includeForestry")))
    }

    server.addTool(
        name = "get-rankings",
        description = "The countries with the highest emissions in a year, by total emissions or by emissions per person.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                year()
                integer("limit", "How many countries to return, 1 to 50. Default 10.")
                boolean("perCapita", "Rank by emissions per person instead of total emissions. Default false.")
                includeForestry()
            },
            required = listOf("year")
        )
    ) { request ->
        val year = request.arguments.string("year") ?: return@addTool missing("year")
        text(
            reports.rankings(
                year = year,
                limit = request.arguments.int("limit") ?: 10,
                perCapita = request.arguments.boolean("perCapita"),
                includeForestry = request.arguments.boolean("includeForestry")
            )
        )
    }

    server.addTool(
        name = "get-country-sources",
        description = "The largest individual emission sources in a country (power stations, airports, factories, " +
            "mines and so on) with each one's source id, sector, emissions and location.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                string("countryCode", "ISO 3166-1 alpha-3 country code, e.g. \"IRL\"")
                string("year", "Four digit year. Leave out for the latest year.")
                integer("limit", "How many sources to return, 1 to 50. Default 10.")
                includeForestry()
            },
            required = listOf("countryCode")
        )
    ) { request ->
        val countryCode = request.arguments.string("countryCode") ?: return@addTool missing("countryCode")
        text(
            reports.countrySources(
                countryCode = countryCode,
                year = request.arguments.string("year"),
                limit = request.arguments.int("limit") ?: 10,
                includeForestry = request.arguments.boolean("includeForestry")
            )
        )
    }

    server.addTool(
        name = "get-source-details",
        description = "Details of one emission source: owners, location, yearly and monthly emissions, activity and " +
            "capacity. Takes a source id from get-country-sources.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                integer("sourceId", "Source id, as returned by get-country-sources")
            },
            required = listOf("sourceId")
        )
    ) { request ->
        val sourceId = request.arguments.int("sourceId") ?: return@addTool missing("sourceId")
        text(reports.sourceDetails(sourceId))
    }

    server.addTool(
        name = "get-population",
        description = "Get the total population for a country (use a 3 letter ISO code). get-emissions already " +
            "returns emissions per person, so this is only needed for other population questions.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                string("countryCode", "ISO 3166-1 alpha-3 country code, e.g. \"IRL\"")
            },
            required = listOf("countryCode")
        )
    ) { request ->
        val countryCode = request.arguments.string("countryCode") ?: return@addTool missing("countryCode")
        val population = climateTraceRepository.getPopulation(countryCode)
        text(population.toString())
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
    val server = configureMcpServer()
    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )

    runBlocking {
        server.createSession(transport)
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
    val server = configureMcpServer()
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp {
            server
        }
    }.start(wait = true)
}

/**
 * Launches an MCP (Model Context Protocol) server using the Streamable HTTP transport on the
 * specified port, at the default "/mcp" path.
 *
 * @param port The port number on which the server should be started.
 */
fun `run streamable http mcp server`(port: Int): Unit = runBlocking {
    val server = configureMcpServer()
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcpStreamableHttp {
            server
        }
    }.start(wait = true)
}
