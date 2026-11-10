package dev.johnoreilly.climatetrace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


sealed class CountryListUIState {
    object Loading : CountryListUIState()
    data class Error(val message: String) : CountryListUIState()
    data class Success(
        val countryList: List<Country>,
        val rankings: Map<String, Int> = emptyMap(),
        val perCapitaRankings: Map<String, Int> = emptyMap(),
        val rankingsList: List<CountryEmissionsInfo> = emptyList(),
        val year: String = "",
        val excludeForestry: Boolean = false
    ) : CountryListUIState()
}

open class CountryListViewModel internal constructor(
    private val climateTraceRepository: ClimateTraceRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow<CountryListUIState>(CountryListUIState.Loading)
    val viewState: StateFlow<CountryListUIState> = _viewState.asStateFlow()


    private val refreshRequests = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(climateTraceRepository.excludeForestry, refreshRequests) { exclude, _ -> exclude }
                .collectLatest { loadCountryData(it) }
        }
    }

    fun refresh() {
        refreshRequests.value++
    }

    fun setIncludeForestry(include: Boolean) {
        climateTraceRepository.setExcludeForestry(!include)
    }

    private suspend fun loadCountryData(excludeForestry: Boolean) {
        // Flipping the forestry setting re-ranks a list that's already on screen, so leave it
        // there until the new numbers arrive rather than dropping back to a spinner.
        if (_viewState.value !is CountryListUIState.Success) {
            _viewState.value = CountryListUIState.Loading
        }
        try {
            val countries = climateTraceRepository.fetchCountries().sortedBy { it.name }
            val year = climateTraceRepository.fetchDataCoverage().latestYear
            val rankingsResponse = climateTraceRepository.fetchRankings(year, excludeForestry)
            val rankings = rankingsResponse.rankings.associate { it.country to it.rank }
            val perCapitaRankings = rankingsResponse.rankings
                .filter { it.emissionsPerCapita > 0 }
                .sortedByDescending { it.emissionsPerCapita }
                .mapIndexed { index, info -> info.country to (index + 1) }
                .toMap()
            _viewState.value = CountryListUIState.Success(
                countryList = countries,
                rankings = rankings,
                perCapitaRankings = perCapitaRankings,
                rankingsList = rankingsResponse.rankings,
                year = year,
                excludeForestry = excludeForestry
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _viewState.value = CountryListUIState.Error(e.message ?: "Unknown Error")
        }
    }
}
