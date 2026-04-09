package dev.johnoreilly.climatetrace.viewmodel

import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.Country
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


sealed class CountryListUIState {
    object Loading : CountryListUIState()
    data class Error(val message: String) : CountryListUIState()
    data class Success(val countryList: List<Country>, val rankings: Map<String, Int> = emptyMap()) : CountryListUIState()
}

open class CountryListViewModel : ViewModel(), KoinComponent {
    private val climateTraceRepository: ClimateTraceRepository by inject()

    private val _viewState = MutableStateFlow<CountryListUIState>(viewModelScope, CountryListUIState.Loading)
    @NativeCoroutinesState
    val viewState: StateFlow<CountryListUIState> = _viewState.asStateFlow()


    init {
        viewModelScope.coroutineScope.launch {
            try {
                val countries = climateTraceRepository.fetchCountries().sortedBy { it.name }
                val rankingsResponse = climateTraceRepository.fetchRankings("2025")
                val rankings = rankingsResponse.rankings.associate { it.country to it.rank }
                _viewState.value =  CountryListUIState.Success(countries, rankings)

            } catch (e: Exception) {
                _viewState.value = CountryListUIState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}
