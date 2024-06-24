package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.johnoreilly.climatetrace.viewmodel.CountryListUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class)
class CountryListScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = koinInject<CountryListViewModel>()
        val viewState by viewModel.viewState.collectAsState()

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = {
                    Text("ClimateTraceKMP")
                })
            }
        ) {
            Column(Modifier.padding(it)) {
                when (val state = viewState) {
                    is CountryListUIState.Loading -> {
                        Column(modifier = Modifier.fillMaxSize().fillMaxHeight()
                                .wrapContentSize(Alignment.Center)
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is CountryListUIState.Error -> {}
                    is CountryListUIState.Success -> {
                        CountryListView(state.countryList, null) { country ->
                            navigator.push(CountryEmissionsScreen(country))
                        }
                    }
                }
            }
        }
    }
}