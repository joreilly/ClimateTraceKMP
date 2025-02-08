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
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryEmissionsScreen(country: Country, onBack: () -> Unit ) {
    val viewModel = koinInject<CountryDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsState()

    LaunchedEffect(country) {
        viewModel.setCountry(country)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(country.name)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            CountryInfoDetailedView(viewState) { year ->
                viewModel.setYear(year)
            }
        }
    }
}
