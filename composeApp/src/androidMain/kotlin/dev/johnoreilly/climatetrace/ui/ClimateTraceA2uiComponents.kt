package dev.johnoreilly.climatetrace.ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.a2ui.A2uiSurfaceDefaults
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.runtime.key
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.carlsen.flagkit.FlagKit
import dev.johnoreilly.climatetrace.ui.utils.alpha3ToAlpha2

/**
 * ClimateTrace-specific A2UI components, added alongside the Material 3 Basic Catalog. Their
 * descriptions and properties end up in the catalog JSON Schema the agent is given.
 */
val climateTraceA2uiComponents: List<A2uiComponent> = listOf(CountryFlagComponent, EmissionsChartComponent)

/** A country's flag, drawn with the same FlagKit flags used elsewhere in the app. */
object CountryFlagComponent : A2uiComponent {
    private val countryCode = A2uiProperty.string(
        "countryCode",
        required = true,
        description = "ISO 3166-1 alpha-3 country code, e.g. 'DEU'."
    )

    override val name = "CountryFlag"
    override val description =
        "Shows the national flag of a country. Use it next to a country's name, e.g. in a card title " +
            "Row with \"align\": \"center\"."
    override val properties = listOf(countryCode)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        FlagImage(properties[countryCode].orEmpty(), modifier.size(width = 32.dp, height = 22.dp))
    }
}

/** Horizontal bar chart, styled like the Rankings screen's rows. */
object EmissionsChartComponent : A2uiComponent {
    private val title = A2uiProperty.string("title", description = "Optional chart title.")
    private val unit = A2uiProperty.string("unit", description = "Unit shown after each value, e.g. 'Mt CO2e'.")
    private val label = A2uiProperty.string("label", required = true, description = "Bar label, e.g. a country name.")
    private val value = A2uiProperty.number("value", required = true, description = "Bar value (numbers, not strings).")
    private val countryCode = A2uiProperty.string(
        "countryCode",
        description = "Optional ISO 3166-1 alpha-3 code; shows the country's flag next to the label."
    )
    private val bars = A2uiProperty.nestedList(
        "bars",
        properties = listOf(label, value, countryCode),
        required = true,
        description = "The bars, in the order to show them.",
        minItems = 1,
    )

    override val name = "EmissionsChart"
    override val description =
        "Horizontal bar chart for comparing numeric values such as emissions across countries or years. " +
            "Prefer it over a list of Text components when comparing two or more values."
    override val properties = listOf(title, unit, bars)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val rows = properties[bars].orEmpty().map { bar ->
            Bar(
                label = bar[label].orEmpty(),
                value = bar[value]?.toDouble() ?: 0.0,
                countryCode = bar[countryCode],
            )
        }
        val max = rows.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
        val unitText = properties[unit]?.let { " $it" }.orEmpty()

        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            properties[title]?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
            rows.forEach { bar ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bar.countryCode != null) {
                        FlagImage(bar.countryCode, Modifier.size(width = 24.dp, height = 16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        bar.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(120.dp)
                    )
                    Box(Modifier.weight(1f).height(20.dp)) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((bar.value / max).toFloat().coerceIn(0.01f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "%,.1f".format(bar.value) + unitText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(min = 72.dp)
                    )
                }
            }
        }
    }

    private class Bar(val label: String, val value: Double, val countryCode: String?)
}

@Composable
private fun FlagImage(alpha3Code: String, modifier: Modifier) {
    val flag = alpha3ToAlpha2(alpha3Code)?.let { FlagKit.getFlag(it) } ?: return
    Box(modifier.clip(RoundedCornerShape(3.dp))) {
        Image(imageVector = flag, contentDescription = alpha3Code, modifier = Modifier.fillMaxSize())
    }
}

/**
 * The basic catalog's Row, except that `"align": "stretch"` actually gives the children (e.g. a row
 * of Cards) the same height. The library's Row applies fillMaxHeight() for stretch, which does
 * nothing in the chat's scrolling list (the height is unbounded), and the Card inside each child
 * isn't given the height either. Here the Row is measured at its intrinsic height and each child
 * component is told to fill it. Other alignments use the library's Row unchanged.
 */
object EqualHeightRow : A2uiBasicCatalogV1.Row {
    private val materialRow = MaterialA2uiBasicCatalogV1Defaults.row

    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        justify: A2uiBasicCatalogV1.Row.Justify,
        align: A2uiBasicCatalogV1.Row.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        if (align != A2uiBasicCatalogV1.Row.Align.Stretch) {
            with(materialRow) { TypedContent(children, justify, align, accessibility, modifier) }
            return
        }

        val description = listOfNotNull(accessibility?.label, accessibility?.description)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        Row(
            modifier = modifier
                .height(IntrinsicSize.Min)
                .then(if (description.isEmpty()) Modifier else Modifier.semantics { contentDescription = description }),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            children.forEach { reference ->
                key(reference.id, reference.baseDataPath) {
                    when (val state = observeA2uiComponentState(reference)) {
                        is A2uiComponentState.Success -> {
                            val weight = state.component.properties[A2uiBasicCatalogV1.WeightProperty]?.toFloat()
                                ?: if (justify == A2uiBasicCatalogV1.Row.Justify.Stretch) 1f else null
                            A2uiComponent(
                                component = state.component,
                                modifier = (if (weight != null) Modifier.weight(weight) else Modifier).fillMaxHeight(),
                            )
                        }
                        is A2uiComponentState.Loading -> A2uiSurfaceDefaults.LoadingIndicator()
                        is A2uiComponentState.Error -> A2uiSurfaceDefaults.ErrorFallback(state.exception)
                    }
                }
            }
        }
    }
}
