package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.viewmodel.ClimateTraceViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
data class CountryEmissionsScreen(val country: Country) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = koinInject<ClimateTraceViewModel>()
        val countryEmissionInfo by viewModel.countryEmissionInfo.collectAsState()
        val countryAssetEmissions by viewModel.countryAssetEmissions.collectAsState()
        val isLoadingCountryDetails by viewModel.isLoadingCountryDetails.collectAsState()

        LaunchedEffect(country) {
            viewModel.fetchCountryDetails(country)
        }

        Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                            title = {
                                Text("ClimateTraceKMP")
                            },
                            navigationIcon = {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                    )
                }
        ) {
            Column(Modifier.padding(it)) {
                CountryInfoDetailedView(
                        country,
                        viewModel.year,
                        countryEmissionInfo,
                        countryAssetEmissions,
                        isLoadingCountryDetails
                )
            }
        }
    }
}