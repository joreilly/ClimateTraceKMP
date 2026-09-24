package dev.johnoreilly.climatetrace.agent

import android.content.Context
import android.content.Intent
import android.icu.text.MessageFormat
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.processor.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import dev.johnoreilly.climatetrace.ui.EqualHeightRow
import dev.johnoreilly.climatetrace.ui.climateTraceA2uiComponents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Renders A2UI surfaces with the Material 3 Basic Catalog (`androidx.compose.material3:material3-a2ui`)
 * plus ClimateTrace's own components ([climateTraceA2uiComponents]).
 */
class AndroidA2uiRenderer(context: Context) : A2uiRenderer {

    private val basicCatalog = materialA2uiBasicCatalogV1(
        image = MaterialA2uiBasicCatalogV1Defaults.image { url, contentDescription, contentScale, modifier, onError ->
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                onError = { onError(it.result.throwable) },
            )
        },
        // The agent has no media to show, so video/audio just show their URL.
        video = MaterialA2uiBasicCatalogV1Defaults.video { url, modifier, _ -> Text("Video: $url", modifier) },
        audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { url, _, modifier, _ ->
            Text("Audio: $url", modifier)
        },
        urlOpener = { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        messageFormatter = { pattern, locale, arguments -> MessageFormat(pattern, locale).format(arguments) },
        localeProvider = A2uiLocaleProvider.Default,
        row = EqualHeightRow,
    )

    // A custom catalog needs its own id: the agent can't assume the standard basic catalog id
    // includes CountryFlag/EmissionsChart. Fine here as the agent reads this schema directly.
    private val catalog = A2uiCatalog(
        catalogId = "https://github.com/joreilly/ClimateTraceKMP/a2ui/catalog/v1",
        components = basicCatalog.components + climateTraceA2uiComponents,
        functions = basicCatalog.functions,
        themeSchema = basicCatalog.themeSchema,
    )

    private val processor: A2uiMessageProcessor = A2uiMessageProcessor(catalogs = listOf(catalog))
    private val parser = A2uiMessageParser()

    // Lives as long as the app: the renderer is a Koin singleton.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch { processor.collectMessages() }
    }

    override val isSupported = true
    override val catalogId: String = catalog.id
    override val catalogSchema: String by lazy { catalog.toJsonSchemaString() }

    override val surfaceIds: StateFlow<List<String>> = processor.activeSurfaces
        .map { surfaces -> surfaces.map { it.id } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val events: Flow<A2uiEvent> = processor.outboundEvents.map { message ->
        when (message) {
            is A2uiClientEventMessage -> A2uiEvent.UserAction(
                "${message.type} on '${message.componentId}' (surface ${message.surfaceId}) ${message.context}"
            )
            is A2uiClientErrorMessage -> A2uiEvent.Error(
                "A2UI ${message.code} (surface ${message.surfaceId}): ${message.message} ${message.context}"
            )
        }
    }

    // Parse errors are passed to processError() by processInput() and come back on outboundEvents,
    // along with errors found later when the components are validated against the catalog.
    override fun process(messageJson: String) = processor.processInput(parser, messageJson)

    override fun reportError(message: String) = processor.processError(
        A2uiClientErrorMessage(
            code = "VALIDATION_FAILED",
            surfaceId = GLOBAL_SURFACE_ID,
            message = message,
            context = mapOf("path" to "/"),
        )
    )

    private companion object {
        // Same id the library uses for errors that aren't tied to a surface.
        const val GLOBAL_SURFACE_ID = "__global__"
    }

    @Composable
    override fun Surface(surfaceId: String, modifier: Modifier) {
        val surfaces by processor.activeSurfaces.collectAsState()
        val surface = surfaces.firstOrNull { it.id == surfaceId } ?: return
        A2uiSurface(surfaceModel = surface, modifier = modifier.fillMaxWidth())
    }
}
