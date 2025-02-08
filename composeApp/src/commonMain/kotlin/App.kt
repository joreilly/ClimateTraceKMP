import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.NavEntry
import androidx.navigation3.SinglePaneNavDisplay
import dev.johnoreilly.climatetrace.di.commonModule
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.CountryEmissionsScreen
import dev.johnoreilly.climatetrace.ui.CountryListScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication


data object HomeScreenKey
data class CountryScreenKey(val country: Country)

@Preview
@Composable
fun App() {
    MaterialTheme {
        val backStack = remember { mutableStateListOf<Any>(HomeScreenKey) }

        SinglePaneNavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    is HomeScreenKey -> NavEntry(key) {
                        CountryListScreen { country ->
                            backStack.add(CountryScreenKey(country))
                        }
                    }
                    is CountryScreenKey -> NavEntry(key) {
                        CountryEmissionsScreen(key.country) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                    else -> NavEntry(Unit) { Text("Unknown route") }
                }
            }
        )
    }
}