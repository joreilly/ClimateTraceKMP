package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.johnoreilly.climatetrace.viewmodel.ClimateTraceViewModel
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class)
class CountryListScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

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
        ) {
            Column(Modifier.padding(it)) {
                CountryListView(
                    countryList.value,
                    selectedCountry.value,
                    isLoadingCountries
                ) { country ->
                    navigator.push(CountryEmissionsScreen(country))
                }
            }
        }
    }
}