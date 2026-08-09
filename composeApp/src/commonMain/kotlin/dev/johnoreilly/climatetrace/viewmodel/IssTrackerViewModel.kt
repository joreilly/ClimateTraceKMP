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


sealed class IssPositionUiState {
    data object Loading : IssPositionUiState()
    data class Error(val message: String) : IssPositionUiState()
    data class OverOcean(val latitude: Double, val longitude: Double) : IssPositionUiState()
    data class OverCountry(val latitude: Double, val longitude: Double, val country: Country) : IssPositionUiState()
}

open class IssTrackerViewModel : ViewModel(), KoinComponent {
    private val climateTraceRepository: ClimateTraceRepository by inject()

    private val _viewState = MutableStateFlow<IssPositionUiState>(viewModelScope, IssPositionUiState.Loading)
    @NativeCoroutinesState
    val viewState: StateFlow<IssPositionUiState> = _viewState.asStateFlow()

    private var countries: List<Country> = emptyList()

    init {
        viewModelScope.coroutineScope.launch {
            try {
                countries = climateTraceRepository.fetchCountries()
            } catch (_: Exception) {
                // country lookup will just fall back to "over ocean" until this succeeds on retry
            }

            climateTraceRepository.pollISSPosition().collect { position ->
                try {
                    val countryCode = climateTraceRepository.reverseGeocodeCountry(position.latitude, position.longitude)
                    val country = countryCode?.let { code -> countries.find { it.id == code } }
                    _viewState.value = if (country != null) {
                        IssPositionUiState.OverCountry(position.latitude, position.longitude, country)
                    } else {
                        IssPositionUiState.OverOcean(position.latitude, position.longitude)
                    }
                } catch (e: Exception) {
                    _viewState.value = IssPositionUiState.Error(e.message ?: "Unknown Error")
                }
            }
        }
    }
}
