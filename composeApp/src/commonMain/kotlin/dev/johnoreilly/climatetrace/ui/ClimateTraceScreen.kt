package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.FlowLegend
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.util.toString


@Composable
fun ClimateTraceScreen() {
    val climateTraceApi = remember { ClimateTraceApi() }

    var countryList by remember { mutableStateOf(emptyList<Country>()) }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var countryEmissionInfo by remember { mutableStateOf<CountryEmissionsInfo?>(null) }
    var countryAssetEmissons by remember { mutableStateOf<List<CountryAssetEmissionsInfo>?>(null) }


    LaunchedEffect(true) {
        countryList = climateTraceApi.fetchCountries().sortedBy { it.name }
    }

    LaunchedEffect(selectedCountry) {
        selectedCountry?.let {
            val countryEmissionInfoList = climateTraceApi.fetchCountryEmissionsInfo(it.alpha3)
            countryEmissionInfo = countryEmissionInfoList[0]
            countryAssetEmissons = climateTraceApi.fetchCountryAssetEmissionsInfo(it.alpha3)[it.alpha3]
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
                CountryInfoDetailedView(it, countryEmissionInfo, countryAssetEmissons)
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
    countryAssetEmissionsList: List<CountryAssetEmissionsInfo>?
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(country.name, style = MaterialTheme.typography.titleLarge)

        countryEmissionInfo?.let {
            val co2 = (countryEmissionInfo.emissions.co2/1_000_000).toInt()
            val percentage = (countryEmissionInfo.emissions.co2/countryEmissionInfo.worldEmissions.co2).toPercent(2)
            Text("co2 = $co2 Million Tonnes")
            Text("rank = ${countryEmissionInfo.rank} ($percentage)")
        }

        Spacer(modifier = Modifier.size(16.dp))

        countryAssetEmissionsList?.let {
            SectorEmissionsPieChart(countryAssetEmissionsList)
        }
    }
}

private fun Float.toPercent(precision: Int): String {
    @Suppress("MagicNumber")
    return "${(this * 100.0f).toString(precision)}%"
}

@Composable
fun HoverSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = Color.LightGray,
        modifier = modifier.padding(8.dp)
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            content()
        }
    }
}


@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun SectorEmissionsPieChart(
    assetEmissionsInfoList: List<CountryAssetEmissionsInfo>,
    modifier: Modifier = Modifier,
) {
    val numberOfEntries = assetEmissionsInfoList.size
    val filteredEmissionsList = assetEmissionsInfoList
        .filter { it.emissions > 0 }
        .sortedByDescending { it.emissions }
        .take(10)
    val values = filteredEmissionsList.map { it.emissions }
    val labels = filteredEmissionsList.map { it.sector }
    val total = values.sum()
    val colors = generateHueColorPalette(values.size)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PieChart(
            values,
            modifier = modifier.padding(start = 8.dp),
            slice = { i: Int ->
                DefaultSlice(
                    color = colors[i],
                    hoverExpandFactor = 1.05f,
                    hoverElement = { HoverSurface { Text(values[i].toString()) } },
                )
            },
            label = { i ->
                Text((values[i] / total).toPercent(1))
            }
        )

        FlowLegend(
            labels.size,
            symbol = { i ->
                Symbol(modifier = Modifier.size(8.dp), fillBrush = SolidColor(colors[i]))
            },
            label = { i ->
                Text(labels[i])
            },
            modifier = Modifier.padding(8.dp).border(1.dp, Color.Black).padding(8.dp)
        )
    }
}
