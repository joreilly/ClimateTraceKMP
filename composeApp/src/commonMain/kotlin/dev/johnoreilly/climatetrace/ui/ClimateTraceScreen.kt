package dev.johnoreilly.climatetrace.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.carlsen.flagkit.FlagKit
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.utils.PanelState
import dev.johnoreilly.climatetrace.ui.utils.ResizablePanel
import dev.johnoreilly.climatetrace.ui.utils.alpha3ToAlpha2
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsViewModel
import dev.johnoreilly.climatetrace.viewmodel.CountryListUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import org.koin.compose.koinInject


class ClimateTraceScreen: Screen {
    @Composable
    override fun Content() {
        val countryListViewModel = koinInject<CountryListViewModel>()
        val countryListViewState by countryListViewModel.viewState.collectAsState()

        Column(Modifier) {
            when (val state = countryListViewState) {
                is CountryListUIState.Loading -> {
                    Column(modifier = Modifier.fillMaxSize().fillMaxHeight().wrapContentSize(Alignment.Center)) {
                        CircularProgressIndicator()
                    }
                }
                is CountryListUIState.Error -> {
                    Text("Error")
                }
                is CountryListUIState.Success -> {
                    CountryScreenSuccess(state.countryList, state.rankings, state.perCapitaRankings)
                }
            }
        }
    }
}

@Composable
fun CountryScreenSuccess(
    countryList: List<Country>,
    rankings: Map<String, Int> = emptyMap(),
    perCapitaRankings: Map<String, Int> = emptyMap()
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val windowSizeClass = windowAdaptiveInfo.windowSizeClass

    if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) {
        CompactCountryLayout(countryList = countryList, rankings = rankings, perCapitaRankings = perCapitaRankings)
    } else {
        ExpandedCountryLayout(countryList = countryList, rankings = rankings, perCapitaRankings = perCapitaRankings)
    }
}

@Composable
private fun CompactCountryLayout(
    countryList: List<Country>,
    rankings: Map<String, Int>,
    perCapitaRankings: Map<String, Int>
) {
    val navigator = LocalNavigator.current
    Column(Modifier.fillMaxSize()) {
        CountryListView(
            countryList = countryList,
            selectedCountry = null,
            countrySelected = { country ->
                navigator?.push(CountryEmissionsScreen(country, perCapitaRankings[country.id]))
            },
            rankings = rankings
        )
    }
}

@Composable
private fun ExpandedCountryLayout(
    countryList: List<Country>,
    rankings: Map<String, Int>,
    perCapitaRankings: Map<String, Int>
) {
    val countryDetailsViewModel = koinInject<CountryDetailsViewModel>()
    val countryDetailsViewState by countryDetailsViewModel.viewState.collectAsState()
    var selectedCountry by remember { mutableStateOf<Country?>(null) }

    val panelState = remember { PanelState() }
    val animatedSize = if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize

    Row(Modifier.fillMaxSize()) {
        ResizablePanel(
            Modifier.width(animatedSize).fillMaxHeight(),
            title = "Countries",
            state = panelState
        ) {
            CountryListView(
                countryList = countryList,
                selectedCountry = selectedCountry,
                countrySelected = { country ->
                    selectedCountry = country
                    countryDetailsViewModel.setCountry(country)
                },
                rankings = rankings
            )
        }

        VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.fillMaxHeight()) {
            CountryInfoDetailedView(
                viewState = countryDetailsViewState,
                perCapitaRank = selectedCountry?.let { perCapitaRankings[it.id] },
                onYearSelected = { countryDetailsViewModel.setYear(it) }
            )
        }
    }
}


@Composable
fun CountryListView(
    countryList: List<Country>,
    selectedCountry: Country?,
    countrySelected: (country: Country) -> Unit,
    rankings: Map<String, Int> = emptyMap()
) {
    val searchQuery = remember { mutableStateOf("") }

    Column {
        SearchableList(
            searchQuery = searchQuery,
            onSearchQueryChange = { query -> searchQuery.value = query },
            countryList = countryList,
            selectedCountry = selectedCountry,
            countrySelected = countrySelected,
            rankings = rankings
        )
    }
}

enum class CountrySort { Name, Rank }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableList(
    searchQuery: MutableState<String>,
    onSearchQueryChange: (String) -> Unit,
    countryList: List<Country>,
    selectedCountry: Country?,
    countrySelected: (country: Country) -> Unit,
    rankings: Map<String, Int> = emptyMap()
) {
    var sortMode by remember { mutableStateOf(CountrySort.Name) }
    val filteredCountryList = countryList
        .filter { it.name.contains(searchQuery.value, ignoreCase = true) || it.id.contains(searchQuery.value, true) }
        .let { list ->
            when (sortMode) {
                CountrySort.Name -> list.sortedBy { it.name }
                CountrySort.Rank -> list.sortedBy { rankings[it.id] ?: Int.MAX_VALUE }
            }
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
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortMode == CountrySort.Name,
                    onClick = { sortMode = CountrySort.Name },
                    label = { Text("Name") }
                )
                FilterChip(
                    selected = sortMode == CountrySort.Rank,
                    onClick = { sortMode = CountrySort.Rank },
                    label = { Text("Rank") }
                )
            }
            if (filteredCountryList.isEmpty()) {
                EmptyState(message = "")
            } else {
                LazyColumn {
                    items(filteredCountryList) { country ->
                        CountryRow(
                            country = country,
                            selectedCountry = selectedCountry,
                            countrySelected = countrySelected,
                            rankings = rankings
                        )
                    }
                }
            }
        },
        active = true,
        onActiveChange = {},
        colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
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
    countrySelected: (country: Country) -> Unit,
    rankings: Map<String, Int> = emptyMap()
) {
    val rank = rankings[country.id]
    val isSelected = country.id == selectedCountry?.id
    val rowBackground = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val primaryTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }
    val rankBadgeColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBackground)
                .clickable(onClick = { countrySelected(country) })
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (rank != null) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = rankBadgeColor
                    )
                }
            }

            // Flag
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 22.dp)
                    .clip(RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                val flag = alpha3ToAlpha2(country.id)?.let { FlagKit.getFlag(it) }
                flag?.let {
                    Image(
                        imageVector = it,
                        contentDescription = country.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = country.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = primaryTextColor
                )
                Text(
                    text = "${country.continent} • ${country.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }
        }
        HorizontalDivider()
    }
}
