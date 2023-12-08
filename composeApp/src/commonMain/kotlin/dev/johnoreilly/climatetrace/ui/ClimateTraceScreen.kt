package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.johnoreilly.climatetrace.remote.Asset
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo


@Composable
fun ClimateTraceSreen() {
    val climateTraceApi = remember { ClimateTraceApi() }

    var countryList by remember { mutableStateOf(emptyList<Country>()) }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var countryEmissionInfo by remember { mutableStateOf<CountryEmissionsInfo?>(null) }
    var countryAssets by remember { mutableStateOf<List<Asset>?>(null) }


    LaunchedEffect(true) {
        countryList = climateTraceApi.fetchCountries().sortedBy { it.name }
    }

    LaunchedEffect(selectedCountry) {
        selectedCountry?.let {
            val countryEmissionInfoList = climateTraceApi.fetchCountryEmissionsInfo(it.alpha3)
            countryEmissionInfo = countryEmissionInfoList[0]

            countryAssets = climateTraceApi.fetchCountryAssets(it.alpha3).assets
        }
    }

    Row(Modifier.fillMaxSize()) {

        Box(Modifier.width(250.dp).fillMaxHeight().background(color = Color.LightGray)) {
            CountryListView(countryList, selectedCountry) {
                selectedCountry = it
            }
        }

        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight())

        Box(Modifier.fillMaxHeight()) {
            selectedCountry?.let {
                CountryInfoDetailedView(it, countryEmissionInfo, countryAssets)
            }
        }
    }
}


@Composable
fun CountryListView(
    countryList: List<Country>,
    selectedCountry: Country?,
    countrySelected: (country: Country) -> Unit
) {

    LazyColumn {
        items(countryList) { country ->
            CountryRow(country, selectedCountry, countrySelected)
        }
    }
}





@Composable
fun CountryRow(
    country: Country,
    selectedCountry: Country?,
    countrySelected: (country: Country) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = { countrySelected(country) })
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                country.name,
                style = if (country.name == selectedCountry?.name) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun CountryInfoDetailedView(
    country: Country,
    countryEmissionInfo: CountryEmissionsInfo?,
    countryAssets: List<Asset>?
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.size(12.dp))

        Text(country.name, style = MaterialTheme.typography.titleLarge)

        countryEmissionInfo?.let {
            val co2 = (countryEmissionInfo.emissions.co2/1_000_000).toInt()

            val percentage = (100 * countryEmissionInfo.emissions.co2/countryEmissionInfo.worldEmissions.co2).toInt()
            Text("co2 = $co2 Million Tonnes")
            Text("rank = ${countryEmissionInfo.rank} ($percentage%)")
        }

        Spacer(modifier = Modifier.size(32.dp))

        countryAssets?.forEach { asset ->
            Text("${asset.name} (sector = ${asset.sector}, asset type = ${asset.assetType})")
        }
    }
}
