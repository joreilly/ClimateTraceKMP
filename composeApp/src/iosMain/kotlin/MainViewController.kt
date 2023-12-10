import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.CountryListView

fun CountryListViewController(onCountryClicked: (countryCode: String) -> Unit) = ComposeUIViewController {
    val climateTraceApi = remember { ClimateTraceApi() }
    var countryList by remember { mutableStateOf(emptyList<Country>()) }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }

    LaunchedEffect(true) {
        countryList = climateTraceApi.fetchCountries().sortedBy { it.name }
    }

    CountryListView(countryList, selectedCountry) {
        selectedCountry = it
        onCountryClicked(it.alpha3)
    }
}
