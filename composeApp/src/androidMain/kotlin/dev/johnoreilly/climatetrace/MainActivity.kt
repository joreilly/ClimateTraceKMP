@file:OptIn(ExperimentalMaterial3Api::class)

package dev.johnoreilly.climatetrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.johnoreilly.climatetrace.di.initKoin
import dev.johnoreilly.climatetrace.ui.CountryEmissionsScreen
import dev.johnoreilly.climatetrace.ui.CountryListView
import dev.johnoreilly.climatetrace.viewmodel.ClimateTraceViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initKoin()

        setContent {
            MaterialTheme {
                AndroidApp()
            }
        }
    }
}

@Composable
fun AndroidApp() {
    Navigator(screen = CountryListScreen())
}


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

