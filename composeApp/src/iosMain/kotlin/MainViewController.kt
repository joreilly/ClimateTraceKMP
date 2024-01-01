import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.ui.CountryInfoDetailedView
import dev.johnoreilly.climatetrace.ui.CountryListView
import dev.johnoreilly.climatetrace.viewmodel.ClimateTraceViewModel

fun CountryListViewController(onCountryClicked: (country: Country) -> Unit) = ComposeUIViewController {
    val viewModel = remember { ClimateTraceViewModel() }
    val countryList by viewModel.countryList.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val isLoadingCountries by viewModel.isLoadingCountries.collectAsState()

    CountryListView(countryList, selectedCountry, isLoadingCountries) {
        //selectedCountry.value = it
        onCountryClicked(it)
    }
}


fun CountryInfoDetailedViewController(country: Country) = ComposeUIViewController {
    val viewModel = remember { ClimateTraceViewModel() }
    val countryEmissionInfo by viewModel.countryEmissionInfo.collectAsState()
    val countryAssetEmissions by viewModel.countryAssetEmissions.collectAsState()
    val isLoadingCountryDetails by viewModel.isLoadingCountries.collectAsState()

    LaunchedEffect(country) {
        viewModel.setCountry(country)
    }

    CountryInfoDetailedView(country, countryEmissionInfo, countryAssetEmissions, isLoadingCountryDetails)
}
