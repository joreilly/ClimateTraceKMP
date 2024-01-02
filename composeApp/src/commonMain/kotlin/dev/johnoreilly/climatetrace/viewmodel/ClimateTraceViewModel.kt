package dev.johnoreilly.climatetrace.viewmodel

import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.*
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

open class ClimateTraceViewModel : KMMViewModel() {
    private val climateTraceApi = ClimateTraceApi()

    @NativeCoroutinesState
    val countryList = MutableStateFlow<List<Country>>(emptyList())

    @NativeCoroutinesState
    val selectedCountry = MutableStateFlow<Country?>(null)

    @NativeCoroutinesState
    val countryEmissionInfo = MutableStateFlow<CountryEmissionsInfo?>(null)

    @NativeCoroutinesState
    val countryAssetEmissions = MutableStateFlow<List<CountryAssetEmissionsInfo>?>(null)

    val isLoadingCountries = MutableStateFlow(false)
    val isLoadingCountryDetails = MutableStateFlow(false)

    init {
        viewModelScope.coroutineScope.launch {
            isLoadingCountries.value = true
            countryList.value = climateTraceApi.fetchCountries().sortedBy { it.name }
            isLoadingCountries.value = false
        }
    }

    fun setCountry(country: Country) {
        selectedCountry.value = country
        viewModelScope.coroutineScope.launch {
            isLoadingCountryDetails.value = true
            countryEmissionInfo.value = climateTraceApi.fetchCountryEmissionsInfo(country.alpha3).firstOrNull()
            countryAssetEmissions.value = climateTraceApi.fetchCountryAssetEmissionsInfo(country.alpha3)[country.alpha3]
            isLoadingCountryDetails.value = false
        }
    }
}