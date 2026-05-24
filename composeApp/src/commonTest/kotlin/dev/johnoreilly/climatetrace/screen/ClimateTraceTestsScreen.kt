package dev.johnoreilly.climatetrace.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.ui.CountryInfoDetailedView
import dev.johnoreilly.climatetrace.ui.CountryListView
import dev.johnoreilly.climatetrace.ui.utils.formatEmissionsQuantity
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsUIState
import io.github.koalaplot.core.util.toString
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ClimateTraceScreenTest {
    private val country = Country("IRL", name = "Ireland", continent = "Europe")
    private val countryList = listOf<Country>(country)
    private val emissionsQuantity = 53_000_000.0
    private val percentage = emissionsQuantity / 53_000_000_000.0
    private val countryEmissionsInfo = CountryEmissionsInfo(
        country = country.id,
        rank = 73,
        emissionsQuantity = emissionsQuantity,
        percentage = percentage,
    )
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
            CountryInfoDetailedView(state, onYearSelected = {})
        }

        onNodeWithText(country.name).assertExists()
        onNodeWithText(formatEmissionsQuantity(emissionsQuantity)).assertExists()
        onNodeWithText("${percentage.toString(2)}%").assertExists()
    }

}
