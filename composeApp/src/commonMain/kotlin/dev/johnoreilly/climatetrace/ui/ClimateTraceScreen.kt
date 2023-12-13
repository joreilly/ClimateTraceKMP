package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.johnoreilly.climatetrace.ktx.performAsyncOperation
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


@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ClimateTraceScreen() {
    val windowSizeClass = calculateWindowSizeClass()
    val climateTraceApi = remember { ClimateTraceApi() }

    var countryList by remember { mutableStateOf(emptyList<Country>()) }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var countryEmissionInfo by remember { mutableStateOf<CountryEmissionsInfo?>(null) }
    var countryAssetEmissions by remember { mutableStateOf<List<CountryAssetEmissionsInfo>?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(true) {
        performAsyncOperation(
            isLoadingState = isLoading,
            operation = { climateTraceApi.fetchCountries().sortedBy { it.name } },
            onSuccess = { countries -> countryList = countries }
        )
    }

    LaunchedEffect(selectedCountry) {
        selectedCountry?.let { country ->
            val countryEmissionInfoList = climateTraceApi.fetchCountryEmissionsInfo(country.alpha3)
            countryEmissionInfo = countryEmissionInfoList.firstOrNull()
            countryAssetEmissions =
                climateTraceApi.fetchCountryAssetEmissionsInfo(country.alpha3)[country.alpha3]
        }
    }

    Row(Modifier.fillMaxSize()) {

        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
            Column(Modifier.fillMaxWidth()) {

                Box(Modifier.height(250.dp).fillMaxWidth().background(color = Color.LightGray)) {
                    CountryListView(countryList, selectedCountry, isLoading.value) { country ->
                        selectedCountry = country
                    }
                }

                Spacer(modifier = Modifier.width(1.dp).fillMaxWidth())
                selectedCountry?.let { country ->
                    CountryInfoDetailedView(country, countryEmissionInfo, countryAssetEmissions)
                }
            }
        } else {
            Box(Modifier.width(250.dp).fillMaxHeight().background(color = Color.LightGray)) {
                CountryListView(countryList, selectedCountry, isLoading.value) { country ->
                    selectedCountry = country
                }
            }

            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight())
            Box(Modifier.fillMaxHeight()) {
                selectedCountry?.let { country ->
                    CountryInfoDetailedView(country, countryEmissionInfo, countryAssetEmissions)
                }
            }
        }
    }
}


@Composable
fun CountryListView(
    countryList: List<Country>,
    selectedCountry: Country?,
    isLoading: Boolean,
    countrySelected: (country: Country) -> Unit
) {
    val searchQuery = remember { mutableStateOf("") }

    if (isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxHeight()
                .wrapContentSize(Alignment.Center)
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column {
            SearchableList(
                isLoading = isLoading,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> searchQuery.value = query },
                countryList = countryList,
                selectedCountry = selectedCountry,
                countrySelected = countrySelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchableList(
    isLoading: Boolean,
    searchQuery: MutableState<String>,
    onSearchQueryChange: (String) -> Unit,
    countryList: List<Country>,
    selectedCountry: Country?,
    countrySelected: (country: Country) -> Unit
) {
    val filteredCountryList = countryList.filter {
        it.name.contains(searchQuery.value, ignoreCase = true)
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    SearchBar(
        query = searchQuery.value,
        onQueryChange = onSearchQueryChange,
        onSearch = {
            onSearchQueryChange.invoke(searchQuery.value)
            keyboardController?.hide()
        },
        placeholder = {
            Text(text = "Search countries")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = "search"
            )
        },
        trailingIcon = {
            if (searchQuery.value.isNotEmpty() && isLoading.not()) {
                IconButton(onClick = {
                    onSearchQueryChange("")
                    keyboardController?.hide()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "clear_search"
                    )
                }
            }
        },
        content = {
            if (filteredCountryList.isEmpty() && isLoading.not()) {
                CountryListEmptyState()
            } else {
                LazyColumn {
                    items(filteredCountryList) { country ->
                        CountryRow(country, selectedCountry, countrySelected)
                    }
                }
            }
        },
        active = true,
        onActiveChange = {},
        tonalElevation = 0.dp
    )
}

@Composable
fun CountryListEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxHeight()
            .wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No Countries Found!", style = MaterialTheme.typography.titleMedium)
        Text("search differently", style = MaterialTheme.typography.bodyLarge)
    }
}


@Composable
fun CountryRow(
    country: Country,
    selectedCountry: Country?,
    countrySelected: (country: Country) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { countrySelected(country) })
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = country.name,
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
        Text(
            text = country.name,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(16.dp))

        countryEmissionInfo?.let {
            val co2 = (countryEmissionInfo.emissions.co2 / 1_000_000).toInt()
            val percentage = (countryEmissionInfo.emissions.co2 / countryEmissionInfo.worldEmissions.co2).toPercent(2)
            Text("co2 = $co2 Million Tonnes (2022)")
            Text("rank = ${countryEmissionInfo.rank} ($percentage)")
        }

        Spacer(modifier = Modifier.size(16.dp))

        countryAssetEmissionsList?.let {
            SectorEmissionsPieChart(countryAssetEmissionsList)
        }
    }
}

private fun Float.toPercent(precision: Int): String = "${(this * 100.0f).toString(precision)}%"

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
    val filteredEmissionsList = assetEmissionsInfoList
        .filter { it.emissions > 0 }
        .sortedByDescending { it.emissions }
        .take(10)
    val values = filteredEmissionsList.map { it.emissions / 1_000_000 }
    val labels = filteredEmissionsList.map { it.sector }
    val total = values.sum()
    val colors = generateHueColorPalette(values.size)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PieChart(
            values = values,
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

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            FlowLegend(
                itemCount = labels.size,
                symbol = { i ->
                    Symbol(modifier = Modifier.size(8.dp), fillBrush = SolidColor(colors[i]))
                },
                label = { i ->
                    Text(labels[i])
                },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
