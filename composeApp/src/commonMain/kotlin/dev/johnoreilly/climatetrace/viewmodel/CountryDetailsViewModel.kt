package dev.johnoreilly.climatetrace.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.Asset
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.remote.DataMetadata
import dev.johnoreilly.climatetrace.remote.MonthlyEmissions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


internal sealed class CountryDetailsUIState {
    data object NoCountrySelected : CountryDetailsUIState()
    data object Loading : CountryDetailsUIState()
    data class Error(val message: String) : CountryDetailsUIState()
    data class Success(
        val country: Country,
        val year: String,
        val availableYears: List<String>,
        val countryEmissionInfo: CountryEmissionsInfo?,
        val countryAssetEmissionsList: List<CountryAssetEmissionsInfo>,
        val assets: List<Asset> = emptyList(),
        val yearlyEmissions: Map<String, Double> = emptyMap(),
        // True while a different year is being fetched: the previous year's figures stay on
        // screen (with `year` already pointing at the new selection) instead of a blank spinner.
        val isRefreshing: Boolean = false,
        // Month-by-month totals for `year`.
        val monthlyEmissions: List<MonthlyEmissions> = emptyList(),
        val excludeForestry: Boolean = false,
        val dataMetadata: DataMetadata? = null,
        // Non-null when `year` is still in progress: the last month (1..12) it has data for.
        val partialThroughMonth: Int? = null
    ) : CountryDetailsUIState()
}

internal sealed interface CountryDetailsEvents {
    data class SetCountry(val country: Country): CountryDetailsEvents
    data class SetYear(val year: String): CountryDetailsEvents
    data object Retry: CountryDetailsEvents
}

internal open class CountryDetailsViewModel : ViewModel(), KoinComponent {
    private val climateTraceRepository: ClimateTraceRepository by inject()
    private val events = MutableSharedFlow<CountryDetailsEvents>(extraBufferCapacity = 20)

    val viewState: StateFlow<CountryDetailsUIState> = viewModelScope.launchMolecule(mode = RecompositionMode.Immediate) {
        CountryDetailsPresenter(events)
    }

    fun setYear(year: String) {
        events.tryEmit(CountryDetailsEvents.SetYear(year))
    }

    fun setCountry(country: Country) {
        events.tryEmit(CountryDetailsEvents.SetCountry(country))
    }

    fun setIncludeForestry(include: Boolean) {
        climateTraceRepository.setExcludeForestry(!include)
    }

    fun retry() {
        events.tryEmit(CountryDetailsEvents.Retry)
    }

    @Composable
    fun CountryDetailsPresenter(events: Flow<CountryDetailsEvents>): CountryDetailsUIState {
        var uiState by remember { mutableStateOf<CountryDetailsUIState>(CountryDetailsUIState.NoCountrySelected) }
        var selectedCountry by remember { mutableStateOf<Country?>(null) }
        // Null until the user picks one: the default is the latest complete year in the data.
        var selectedYear by remember { mutableStateOf<String?>(null) }
        var yearlyEmissions by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
        var retryCount by remember { mutableIntStateOf(0) }
        val excludeForestry by climateTraceRepository.excludeForestry.collectAsState()

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    is CountryDetailsEvents.SetCountry -> selectedCountry = event.country
                    is CountryDetailsEvents.SetYear -> selectedYear = event.year
                    CountryDetailsEvents.Retry -> retryCount++
                }
            }
        }

        LaunchedEffect(selectedCountry, excludeForestry, retryCount) {
            yearlyEmissions = emptyMap()
            selectedCountry?.let { country ->
                try {
                    // Complete years only: a year to date would read as a collapse on the trend.
                    val years = climateTraceRepository.fetchDataCoverage().completeYears
                    yearlyEmissions = coroutineScope {
                        years.map { y ->
                            y to async {
                                runCatching {
                                    climateTraceRepository.fetchCountryEmissionsInfo(country.id, y, excludeForestry)
                                        .firstOrNull()?.emissionsQuantity
                                }.getOrNull()
                            }
                        }.mapNotNull { (y, deferred) ->
                            deferred.await()?.let { y to it }
                        }.toMap()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    yearlyEmissions = emptyMap()
                }
            }
        }

        LaunchedEffect(selectedCountry, selectedYear, excludeForestry, retryCount) {
            selectedCountry?.let { country ->
                val current = uiState
                uiState = if (current is CountryDetailsUIState.Success && current.country.id == country.id) {
                    current.copy(
                        year = selectedYear ?: current.year,
                        excludeForestry = excludeForestry,
                        partialThroughMonth = null,
                        isRefreshing = true
                    )
                } else {
                    CountryDetailsUIState.Loading
                }
                try {
                    val coverage = climateTraceRepository.fetchDataCoverage()
                    val year = selectedYear ?: coverage.latestYear
                    uiState = coroutineScope {
                        val countryEmissionInfo = async { climateTraceRepository.fetchCountryEmissionsInfo(country.id, year, excludeForestry).firstOrNull() }
                        val breakdown = async { climateTraceRepository.fetchCountryEmissionsBreakdown(country.id, year, excludeForestry) }
                        val assets = async { climateTraceRepository.fetchAssetsByCountry(country.id, excludeForestry = excludeForestry) }
                        val emissions = breakdown.await()
                        CountryDetailsUIState.Success(
                            country = country,
                            year = year,
                            availableYears = coverage.years,
                            countryEmissionInfo = countryEmissionInfo.await(),
                            countryAssetEmissionsList = emissions.sectors?.summaries.orEmpty() + emissions.subsectors?.summaries.orEmpty(),
                            assets = assets.await(),
                            yearlyEmissions = yearlyEmissions,
                            monthlyEmissions = emissions.totals?.timeseries.orEmpty().sortedBy { it.month },
                            excludeForestry = excludeForestry,
                            dataMetadata = coverage.metadata,
                            partialThroughMonth = coverage.partialThroughMonth.takeIf { year == coverage.partialYear }
                        )
                    }
                } catch (e: CancellationException) {
                    // Superseded by a newer country/year selection, which owns uiState from here.
                    throw e
                } catch (e: Exception) {
                    uiState = CountryDetailsUIState.Error("Error retrieving data from backend, ${e.message}")
                }
            }
        }

        LaunchedEffect(yearlyEmissions) {
            val current = uiState
            if (current is CountryDetailsUIState.Success && yearlyEmissions.isNotEmpty()) {
                uiState = current.copy(yearlyEmissions = yearlyEmissions)
            }
        }

        return uiState
    }
}

