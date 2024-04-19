import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cafe.adriel.voyager.navigator.Navigator
import dev.johnoreilly.climatetrace.di.commonModule
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.ClimateTraceScreen
import dev.johnoreilly.climatetrace.ui.CountryInfoDetailedView
import dev.johnoreilly.climatetrace.ui.CountryListView
import dev.johnoreilly.climatetrace.viewmodel.ClimateTraceViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject


@Preview
@Composable
fun AppVoyagerNav() {
    KoinApplication(application = {
        modules(commonModule())
    }) {
        MaterialTheme {
            Navigator(screen = ClimateTraceScreen())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppJetpackBav() {
    KoinApplication(application = {
        modules(commonModule())
    }) {

        MaterialTheme {
            val navController = rememberNavController()

            val viewModel = koinInject<ClimateTraceViewModel>()
            val countryList = viewModel.countryList.collectAsState()
            val selectedCountry = viewModel.selectedCountry.collectAsState()
            val isLoadingCountries by viewModel.isLoadingCountries.collectAsState()

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(title = {
                        Text("ClimateTraceKMP")
                    })
                }
            ) { innerPadding ->

                NavHost(
                    navController = navController,
                    startDestination = "countryList",
                    modifier = Modifier.padding(innerPadding)
                ) {

                    composable(route = "countryList") {
                        CountryListView(countryList.value, selectedCountry.value, isLoadingCountries) { country ->
                            navController.navigate("details/${country.name}/${country.alpha3}")
                        }
                    }
                    composable("details/{countryName}/{countryCode}",) { backStackEntry ->

                        val countryName = backStackEntry.arguments?.getString("countryName") ?: ""
                        val countryCode = backStackEntry.arguments?.getString("countryCode") ?: ""
                        val country = Country(countryCode, "", countryName, "")

                        val countryEmissionInfo by viewModel.countryEmissionInfo.collectAsState()
                        val countryAssetEmissions by viewModel.countryAssetEmissions.collectAsState()
                        val isLoadingCountryDetails by viewModel.isLoadingCountryDetails.collectAsState()

                        LaunchedEffect(country) {
                            viewModel.fetchCountryDetails(country)
                        }

                        CountryInfoDetailedView(country, viewModel.year, countryEmissionInfo,
                            countryAssetEmissions, isLoadingCountryDetails)
                    }
                }
            }
        }
    }

}
