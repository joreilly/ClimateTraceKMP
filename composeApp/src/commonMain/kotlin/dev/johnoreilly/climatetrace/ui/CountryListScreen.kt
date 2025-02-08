package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.viewmodel.CountryListUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import org.koin.compose.koinInject


@Composable
fun CountryListScreen(navigateToCountry: (Country) -> Unit) {
    val viewModel = koinInject<CountryListViewModel>()
    val viewState by viewModel.viewState.collectAsState()

    Column {
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
                    navigateToCountry(country)
                }
            }
        }
    }
}
