package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.ui.utils.formatEmissionsQuantity
import dev.johnoreilly.climatetrace.ui.utils.sectorIcon
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsUIState
import io.github.koalaplot.core.util.toString

// Stripped-down companion to CountryInfoDetailedView: header, key stats, year
// picker, trend, and a compact sector breakdown - no asset list or treemap.
@Composable
fun CountryBasicEmissionsView(
    viewState: CountryDetailsUIState,
    perCapitaRank: Int? = null,
    onYearSelected: (String) -> Unit = {}
) {
    when (viewState) {
        CountryDetailsUIState.NoCountrySelected -> {
            Column(Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                Text(text = "No Country Selected.", style = MaterialTheme.typography.titleMedium)
            }
        }
        is CountryDetailsUIState.Loading -> {
            Column(Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                CircularProgressIndicator()
            }
        }
        is CountryDetailsUIState.Error -> {
            Column(Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                Text(text = "Error", style = MaterialTheme.typography.titleMedium)
            }
        }
        is CountryDetailsUIState.Success -> {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CountryHeader(viewState)

                YearChips(viewState.year, viewState.availableYears, onYearSelected)

                val countryEmissionInfo = viewState.countryEmissionInfo
                if (countryEmissionInfo != null) {
                    KeyFiguresRow(
                        co2Value = formatEmissionsQuantity(countryEmissionInfo.emissionsQuantity),
                        co2PercentChange = countryEmissionInfo.emissionsPercentChange,
                        rank = countryEmissionInfo.rank,
                        share = "${countryEmissionInfo.percentage.toString(2)}%",
                        perCapita = "${countryEmissionInfo.emissionsPerCapita.toString(1)} t",
                        perCapitaRank = perCapitaRank
                    )
                } else {
                    Text(
                        text = "No emissions data available for ${viewState.year}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (viewState.yearlyEmissions.size >= 2) {
                    CO2TrendSparkline(
                        yearlyEmissions = viewState.yearlyEmissions,
                        selectedYear = viewState.year,
                        modifier = Modifier.fillMaxWidth(),
                        onYearSelected = onYearSelected
                    )
                }

                val topSectors = viewState.countryAssetEmissionsList
                    .filter { it.sector != null && it.subsector == null && it.emissionsQuantity > 0 }
                    .sortedByDescending { it.emissionsQuantity }
                    .take(5)
                if (topSectors.isNotEmpty()) {
                    TopSectorsList(topSectors)
                }
            }
        }
    }
}

@Composable
private fun TopSectorsList(sectors: List<CountryAssetEmissionsInfo>) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top Sectors",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(12.dp))
            sectors.forEachIndexed { index, sector ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = sectorIcon(sector.sector),
                                contentDescription = sector.sector,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = sector.sector?.let { prettySectorName(it) } ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.size(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatEmissionsQuantity(sector.emissionsQuantity),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${sector.percentage.toString(1)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < sectors.size - 1) {
                    Spacer(Modifier.size(2.dp))
                }
            }
        }
    }
}
