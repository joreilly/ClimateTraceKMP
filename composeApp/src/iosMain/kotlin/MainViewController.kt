
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.CountryInfoDetailedView
import dev.johnoreilly.climatetrace.ui.CountryListView
import dev.johnoreilly.climatetrace.ui.CountryScreenSuccess
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsViewModel
import dev.johnoreilly.climatetrace.viewmodel.CountryListUIState
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import org.koin.compose.koinInject

fun CountryListViewController(onCountryClicked: (country: Country) -> Unit) = ComposeUIViewController {
    val viewModel = koinInject<CountryListViewModel>()
    val viewState by viewModel.viewState.collectAsState()

    when (val state = viewState) {
        is CountryListUIState.Loading -> {
            Column(modifier = Modifier.fillMaxSize().fillMaxHeight()
                .wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        }
        is CountryListUIState.Error -> {
            Text("Error")
        }
        is CountryListUIState.Success -> {
            CountryScreenSuccess(state.countryList)
        }
    }
}


fun CountryInfoDetailedViewController(country: Country) = ComposeUIViewController {
    val viewModel = koinInject<CountryDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsState()

    LaunchedEffect(country) {
        viewModel.setCountry(country)
    }

    CountryInfoDetailedView(viewState) {
        viewModel.setYear(it)
    }
}
