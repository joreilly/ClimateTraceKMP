package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsViewModel
import dev.johnoreilly.climatetrace.viewmodel.CountryListUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import dev.johnoreilly.climatetrace.viewmodel.IssPositionUiState
import dev.johnoreilly.climatetrace.viewmodel.IssTrackerViewModel
import kotlin.math.roundToInt
import org.koin.compose.koinInject

class IssTrackerScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val issTrackerViewModel = koinInject<IssTrackerViewModel>()
        val issViewState by issTrackerViewModel.viewState.collectAsState()

        val countryListViewModel = koinInject<CountryListViewModel>()
        val countryListViewState by countryListViewModel.viewState.collectAsState()
        val perCapitaRankings = (countryListViewState as? CountryListUIState.Success)?.perCapitaRankings ?: emptyMap()

        val countryDetailsViewModel = koinInject<CountryDetailsViewModel>()
        val countryDetailsViewState by countryDetailsViewModel.viewState.collectAsState()

        LaunchedEffect(issViewState) {
            (issViewState as? IssPositionUiState.OverCountry)?.let { state ->
                countryDetailsViewModel.setCountry(state.country)
            }
        }

        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("ISS Emissions Tracker") }) }
        ) { paddingValues ->
            Row(Modifier.padding(paddingValues).fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IssPositionCard(issViewState)
                    IssMapCard(issViewState)
                }

                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (val state = issViewState) {
                        is IssPositionUiState.OverCountry -> {
                            CountryBasicEmissionsView(
                                viewState = countryDetailsViewState,
                                perCapitaRank = perCapitaRankings[state.country.id],
                                onYearSelected = { year -> countryDetailsViewModel.setYear(year) }
                            )
                        }
                        is IssPositionUiState.OverOcean -> {
                            EmptyState(
                                title = "Over the Ocean",
                                message = "The ISS isn't currently over a country tracked by Climate TRACE."
                            )
                        }
                        is IssPositionUiState.Error -> {
                            EmptyState(title = "Error", message = state.message)
                        }
                        is IssPositionUiState.Loading -> {
                            Column(Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun issCoordinates(state: IssPositionUiState): Pair<Double, Double>? = when (state) {
    is IssPositionUiState.OverCountry -> state.latitude to state.longitude
    is IssPositionUiState.OverOcean -> state.latitude to state.longitude
    else -> null
}

@Composable
private fun IssPositionCard(state: IssPositionUiState) {
    val coordinates = issCoordinates(state)
    val latitude = coordinates?.first
    val longitude = coordinates?.second

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ISS Current Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoordinateDisplay(
                    label = "Latitude",
                    value = latitude?.let { formatCoordinate(it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(16.dp))

                CoordinateDisplay(
                    label = "Longitude",
                    value = longitude?.let { formatCoordinate(it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IssMapCard(state: IssPositionUiState) {
    val coordinates = issCoordinates(state) ?: return

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        IssMapView(
            latitude = coordinates.first,
            longitude = coordinates.second,
            modifier = Modifier.padding(16.dp).fillMaxWidth().aspectRatio(1f)
        )
    }
}

@Composable
private fun CoordinateDisplay(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun formatCoordinate(value: Double): String = ((value * 100).roundToInt() / 100.0).toString()
