package dev.johnoreilly.climatetrace.agent

import dev.johnoreilly.climatetrace.remote.AggregatedEmissionsOverview
import dev.johnoreilly.climatetrace.remote.AggregatedEmissionsResponse
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.MonthlyEmissions
import kotlin.test.Test
import kotlin.test.assertEquals

class EmissionsDashboardTest {

    @Test
    fun sectorsAreLargestFirstWithoutSubsectorsAndMonthsInOrder() {
        val breakdown = AggregatedEmissionsResponse(
            totals = AggregatedEmissionsOverview(
                summaries = listOf(CountryAssetEmissionsInfo(gas = "co2e_100yr", emissionsQuantity = 40_000_000.0, percentage = 100.0)),
                timeseries = listOf(
                    MonthlyEmissions(year = 2024, month = 2, emissionsQuantity = 3_000_000.0),
                    MonthlyEmissions(year = 2024, month = 1, emissionsQuantity = 4_000_000.0),
                ),
            ),
            sectors = AggregatedEmissionsOverview(
                summaries = listOf(
                    CountryAssetEmissionsInfo(sector = "power", emissionsQuantity = 10_000_000.0, percentage = 25.0),
                    CountryAssetEmissionsInfo(sector = "agriculture", emissionsQuantity = 30_000_000.0, percentage = 75.0),
                    CountryAssetEmissionsInfo(sector = "agriculture", subsector = "rice-cultivation", emissionsQuantity = 1.0, percentage = 0.0),
                ),
            ),
        )

        assertEquals(
            CountryDashboard(
                code = "IRL",
                name = "Ireland",
                totalTonnes = 40_000_000.0,
                sectors = listOf(
                    SectorTotal("agriculture", 30_000_000.0, 75.0),
                    SectorTotal("power", 10_000_000.0, 25.0),
                ),
                monthly = listOf(MonthTotal(1, 4_000_000.0), MonthTotal(2, 3_000_000.0)),
            ),
            countryDashboard("IRL", "Ireland", breakdown),
        )
    }

    @Test
    fun totalFallsBackToTheSumOfSectors() {
        val breakdown = AggregatedEmissionsResponse(
            sectors = AggregatedEmissionsOverview(
                summaries = listOf(
                    CountryAssetEmissionsInfo(sector = "power", emissionsQuantity = 1.5),
                    CountryAssetEmissionsInfo(sector = "waste", emissionsQuantity = 2.5),
                ),
            ),
        )

        assertEquals(4.0, countryDashboard("IRL", null, breakdown).totalTonnes)
    }
}
