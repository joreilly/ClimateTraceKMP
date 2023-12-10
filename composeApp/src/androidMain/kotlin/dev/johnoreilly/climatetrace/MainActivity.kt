@file:OptIn(ExperimentalMaterial3Api::class)

package dev.johnoreilly.climatetrace

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.ui.CountryInfoDetailedView
import dev.johnoreilly.climatetrace.ui.CountryListView

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val climateTraceApi = remember { ClimateTraceApi() }

        var countryList by remember { mutableStateOf(emptyList<Country>()) }
        val selectedCountry by remember { mutableStateOf<Country?>(null) }

        LaunchedEffect(true) {
            countryList = climateTraceApi.fetchCountries().sortedBy { it.name }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = {
                    Text("ClimateTraceKMP")
                })
            }
        ) {
            Column(Modifier.padding(it)) {
                CountryListView(countryList, selectedCountry) {
                    navigator.push(CountryEmissionsScreen(it))
                }
            }
        }
    }


    data class CountryEmissionsScreen(val country: Country) : Screen {

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow

            val climateTraceApi = remember { ClimateTraceApi() }
            var countryEmissionInfo by remember { mutableStateOf<CountryEmissionsInfo?>(null) }
            var countryAssetEmissions by remember {
                mutableStateOf<List<CountryAssetEmissionsInfo>?>(
                    null
                )
            }

            LaunchedEffect(country) {
                val countryEmissionInfoList =
                    climateTraceApi.fetchCountryEmissionsInfo(country.alpha3)
                countryEmissionInfo = countryEmissionInfoList[0]
                countryAssetEmissions =
                    climateTraceApi.fetchCountryAssetEmissionsInfo(country.alpha3)[country.alpha3]
            }

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text("ClimateTraceKMP")
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) {
                Column(Modifier.padding(it)) {
                    CountryInfoDetailedView(country, countryEmissionInfo, countryAssetEmissions)
                }
            }
        }
    }
}


