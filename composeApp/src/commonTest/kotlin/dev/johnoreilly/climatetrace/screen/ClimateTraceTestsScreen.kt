package dev.johnoreilly.climatetrace.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.remote.EmissionInfo
import dev.johnoreilly.climatetrace.ui.CountryInfoDetailedView
import dev.johnoreilly.climatetrace.ui.CountryListView
import dev.johnoreilly.climatetrace.ui.toPercent
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsUIState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ClimateTraceScreenTest {
    private val country = Country("IRL", "IE", "Ireland", "Europe")
    private val countryList = listOf<Country>(country)
    private val countryEmissions = EmissionInfo(53_000_000.0f, 75_000_000.0f, 100_000_000.0f)
    private val worldEmissions = EmissionInfo(53_000_000_000.0f, 75_000_000_000.0f, 100_000_000_000.0f)
    private val countryEmissionsInfo = CountryEmissionsInfo(country = country.alpha3,
        rank = 73, emissions = countryEmissions, worldEmissions = worldEmissions)
    private val year = "2022"

    @Test
    fun testCountryListView() = runComposeUiTest {
        setContent {
            CountryListView(countryList, null, {})
        }

        onNodeWithText(country.name).assertExists()
    }


    @Test
    fun testCountryInfoDetailsView() = runComposeUiTest {
        val state = CountryDetailsUIState.Success(country,
            year, listOf("2022", "2023"), countryEmissionsInfo, emptyList(),
        )
        setContent {
            CountryInfoDetailedView(state, {})
        }

        onNodeWithText(country.name).assertExists()
        val millionTonnes = (countryEmissions.co2 / 1_000_000).toInt()
        val percentage = (countryEmissions.co2.toDouble() / worldEmissions.co2).toPercent(2)
        onNodeWithText("$millionTonnes").assertExists()
        onNodeWithText(percentage).assertExists()
    }

}

