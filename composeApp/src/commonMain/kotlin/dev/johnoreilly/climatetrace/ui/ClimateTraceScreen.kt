package dev.johnoreilly.climatetrace.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.carlsen.flagkit.FlagKit
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.utils.ConfigureImageLoader
import dev.johnoreilly.climatetrace.ui.utils.PanelState
import dev.johnoreilly.climatetrace.ui.utils.ResizablePanel
import dev.johnoreilly.climatetrace.ui.utils.alpha3ToAlpha2
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsViewModel
import dev.johnoreilly.climatetrace.viewmodel.CountryListUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC


// Public for androidApp
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
class ClimateTraceScreen: Screen {
    @Composable
    override fun Content() {
        // The Android app enters here rather than through App(), and the detail screens show maps.
        ConfigureImageLoader()
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
internal fun CountryScreenSuccess(
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
                onRetry = { countryDetailsViewModel.retry() },
                onIncludeForestryChange = { countryDetailsViewModel.setIncludeForestry(it) },
                onYearSelected = { countryDetailsViewModel.setYear(it) }
            )
        }
    }
}


internal enum class CountrySort { Name, Rank }

@Composable
internal fun CountryListView(
    countryList: List<Country>,
    selectedCountry: Country?,
    countrySelected: (country: Country) -> Unit,
    rankings: Map<String, Int> = emptyMap()
) {
    // Saveable so the query, sort order and scroll position are still there on the way back
    // from a country's detail screen.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(CountrySort.Name) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredCountryList = remember(countryList, rankings, searchQuery, sortMode) {
        val query = searchQuery.trim()
        countryList
            .filter { it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true) }
            .let { list ->
                when (sortMode) {
                    CountrySort.Name -> list.sortedBy { it.name }
                    CountrySort.Rank -> list.sortedBy { rankings[it.id] ?: Int.MAX_VALUE }
                }
            }
    }

    // A new query or sort order is a new list: show it from the top rather than wherever the
    // previous one happened to be scrolled to.
    fun backToTop() {
        scope.launch { listState.scrollToItem(0) }
    }

    Column(Modifier.fillMaxSize()) {
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                backToTop()
            },
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
            placeholder = { Text("Search countries", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        backToTop()
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilterChip(
                selected = sortMode == CountrySort.Name,
                onClick = {
                    sortMode = CountrySort.Name
                    backToTop()
                },
                label = { Text("Name") }
            )
            FilterChip(
                selected = sortMode == CountrySort.Rank,
                onClick = {
                    sortMode = CountrySort.Rank
                    backToTop()
                },
                label = { Text("Rank") }
            )
        }
        HorizontalDivider()

        if (filteredCountryList.isEmpty()) {
            if (searchQuery.isBlank()) {
                EmptyState()
            } else {
                EmptyState(
                    title = "No matching countries",
                    message = "Nothing matches \"${searchQuery.trim()}\"",
                    actionLabel = "Clear search",
                    onAction = { searchQuery = "" }
                )
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(filteredCountryList) { country ->
                    CountryRow(
                        country = country,
                        selectedCountry = selectedCountry,
                        countrySelected = {
                            keyboardController?.hide()
                            countrySelected(it)
                        },
                        rankings = rankings
                    )
                }
            }
        }
    }
}

@Composable
internal fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String = "Unable to Load Data"
) {
    Column(
        modifier = modifier.fillMaxSize().wrapContentSize(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
internal fun EmptyState(
    title: String? = null,
    message: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            title ?: "No Countries Found!",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}


@Composable
internal fun CountryRow(
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
                        contentDescription = null,
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
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider()
    }
}
