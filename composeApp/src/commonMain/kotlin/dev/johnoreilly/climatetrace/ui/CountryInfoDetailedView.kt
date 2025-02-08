package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

//        Text(
//            text = viewState.country.name,
//            style = MaterialTheme.typography.titleLarge,
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.size(16.dp))

        val year = viewState.year
        val countryAssetEmissionsList = viewState.countryAssetEmissionsList
        val countryEmissionInfo = viewState.countryEmissionInfo

        YearSelector(year, onYearSelected)
        countryEmissionInfo?.let {
            val co2 = (countryEmissionInfo.emissions.co2 / 1_000_000).toInt()
            val percentage =
                (countryEmissionInfo.emissions.co2 / countryEmissionInfo.worldEmissions.co2).toPercent(2)

            Text(text = "co2 = $co2 Million Tonnes ($year)")
            Text(text = "rank = ${countryEmissionInfo.rank} ($percentage)")

            Spacer(modifier = Modifier.size(16.dp))

            val filteredCountryAssetEmissionsList =
                countryAssetEmissionsList.filter { it.sector != null }
            if (filteredCountryAssetEmissionsList.isNotEmpty()) {
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
fun YearSelector(selectedYear: String, onYearSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf("2021", "2022")

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Text(selectedYear, modifier = Modifier.clickable(onClick = { expanded = true }))
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { year ->
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

