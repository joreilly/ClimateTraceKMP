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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
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

        when (val state = countryListViewState) {
            is CountryListUIState.Loading -> {
                Column(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Loading global emissions data…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is CountryListUIState.Error -> {
                ErrorState(message = state.message, onRetry = { countryListViewModel.refresh() })
            }
            is CountryListUIState.Success -> {
                CountryScreenSuccess(state.countryList, state.rankings, state.perCapitaRankings)
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
    var isSearching by remember { mutableStateOf(false) }

    Column {
        SearchableList(
            searchQuery = searchQuery,
            onSearchQueryChange = { query -> searchQuery.value = query },
            countryList = countryList,
            selectedCountry = selectedCountry,
            countrySelected = countrySelected,
            rankings = rankings,
            isSearching = isSearching,
            onSearchToggle = { isSearching = !isSearching },
            onClearSearch = { isSearching = false; searchQuery.value = "" }
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
    rankings: Map<String, Int> = emptyMap(),
    isSearching: Boolean = false,
    onSearchToggle: () -> Unit = {},
    onClearSearch: () -> Unit = {}
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

    // Sort chips always visible above the search bar
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

    if (isSearching) {
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
                    IconButton(onClick = { onClearSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = "clear_search"
                        )
                    }
                }
            },
            content = {
                if (filteredCountryList.isEmpty()) {
                    EmptyState(message = "No matches for \"${searchQuery.value}\"")
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
            onActiveChange = { onClearSearch() },
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
    } else {
        // Collapsed state: search button + list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSearchToggle)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search countries…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (filteredCountryList.isEmpty()) {
            EmptyState()
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
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().wrapContentSize(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Unable to Load Data",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
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
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // Trailing chevron to indicate clickability
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View details",
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider()
    }
}
