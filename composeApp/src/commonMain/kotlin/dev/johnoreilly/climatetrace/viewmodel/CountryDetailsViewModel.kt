package dev.johnoreilly.climatetrace.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


sealed class CountryDetailsUIState {
    data object NoCountrySelected : CountryDetailsUIState()
    data object Loading : CountryDetailsUIState()
    data class Error(val message: String) : CountryDetailsUIState()
    data class Success(
        val country: Country,
        val year: String,
        val countryEmissionInfo: CountryEmissionsInfo?,
        val countryAssetEmissionsList: List<CountryAssetEmissionsInfo>
    ) : CountryDetailsUIState()
}

sealed interface CountryDetailsEvents {
    data class SetCountry(val country: Country): CountryDetailsEvents
    data class SetYear(val year: String): CountryDetailsEvents
}

open class CountryDetailsViewModel : ViewModel(), KoinComponent {
    private val climateTraceRepository: ClimateTraceRepository by inject()

    private val events = MutableSharedFlow<CountryDetailsEvents>()

    val viewState: StateFlow<CountryDetailsUIState> = viewModelScope.coroutineScope.launchMolecule(mode = RecompositionMode.Immediate) {
        CountryDetailsPresenter(events)
    }

    fun setYear(year: String) {
        viewModelScope.launch {
            events.emit(CountryDetailsEvents.SetYear(year))
        }
    }

    fun setCountry(country: Country) {
        viewModelScope.launch {
            events.emit(CountryDetailsEvents.SetCountry(country))
        }
    }

    @Composable
    fun CountryDetailsPresenter(events: Flow<CountryDetailsEvents>): CountryDetailsUIState {
        var uiState by remember { mutableStateOf<CountryDetailsUIState>(CountryDetailsUIState.NoCountrySelected) }
        var selectedCountry by remember { mutableStateOf<Country?>(null) }
        var selectedYear by remember { mutableStateOf("2022") }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    is CountryDetailsEvents.SetCountry -> selectedCountry = event.country
                    is CountryDetailsEvents.SetYear -> selectedYear = event.year
                }
            }
        }

        LaunchedEffect(selectedCountry, selectedYear) {
            selectedCountry?.let { country ->
                uiState = CountryDetailsUIState.Loading
                try {
                    val countryEmissionInfo = climateTraceRepository.fetchCountryEmissionsInfo(country.alpha3, selectedYear).firstOrNull()
                    val countryAssetEmissionsList = climateTraceRepository.fetchCountryAssetEmissionsInfo(country.alpha3)
                    uiState = CountryDetailsUIState.Success(country, selectedYear, countryEmissionInfo, countryAssetEmissionsList)
                } catch (e: Exception) {
                    uiState = CountryDetailsUIState.Error("Error retrieving data from backend")
                }
            }
        }

        return uiState
    }
}

