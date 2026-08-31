package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.theme.ClimateTraceTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

// Previews for the pieces of the UI that are worth looking at on their own: the row the country
// list is built from, the empty state it falls back to, the two figures widgets on the detail
// screen, and a specimen of the theme they all draw from.
//
// Everything here is driven by literal sample data. None of it touches Koin, the ClimateTrace API
// or the network, so each one renders from source alone — in the IDE, and in any headless renderer.

private val Ireland = Country(id = "IRL", name = "Ireland", continent = "Europe")
private val Brazil = Country(id = "BRA", name = "Brazil", continent = "South America")

private val SampleRankings = mapOf("IRL" to 42, "BRA" to 7)

private val SampleYearlyEmissions = mapOf(
    "2018" to 61_400_000.0,
    "2019" to 60_100_000.0,
    "2020" to 55_800_000.0,
    "2021" to 58_300_000.0,
    "2022" to 57_100_000.0,
    "2023" to 54_900_000.0,
)

@Preview
@Composable
fun CountryRowPreview() {
    ClimateTraceTheme {
        Surface {
            Column {
                CountryRow(
                    country = Ireland,
                    selectedCountry = null,
                    countrySelected = {},
                    rankings = SampleRankings,
                )
                CountryRow(
                    country = Brazil,
                    selectedCountry = null,
                    countrySelected = {},
                    rankings = SampleRankings,
                )
            }
        }
    }
}

@Preview
@Composable
fun CountryRowSelectedPreview() {
    ClimateTraceTheme {
        Surface {
            CountryRow(
                country = Ireland,
                selectedCountry = Ireland,
                countrySelected = {},
                rankings = SampleRankings,
            )
        }
    }
}

@Preview
@Composable
fun CountryRowUnrankedPreview() {
    // No entry in `rankings`, which is what the row shows before the ranking data has loaded.
    ClimateTraceTheme {
        Surface {
            CountryRow(
                country = Ireland,
                selectedCountry = null,
                countrySelected = {},
            )
        }
    }
}

@Preview
@Composable
fun EmptyStatePreview() {
    ClimateTraceTheme {
        Surface {
            EmptyState()
        }
    }
}

@Preview
@Composable
fun EmptyStateWithMessagePreview() {
    ClimateTraceTheme {
        Surface {
            EmptyState(
                title = "No matches",
                message = "No country matched that search.",
            )
        }
    }
}

@Preview
@Composable
fun KeyFiguresRowPreview() {
    ClimateTraceTheme {
        Surface {
            KeyFiguresRow(
                co2Value = "54.9 Mt",
                co2PercentChange = -3.9,
                rank = 42,
                share = "0.15%",
                perCapita = "10.4 t",
                perCapitaRank = 28,
            )
        }
    }
}

@Preview
@Composable
fun KeyFiguresRowRisingPreview() {
    // The other side of the delta: a rise rather than a fall, and no per-capita ranking yet.
    ClimateTraceTheme {
        Surface {
            KeyFiguresRow(
                co2Value = "58.3 Mt",
                co2PercentChange = 4.5,
                rank = 40,
                share = "0.16%",
                perCapita = "11.1 t",
                perCapitaRank = null,
            )
        }
    }
}

@Preview
@Composable
fun CO2TrendSparklinePreview() {
    ClimateTraceTheme {
        Surface {
            CO2TrendSparkline(
                yearlyEmissions = SampleYearlyEmissions,
                selectedYear = "2023",
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Theme catalog
// ---------------------------------------------------------------------------

@Composable
private fun Swatch(name: String, color: Color, onColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = color,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.size(40.dp),
        ) {
            Text(
                text = "Aa",
                color = onColor,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(8.dp),
            )
        }
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColorSchemeSpecimen() {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Colour scheme", style = MaterialTheme.typography.titleMedium)
        Swatch("primary", scheme.primary, scheme.onPrimary)
        Swatch("primaryContainer", scheme.primaryContainer, scheme.onPrimaryContainer)
        Swatch("secondary", scheme.secondary, scheme.onSecondary)
        Swatch("secondaryContainer", scheme.secondaryContainer, scheme.onSecondaryContainer)
        Swatch("tertiary", scheme.tertiary, scheme.onTertiary)
        Swatch("surface", scheme.surface, scheme.onSurface)
        Swatch("surfaceVariant", scheme.surfaceVariant, scheme.onSurfaceVariant)
        Swatch("error", scheme.error, scheme.onError)
    }
}

@Preview
@Composable
fun ColorSchemeLightPreview() {
    ClimateTraceTheme(useDarkTheme = false) {
        Surface { ColorSchemeSpecimen() }
    }
}

@Preview
@Composable
fun ColorSchemeDarkPreview() {
    ClimateTraceTheme(useDarkTheme = true) {
        Surface { ColorSchemeSpecimen() }
    }
}

@Composable
private fun TypeSample(name: String, style: TextStyle) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(name, style = MaterialTheme.typography.labelSmall)
        Text("Emissions by country", style = style)
    }
}

@Preview
@Composable
fun TypographySpecimenPreview() {
    ClimateTraceTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                val type = MaterialTheme.typography
                TypeSample("displaySmall", type.displaySmall)
                TypeSample("headlineMedium", type.headlineMedium)
                TypeSample("titleLarge", type.titleLarge)
                TypeSample("titleMedium", type.titleMedium)
                TypeSample("bodyLarge", type.bodyLarge)
                TypeSample("bodyMedium", type.bodyMedium)
                TypeSample("labelLarge", type.labelLarge)
                TypeSample("labelSmall", type.labelSmall)
            }
        }
    }
}
