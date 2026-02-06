package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.johnoreilly.climatetrace.remote.Asset
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsUIState

@Composable
fun CountryInfoDetailedView(
    viewState: CountryDetailsUIState,
    onYearSelected: (String) -> Unit
) {
    when (viewState) {
        CountryDetailsUIState.NoCountrySelected -> {
            Column(
                modifier = Modifier.fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(text = "No Country Selected.", style = MaterialTheme.typography.titleMedium)
            }
        }
        is CountryDetailsUIState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        }
        is CountryDetailsUIState.Error -> { Text("Error") }
        is CountryDetailsUIState.Success -> {
            CountryInfoDetailedViewSuccess(viewState, onYearSelected)
        }
    }
}


@Composable
fun CountryInfoDetailedViewSuccess(viewState: CountryDetailsUIState.Success, onYearSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card with flag + country label info
        CountryHeader(viewState)

        val year = viewState.year
        val countryAssetEmissionsList = viewState.countryAssetEmissionsList
        val countryEmissionInfo = viewState.countryEmissionInfo

        countryEmissionInfo?.let {
            val co2 = (countryEmissionInfo.emissions.co2 / 1_000_000).toInt()
            val percentage = (countryEmissionInfo.emissions.co2.toDouble() / countryEmissionInfo.worldEmissions.co2).toPercent(2)

            // Emissions Summary Section - combines year selector and key figures
            EmissionsSummarySection(
                year = year,
                availableYears = viewState.availableYears,
                onYearSelected = onYearSelected,
                co2Mt = co2,
                rank = countryEmissionInfo.rank,
                share = percentage
            )

            // Emissions by Sector Section
            val filteredCountryAssetEmissionsList = countryAssetEmissionsList.filter { it.sector != null }
            if (filteredCountryAssetEmissionsList.isNotEmpty()) {
                EmissionsBySectorSection(countryAssetEmissionsList)
            } else {
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No sector data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Assets Section
        if (viewState.assets.isNotEmpty()) {
            AssetsSection(viewState.assets)
        }
    }
}

@Composable
private fun EmissionsSummarySection(
    year: String,
    availableYears: List<String>,
    onYearSelected: (String) -> Unit,
    co2Mt: Int,
    rank: Int,
    share: String
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emissions Summary",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                YearSelector(year, availableYears, onYearSelected)
            }
            Spacer(modifier = Modifier.size(12.dp))
            KeyFiguresRow(co2Mt = co2Mt, rank = rank, share = share)
        }
    }
}

@Composable
private fun EmissionsBySectorSection(countryAssetEmissionsList: List<dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo>) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Emissions by Sector",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(12.dp))
            CountryAssetEmissionsInfoTreeMapChart(countryAssetEmissionsList)
        }
    }
}

@Composable
private fun AssetsSection(assets: List<Asset>) {
    val navigator = LocalNavigator.current

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Top Emission Sources",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))

            assets.forEachIndexed { index, asset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navigator?.push(AssetDetailScreen(asset.id, asset.name))
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        asset.sector?.let {
                            Text(
                                text = it.replace("-", " ").replaceFirstChar { c -> c.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val validOwners = asset.owners?.filter { !it.companyName.isNullOrBlank() }
                        if (!validOwners.isNullOrEmpty()) {
                            Text(
                                text = "Owners: ${validOwners.distinctBy { it.companyId ?: it.companyName }.joinToString { it.companyName ?: "" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index < assets.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CountryHeader(viewState: CountryDetailsUIState.Success) {
    val c = viewState.country
    androidx.compose.material3.Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = c.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "${c.continent} • ${c.alpha2} / ${c.alpha3}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KeyFiguresRow(co2Mt: Int, rank: Int, share: String) {
    Row {
        KeyFigureChip(label = "CO₂ (Mt)", value = co2Mt.toString())
        Spacer(modifier = Modifier.size(8.dp))
        KeyFigureChip(label = "Rank", value = rank.toString())
        Spacer(modifier = Modifier.size(8.dp))
        KeyFigureChip(label = "World Share", value = share)
    }
}

@Composable
private fun KeyFigureChip(label: String, value: String) {
    AssistChip(
        onClick = {},
        label = {
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(text = value, style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}



@Composable
fun YearSelector(selectedYear: String, availableYears: List<String>, onYearSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Text(selectedYear, modifier = Modifier.clickable(onClick = { expanded = true }))
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            availableYears.forEach { year ->
                DropdownMenuItem(onClick = {
                    onYearSelected(year)
                    expanded = false
                }, text = {
                    Text(year)
                })
            }
        }
    }
}

