import io.modelcontextprotocol.kotlin.sdk.server.RegisteredResource
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Resource
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.security.MessageDigest

// MCP Apps (https://modelcontextprotocol.github.io/ext-apps/): a tool whose _meta names a ui://
// resource gets that resource's HTML rendered in a sandboxed iframe by hosts that support it,
// such as Claude. Hosts without MCP Apps support just see an ordinary tool returning text.
// The keys here are the ones the TypeScript SDK's registerAppTool()/registerAppResource() write.

const val DASHBOARD_URI = "ui://climate-trace/emissions-dashboard.html"
private const val MCP_APP_MIME_TYPE = "text/html;profile=mcp-app"

// The view imports the MCP Apps SDK from this CDN, so the sandbox's CSP has to allow it.
private const val SDK_ORIGIN = "https://cdn.jsdelivr.net"

/**
 * Claude serves each server's views from a dedicated origin: the first 32 hex characters of the
 * SHA-256 of the server's public MCP URL (e.g. "https://example.com/mcp"), under claudemcpcontent.com.
 */
fun claudeSandboxDomain(serverUrl: String): String =
    MessageDigest.getInstance("SHA-256").digest(serverUrl.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(32) + ".claudemcpcontent.com"

/** `_meta` for a tool whose result the dashboard view renders. */
fun dashboardToolMeta(): JsonObject = buildJsonObject {
    putJsonObject("ui") { put("resourceUri", DASHBOARD_URI) }
    put("ui/resourceUri", DASHBOARD_URI) // older hosts read the flat key
}

/**
 * `_meta` for the view resource. [serverUrl] is the server's public URL, when it is reachable
 * remotely; without it the host picks a default sandbox origin.
 */
fun dashboardResourceMeta(serverUrl: String?): JsonObject = buildJsonObject {
    putJsonObject("ui") {
        putJsonObject("csp") { putJsonArray("resourceDomains") { add(SDK_ORIGIN) } }
        put("prefersBorder", true)
        if (!serverUrl.isNullOrBlank()) put("domain", claudeSandboxDomain(serverUrl))
    }
}

private object DashboardHtml {
    val text: String by lazy {
        checkNotNull(javaClass.getResource("/emissions-dashboard.html")) { "emissions-dashboard.html missing from resources" }.readText()
    }
}

/** Registers the dashboard view as a ui:// resource. The tool that shows it is in McpServer.kt. */
fun Server.addEmissionsDashboardResource(serverUrl: String?) {
    val meta = dashboardResourceMeta(serverUrl)
    val resource = Resource(
        uri = DASHBOARD_URI,
        name = "emissions-dashboard",
        title = "Emissions dashboard",
        description = "Interactive charts of a country's emissions by sector and by month.",
        mimeType = MCP_APP_MIME_TYPE,
        meta = meta,
    )
    addResources(
        listOf(
            RegisteredResource(resource) {
                // Hosts read the CSP and domain from the contents first, the listing second.
                ReadResourceResult(
                    contents = listOf(TextResourceContents(DashboardHtml.text, DASHBOARD_URI, MCP_APP_MIME_TYPE, meta))
                )
            }
        )
    )
}
