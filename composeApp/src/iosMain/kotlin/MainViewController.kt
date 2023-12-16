import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import dev.johnoreilly.climatetrace.ktx.performAsyncOperation
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.ui.CountryInfoDetailedView
import dev.johnoreilly.climatetrace.ui.CountryListView

fun CountryListViewController(onCountryClicked: (country: Country) -> Unit) = ComposeUIViewController {
    val climateTraceApi = remember { ClimateTraceApi() }
    var countryList by remember { mutableStateOf(emptyList<Country>()) }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(true) {
        performAsyncOperation(
            isLoadingState = isLoading,
            operation = { climateTraceApi.fetchCountries().sortedBy { it.name } },
            onSuccess = { countries -> countryList = countries }
        )
    }

    CountryListView(countryList, selectedCountry, isLoading.value) {
        selectedCountry = it
        onCountryClicked(it)
    }
}


fun CountryInfoDetailedViewController(country: Country) = ComposeUIViewController {
    val climateTraceApi = remember { ClimateTraceApi() }
    var countryEmissionInfo by remember { mutableStateOf<CountryEmissionsInfo?>(null) }
    var countryAssetEmissions by remember { mutableStateOf<List<CountryAssetEmissionsInfo>?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(country) {
        performAsyncOperation(
            isLoadingState = isLoading,
            operation = { climateTraceApi.fetchCountryEmissionsInfo(country.alpha3) },
            onSuccess = { countryEmissionInfoList ->
                countryEmissionInfo = countryEmissionInfoList.firstOrNull()
            }
        )
        performAsyncOperation(
            isLoadingState = isLoading,
            operation = { climateTraceApi.fetchCountryAssetEmissionsInfo(country.alpha3)[country.alpha3] },
            onSuccess = { countryAssetEmissionsDetails ->
                countryAssetEmissions = countryAssetEmissionsDetails
            }
        )
    }

    CountryInfoDetailedView(country, countryEmissionInfo, countryAssetEmissions, isLoading.value)
}
