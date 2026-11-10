package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.carlsen.flagkit.FlagKit
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.ui.utils.alpha3ToAlpha2
import dev.johnoreilly.climatetrace.ui.utils.formatEmissionsQuantity
import dev.johnoreilly.climatetrace.viewmodel.CountryListUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import io.github.koalaplot.core.util.toString
import org.koin.compose.koinInject

private enum class RankingMode { Total, PerCapita }

internal class RankingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinInject<CountryListViewModel>()
        val state by viewModel.viewState.collectAsState()

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = {
                    val year = (state as? CountryListUIState.Success)?.year.orEmpty()
                    Text(if (year.isEmpty()) "Rankings" else "Rankings · $year")
                })
            }
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues).fillMaxSize()) {
                when (val s = state) {
                    is CountryListUIState.Loading -> {
                        Column(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Loading rankings…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is CountryListUIState.Error -> {
                        ErrorState(message = s.message, onRetry = { viewModel.refresh() })
                    }
                    is CountryListUIState.Success -> {
                        RankingsContent(s, onIncludeForestryChange = { viewModel.setIncludeForestry(it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingsContent(state: CountryListUIState.Success, onIncludeForestryChange: (Boolean) -> Unit) {
    var mode by rememberSaveable { mutableIntStateOf(RankingMode.Total.ordinal) }
    val countriesById = remember(state.countryList) {
        state.countryList.associateBy { it.id }
    }
    val rows = remember(state.rankingsList, mode) {
        when (mode) {
            RankingMode.Total.ordinal ->
                state.rankingsList
                    .filter { it.emissionsQuantity > 0 }
                    .sortedByDescending { it.emissionsQuantity }
            else ->
                state.rankingsList
                    .filter { it.emissionsPerCapita > 0 }
                    .sortedByDescending { it.emissionsPerCapita }
        }
    }

    Column {
        TabRow(selectedTabIndex = mode) {
            Tab(
                selected = mode == RankingMode.Total.ordinal,
                onClick = { mode = RankingMode.Total.ordinal },
                text = { Text("Total") }
            )
            Tab(
                selected = mode == RankingMode.PerCapita.ordinal,
                onClick = { mode = RankingMode.PerCapita.ordinal },
                text = { Text("Per Capita") }
            )
        }
        ForestryToggle(
            includeForestry = !state.excludeForestry,
            onIncludeForestryChange = onIncludeForestryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(rows) { idx, info ->
                val valueText = if (mode == RankingMode.Total.ordinal) {
                    formatEmissionsQuantity(info.emissionsQuantity)
                } else {
                    "${info.emissionsPerCapita.toString(1)} t"
                }
                RankingRow(
                    rank = idx + 1,
                    info = info,
                    valueText = valueText,
                    country = countriesById[info.country],
                    perCapitaRank = state.perCapitaRankings[info.country]
                )
            }
        }
    }
}

@Composable
private fun RankingRow(
    rank: Int,
    info: CountryEmissionsInfo,
    valueText: String,
    country: Country?,
    perCapitaRank: Int?
) {
    val navigator = LocalNavigator.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = country != null) {
                    country?.let { navigator?.push(CountryEmissionsScreen(it, perCapitaRank)) }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.width(44.dp)
            )
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 22.dp)
                    .clip(RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                val flag = alpha3ToAlpha2(info.country)?.let { FlagKit.getFlag(it) }
                flag?.let {
                    Image(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = info.name ?: country?.name ?: info.country,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (country != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider()
    }
}
