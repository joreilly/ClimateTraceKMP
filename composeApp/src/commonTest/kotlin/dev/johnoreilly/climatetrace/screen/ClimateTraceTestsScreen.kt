package dev.johnoreilly.climatetrace.screen

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.ui.CountryListView
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ClimateTraceScreenTest {
    private val countryList = listOf<Country>(Country("IRL", "IE", "Ireland", "Europe"))

    @Test
    fun testCountryListScreen() = runComposeUiTest {
        setContent {
            CountryListView(countryList, null, {})
        }

        onNodeWithText("Ireland").assertExists()
    }
}

