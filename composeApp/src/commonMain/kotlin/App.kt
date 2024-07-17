import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cafe.adriel.voyager.navigator.Navigator
import dev.johnoreilly.climatetrace.di.commonModule
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.ClimateTraceScreen
import dev.johnoreilly.climatetrace.ui.CountryAssetEmissionsInfoTreeMapChart
import dev.johnoreilly.climatetrace.ui.CountryListView
import dev.johnoreilly.climatetrace.ui.SectorEmissionsPieChart
import dev.johnoreilly.climatetrace.ui.YearSelector
import dev.johnoreilly.climatetrace.ui.toPercent
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsViewModel
import dev.johnoreilly.climatetrace.viewmodel.CountryListUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import kotlinx.serialization.Serializable
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

@Serializable
object CountryList

@Composable
fun AppJetpackBav() {
    KoinApplication(application = {
        modules(commonModule())
    }) {
        MaterialTheme {
            val navController = rememberNavController()

NavHost(
    navController = navController,
    startDestination = CountryList,
) {
    composable<CountryList> {
        CountryListScreenJetpackNav { country ->
            navController.navigate(country)
        }
    }
    composable<Country> { backStackEntry ->
        val country: Country = backStackEntry.toRoute()
        CountryInfoDetailedViewJetpackNav(country, popBack = { navController.popBackStack() })
    }
}
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreenJetpackNav(countrySelected: (country: Country) -> Unit) {
    val viewModel = koinInject<CountryListViewModel>()
    val viewState by viewModel.viewState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                    Text("ClimateTraceKMP")
                }
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            when (val state = viewState) {
                is CountryListUIState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize().fillMaxHeight()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CountryListUIState.Error -> {}
                is CountryListUIState.Success -> {
                    CountryListView(state.countryList, null, countrySelected)
                }
            }
        }
    }
}


@Composable
fun CountryInfoDetailedViewJetpackNav(
    country: Country,
    popBack: () -> Unit
) {
    val countryDetailsViewModel: CountryDetailsViewModel = koinInject()
    val countryDetailsViewState by countryDetailsViewModel.viewState.collectAsState()

    LaunchedEffect(country) {
        countryDetailsViewModel.setCountry(country)
    }

    val viewState = countryDetailsViewState
    when (viewState) {
        CountryDetailsUIState.NoCountrySelected -> {
            Column(
                modifier = Modifier.fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(text = "No Country Selected.", style = MaterialTheme.typography.titleMedium)
            }
        }
        is CountryDetailsUIState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        }
        is CountryDetailsUIState.Error -> { Text("Error") }
        is CountryDetailsUIState.Success -> {
            CountryInfoDetailedViewSuccessJetpackNav(viewState, popBack) {
                countryDetailsViewModel.setYear(it)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryInfoDetailedViewSuccessJetpackNav(viewState: CountryDetailsUIState.Success, popBack: () -> Unit, onYearSelected: (String) -> Unit) {

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(viewState.country.name) },
                navigationIcon = {
                    IconButton(onClick = { popBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = viewState.country.name,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(16.dp))

            val year = viewState.year
            val countryAssetEmissionsList = viewState.countryAssetEmissionsList
            val countryEmissionInfo = viewState.countryEmissionInfo

            YearSelector(year, onYearSelected)
            countryEmissionInfo?.let {
                val co2 = (countryEmissionInfo.emissions.co2 / 1_000_000).toInt()
                val percentage =
                    (countryEmissionInfo.emissions.co2 / countryEmissionInfo.worldEmissions.co2).toPercent(
                        2
                    )

                Text(text = "co2 = $co2 Million Tonnes ($year)")
                Text(text = "rank = ${countryEmissionInfo.rank} ($percentage)")

                Spacer(modifier = Modifier.size(16.dp))

                val filteredCountryAssetEmissionsList =
                    countryAssetEmissionsList.filter { it.sector != null }
                if (filteredCountryAssetEmissionsList.isNotEmpty()) {
                    SectorEmissionsPieChart(countryAssetEmissionsList)
                    Spacer(modifier = Modifier.size(32.dp))
                    CountryAssetEmissionsInfoTreeMapChart(countryAssetEmissionsList)
                } else {
                    Spacer(modifier = Modifier.size(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Invalid data",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.Red),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
