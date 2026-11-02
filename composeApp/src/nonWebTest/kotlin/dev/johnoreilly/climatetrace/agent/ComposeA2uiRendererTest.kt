package dev.johnoreilly.climatetrace.agent

import ai.littlepress.a2ui.compat.android.content.Context
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * Renders a surface the way the agent sends one, on desktop and iOS (where A2UI comes from
 * a2ui-kmp rather than Google's Android artifacts), including ClimateTrace's own EmissionsChart.
 */
@OptIn(ExperimentalTestApi::class)
class ComposeA2uiRendererTest {

    @Test
    fun rendersBasicCatalogAndClimateTraceComponents() = runComposeUiTest {
        val renderer = ComposeA2uiRenderer(A2uiBasicCatalogV1.Functions(Context()))

        renderer.process(
            """{"version":"v0.9","createSurface":{"surfaceId":"s1","catalogId":"${renderer.catalogId}"}}"""
        )
        renderer.process(
            """
            {"version":"v0.9","updateComponents":{"surfaceId":"s1","components":[
              {"id":"root","component":"Column","children":["title","chart"]},
              {"id":"title","component":"Text","text":"Emissions 2023"},
              {"id":"chart","component":"EmissionsChart","unit":"Mt CO2e","bars":[
                {"label":"Japan","value":1263.4,"countryCode":"JPN"},
                {"label":"Germany","value":690.0,"countryCode":"DEU"}
              ]}
            ]}}
            """.trimIndent()
        )

        setContent {
            MaterialTheme {
                val surfaceIds by renderer.surfaceIds.collectAsState()
                Column {
                    surfaceIds.forEach { renderer.Surface(it, Modifier) }
                }
            }
        }

        // Messages are processed on a background coroutine.
        waitUntil(timeoutMillis = 10_000) {
            onAllNodes(androidx.compose.ui.test.hasText("1,263.4 Mt CO2e")).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Emissions 2023").assertIsDisplayed()
        onNodeWithText("Germany").assertIsDisplayed()
        onNodeWithText("690.0 Mt CO2e").assertIsDisplayed()
    }
}
