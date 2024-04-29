package dev.johnoreilly.climatetrace.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.utils.PanelState
import dev.johnoreilly.climatetrace.ui.utils.ResizablePanel
import dev.johnoreilly.climatetrace.viewmodel.ClimateTraceViewModel
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ClimateTraceScreen: Screen {
    @Composable
    override fun Content() {
        val windowSizeClass = calculateWindowSizeClass()
        val viewModel = koinInject<ClimateTraceViewModel>()

        val countryList by viewModel.countryList.collectAsState()
        val selectedCountry by viewModel.selectedCountry.collectAsState()
        val countryEmissionInfo by viewModel.countryEmissionInfo.collectAsState()
        val countryAssetEmissions by viewModel.countryAssetEmissions.collectAsState()

        val isLoadingCountries by viewModel.isLoadingCountries.collectAsState()
        val isLoadingCountryDetails by viewModel.isLoadingCountryDetails.collectAsState()


        val panelState = remember { PanelState() }

        val animatedSize = if (panelState.splitter.isResizing) {
            if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize
        } else {
            animateDpAsState(
                if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize,
                SpringSpec(stiffness = Spring.StiffnessLow)
            ).value
        }


        Row(Modifier.fillMaxSize()) {

            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                Column(Modifier.fillMaxWidth()) {

                    Box(
                        Modifier.height(250.dp).fillMaxWidth().background(color = Color.LightGray)
                    ) {
                        CountryListView(
                            countryList = countryList,
                            selectedCountry = selectedCountry,
                            isLoading = isLoadingCountries
                        ) { country ->
                            viewModel.fetchCountryDetails(country)
                        }
                    }

                    Spacer(modifier = Modifier.width(1.dp).fillMaxWidth())
                    CountryInfoDetailedView(
                        country = selectedCountry,
                        year = viewModel.year,
                        countryEmissionInfo = countryEmissionInfo,
                        countryAssetEmissionsList = countryAssetEmissions,
                        isLoading = isLoadingCountryDetails
                    )
                }
            } else {

                ResizablePanel(
                    Modifier.width(animatedSize).fillMaxHeight(),
                    title = "Countries",
                    state = panelState
                ) {
                    CountryListView(
                        countryList = countryList,
                        selectedCountry = selectedCountry,
                        isLoading = isLoadingCountries
                    ) { country ->
                        viewModel.fetchCountryDetails(country)
                    }
                }

                VerticalDivider(thickness = 1.dp, color = Color.DarkGray)
                Box(Modifier.fillMaxHeight()) {
                    CountryInfoDetailedView(
                        country = viewModel.selectedCountry.value,
                        year = viewModel.year,
                        countryEmissionInfo = countryEmissionInfo,
                        countryAssetEmissionsList = countryAssetEmissions,
                        isLoading = isLoadingCountryDetails
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
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
            AnimatedVisibility(
                visible = searchQuery.value.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(onClick = {
                    onSearchQueryChange("")
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
                EmptyState(message = "")
            } else {
                LazyColumn {
                    items(filteredCountryList) { country ->
                        CountryRow(
                            country = country,
                            selectedCountry = selectedCountry,
                            countrySelected = countrySelected
                        )
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
fun EmptyState(
    title: String? = null,
    message: String? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title ?: "No Countries Found!", style = MaterialTheme.typography.titleMedium)
        message?.let {
            Text(message, style = MaterialTheme.typography.bodyLarge)
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


