package dev.johnoreilly.climatetrace.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.ui.CountryInfoDetailedView
import dev.johnoreilly.climatetrace.remote.MonthlyEmissions
import dev.johnoreilly.climatetrace.ui.CountryListView
import dev.johnoreilly.climatetrace.ui.MapPoint
import dev.johnoreilly.climatetrace.ui.zoomToFit
import dev.johnoreilly.climatetrace.ui.utils.formatEmissionsQuantity
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsUIState
import io.github.koalaplot.core.util.toString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun testCountryListSearchFiltersAndClears() = runComposeUiTest {
        val brazil = Country("BRA", name = "Brazil", continent = "South America")
        setContent {
            CountryListView(listOf(country, brazil), null, {})
        }

        onNodeWithText("Search countries").performTextInput("bra")
        onNodeWithText(brazil.name).assertExists()
        onNodeWithText(country.name).assertDoesNotExist()

        onNodeWithText("bra").performTextReplacement("zzz")
        onNodeWithText("No matching countries").assertExists()

        onNodeWithText("Clear search").performClick()
        onNodeWithText(country.name).assertExists()
        onNodeWithText(brazil.name).assertExists()
    }

    @Test
    fun testCountryInfoStaysVisibleWhileYearRefreshes() = runComposeUiTest {
        val state = CountryDetailsUIState.Success(country,
            "2023", listOf("2022", "2023"), countryEmissionsInfo, emptyList(),
            isRefreshing = true
        )
        setContent {
            CountryInfoDetailedView(state, onYearSelected = {})
        }

        onNodeWithText(formatEmissionsQuantity(emissionsQuantity)).assertExists()
        onNodeWithText("Emissions Summary").assertExists()
    }

    @Test
    fun testCountryInfoErrorOffersRetry() = runComposeUiTest {
        var retries = 0
        setContent {
            CountryInfoDetailedView(CountryDetailsUIState.Error("boom"), onRetry = { retries++ }, onYearSelected = {})
        }

        onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun testForestryToggleReportsNewValue() = runComposeUiTest {
        var include: Boolean? = null
        val state = CountryDetailsUIState.Success(country,
            year, listOf("2022", "2023"), countryEmissionsInfo, emptyList(),
            excludeForestry = true
        )
        setContent {
            CountryInfoDetailedView(state, onIncludeForestryChange = { include = it }, onYearSelected = {})
        }

        onNodeWithText("Matches the totals on climatetrace.org", useUnmergedTree = true).assertExists()
        onNodeWithText("Include forestry & land use", useUnmergedTree = true).performClick()
        assertEquals(true, include)
    }

    @Test
    fun testMonthlyChartShowsPeakMonth() = runComposeUiTest {
        val monthly = (1..12).map { MonthlyEmissions(year = 2022, month = it, emissionsQuantity = if (it == 7) 6_000_000.0 else 4_000_000.0) }
        val state = CountryDetailsUIState.Success(country,
            year, listOf("2022", "2023"), countryEmissionsInfo, emptyList(),
            monthlyEmissions = monthly
        )
        setContent {
            CountryInfoDetailedView(state, onYearSelected = {})
        }

        onNodeWithText("Monthly Emissions · 2022").assertExists()
        onNodeWithText("Peak in Jul: ${formatEmissionsQuantity(6_000_000.0)}").assertExists()
    }

    @Test
    fun testMapZoomFitsAllPoints() {
        val dublin = MapPoint(1, 53.43, -6.26, "Dublin Airport")
        val shannon = MapPoint(2, 52.62, -9.08, "Aughinish")
        val sydney = MapPoint(3, -33.87, 151.21, "Sydney")

        // Two sources ~200km apart fit a phone-width map at a regional zoom...
        assertTrue(zoomToFit(listOf(dublin, shannon), 328f, 220f, maxZoom = 10) in 6..8)
        // ...a single source goes as deep as it's allowed...
        assertEquals(10, zoomToFit(listOf(dublin), 328f, 220f, maxZoom = 10))
        // ...and opposite sides of the world fall back to the whole-world view.
        assertEquals(1, zoomToFit(listOf(dublin, sydney), 328f, 220f, maxZoom = 10))
    }

    @Test
    fun testYearInProgressIsLabelledAsYearToDate() = runComposeUiTest {
        val state = CountryDetailsUIState.Success(country,
            "2026", listOf("2025", "2026"), countryEmissionsInfo, emptyList(),
            partialThroughMonth = 8
        )
        setContent {
            CountryInfoDetailedView(state, onYearSelected = {})
        }

        onNodeWithText("2026 is still in progress: figures cover January to August.").assertExists()
        onNodeWithText("CO₂e · Jan–Aug").assertExists()
    }

    @Test
    fun testTappingASectorDrillsIntoItsSubsectors() = runComposeUiTest {
        val breakdown = listOf(
            CountryAssetEmissionsInfo(sector = "agriculture", emissionsQuantity = 30_000_000.0),
            CountryAssetEmissionsInfo(sector = "power", emissionsQuantity = 10_000_000.0),
            CountryAssetEmissionsInfo(sector = "agriculture", subsector = "enteric-fermentation-cattle-pasture", emissionsQuantity = 20_000_000.0),
            CountryAssetEmissionsInfo(sector = "agriculture", subsector = "manure-management", emissionsQuantity = 10_000_000.0),
            CountryAssetEmissionsInfo(sector = "power", subsector = "electricity-generation", emissionsQuantity = 10_000_000.0),
        )
        val state = CountryDetailsUIState.Success(country, year, listOf("2022", "2023"), countryEmissionsInfo, breakdown)
        setContent {
            CountryInfoDetailedView(state, onYearSelected = {})
        }

        onNodeWithText("Manure management").assertDoesNotExist()
        onNodeWithText("Agriculture").performScrollTo().performClick()

        onNodeWithText("Agriculture · 30.0 Mt").assertExists()
        onNodeWithText("Manure management").assertExists()
        onNodeWithText("Enteric fermentation cattle pasture").assertExists()
        // Only the tapped sector's subsectors, and the other sectors are out of the way.
        onNodeWithText("Electricity generation").assertDoesNotExist()
        onNodeWithText("Power").assertDoesNotExist()

        onNodeWithText("All sectors").performScrollTo().performClick()
        onNodeWithText("Power").assertExists()
        onNodeWithText("Manure management").assertDoesNotExist()
    }
}
