import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.johnoreilly.climatetrace.di.commonModule
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.CountryAssetEmissionsInfoTreeMapChart
import dev.johnoreilly.climatetrace.viewmodel.ClimateTraceViewModel
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject


fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ClimateTraceKMP") {
        App()

//        KoinApplication(application = {
//            modules(commonModule())
//        }) {
//            MaterialTheme {
//                DesktopApp()
//            }
//        }
//

    }
}

@Composable
fun DesktopApp() {
    val viewModel = koinInject<ClimateTraceViewModel>()
    val countryAssetEmissions by viewModel.countryAssetEmissions.collectAsState()

    LaunchedEffect(viewModel) {
        val country = Country(alpha3 = "CHN", alpha2 = "CN", name = "United States of America", continent = "NorthAmerica")
        viewModel.fetchCountryDetails(country)
    }

    countryAssetEmissions?.let {
        CountryAssetEmissionsInfoTreeMapChart(it)
    }
}




//suspend fun main() {
//    val climateTraceApi = ClimateTraceApi()
//    val result = climateTraceApi.fetchCountryAssetEmissionsInfo("FRA")
//    println(result)
//}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}

