package dev.johnoreilly.climatetrace.viewmodel

import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject



sealed class CountryDetailsUIState {
    object NoCountrySelected : CountryDetailsUIState()
    object Loading : CountryDetailsUIState()
    data class Error(val message: String) : CountryDetailsUIState()
    data class Success(
        val country: Country,
        val year: String,
        val countryEmissionInfo: CountryEmissionsInfo?,
        val countryAssetEmissionsList: List<CountryAssetEmissionsInfo>
    ) : CountryDetailsUIState()
}


open class CountryDetailsViewModel : ViewModel(), KoinComponent {
    private val climateTraceRepository: ClimateTraceRepository by inject()

    private val _viewState = MutableStateFlow<CountryDetailsUIState>(viewModelScope, CountryDetailsUIState.NoCountrySelected)
    @NativeCoroutinesState
    val viewState = _viewState.asStateFlow()


    @NativeCoroutinesState
    val selectedYear = MutableStateFlow<String>(viewModelScope, "2022")

    @NativeCoroutinesState
    val selectedCountry = MutableStateFlow<Country?>(viewModelScope, null)

    fun setYear(year: String) {
        selectedYear.value = year
        fetchCountryDetails()
    }

    fun setCountry(country: Country) {
        selectedCountry.value = country
        fetchCountryDetails()
    }

    fun fetchCountryDetails() {
        selectedCountry.value?.let { country ->
            _viewState.value = CountryDetailsUIState.Loading
            viewModelScope.coroutineScope.launch {
                val countryEmissionInfo = climateTraceRepository.fetchCountryEmissionsInfo(country.alpha3, selectedYear.value).firstOrNull()
                val countryAssetEmissionsList = climateTraceRepository.fetchCountryAssetEmissionsInfo(country.alpha3)
                _viewState.value = CountryDetailsUIState.Success(country, selectedYear.value, countryEmissionInfo, countryAssetEmissionsList)
            }
        }
    }
}
