package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        horizontalAlignment = Alignment.Start
    ) {
        // Header card with flag + country label info
        CountryHeader(viewState)

        Spacer(modifier = Modifier.size(16.dp))

        val year = viewState.year
        val countryAssetEmissionsList = viewState.countryAssetEmissionsList
        val countryEmissionInfo = viewState.countryEmissionInfo

        // Year selector row
        Column {
            Text(text = "Year", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.size(6.dp))
            YearSelector(year, viewState.availableYears, onYearSelected)
        }

        Spacer(modifier = Modifier.size(12.dp))

        countryEmissionInfo?.let {
            val co2 = (countryEmissionInfo.emissions.co2 / 1_000_000).toInt()
            val percentage = (countryEmissionInfo.emissions.co2 / countryEmissionInfo.worldEmissions.co2).toPercent(2)

            // Key figures chips
            KeyFiguresRow(co2Mt = co2, rank = countryEmissionInfo.rank, share = percentage)

            Spacer(modifier = Modifier.size(16.dp))

            val filteredCountryAssetEmissionsList = countryAssetEmissionsList.filter { it.sector != null }
            if (filteredCountryAssetEmissionsList.isNotEmpty()) {
                // Keep charts unchanged
                SectorEmissionsPieChart(countryAssetEmissionsList)
                Spacer(modifier = Modifier.size(32.dp))
                CountryAssetEmissionsInfoTreeMapChart(countryAssetEmissionsList)
            } else {
                Spacer(modifier = Modifier.size(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Invalid data",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.Red),
                        textAlign = TextAlign.Center
                    )
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

