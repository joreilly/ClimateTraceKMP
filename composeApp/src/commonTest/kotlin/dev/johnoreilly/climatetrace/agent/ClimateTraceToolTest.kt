package dev.johnoreilly.climatetrace.agent

import dev.johnoreilly.climatetrace.data.DataCoverage
import dev.johnoreilly.climatetrace.remote.Asset
import dev.johnoreilly.climatetrace.remote.Centroid
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.DataMetadata
import dev.johnoreilly.climatetrace.remote.TemporalCoverage
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class ClimateTraceToolTest {

    @Test
    fun countryEmissionsAreLabelledInRequestOrder() {
        // API returns ranking order (JPN ahead of DEU), which models used to misattribute
        val infos = listOf(
            CountryEmissionsInfo(country = "JPN", rank = 5, emissionsQuantity = 1_263_401_642.8),
            CountryEmissionsInfo(country = "DEU", rank = 12, emissionsQuantity = 690_013_433.3),
        )

        assertEquals(
            "DEU (2023): 690.0 million tonnes CO2e; rank 12 of all countries\n" +
                "JPN (2023): 1263.4 million tonnes CO2e; rank 5 of all countries\n" +
                "ATL (2023): no data",
            formatCountryEmissions(listOf("DEU", "JPN", "ATL"), "2023", infos),
        )
    }

    @Test
    fun countryCodesAreNormalizedToAlpha3() {
        assertEquals("IRL", normalizeCountryCode("ie"))
        assertEquals("IRL", normalizeCountryCode(" irl "))
        assertEquals("IRE", normalizeCountryCode("IRE")) // not a code; left for the API to reject
    }

    @Test
    fun sectorEmissionsAreSortedWithSubsectorsSeparate() {
        val summaries = listOf(
            CountryAssetEmissionsInfo(sector = "power", gas = "co2e_100yr", emissionsQuantity = 9_602_565.6, percentage = 11.776),
            CountryAssetEmissionsInfo(sector = "agriculture", gas = "co2e_100yr", emissionsQuantity = 30_007_958.9, percentage = 36.800),
            CountryAssetEmissionsInfo(sector = "agriculture", subsector = "enteric-fermentation-cattle-pasture", gas = "co2e_100yr", emissionsQuantity = 12_345_678.0, percentage = 15.14),
        )

        assertEquals(
            """
            IRL (2024) emissions by sector, largest first:
            - agriculture: 30.0 million tonnes CO2e (36.8%)
            - power: 9.6 million tonnes CO2e (11.8%)
            Top subsectors:
            - agriculture / enteric-fermentation-cattle-pasture: 12.3 million tonnes CO2e (15.1%)
            """.trimIndent(),
            formatSectorEmissions("IRL", "2024", summaries),
        )
    }

    @Test
    fun countryEmissionsIncludeRankPerCapitaShareAndChange() {
        val infos = listOf(
            CountryEmissionsInfo(
                country = "IRL", rank = 73, emissionsQuantity = 70_704_546.5,
                emissionsPerCapita = 14.18, percentage = 0.123, emissionsPercentChange = -0.48,
            ),
        )

        assertEquals(
            "IRL (2025): 70.7 million tonnes CO2e; rank 73 of all countries; 14.2 tonnes CO2e per person; " +
                "0.1% of global emissions; -0.5% change on the previous year",
            formatCountryEmissions(listOf("IRL"), "2025", infos),
        )
    }

    @Test
    fun smallQuantitiesStepDownAUnitInsteadOfRoundingToZero() {
        assertEquals("2.9 million tonnes CO2e", 2_917_272.7.toEmissionsText())
        assertEquals("129.2 thousand tonnes CO2e", 129_200.0.toEmissionsText())
        assertEquals("812.5 tonnes CO2e", 812.5.toEmissionsText())
        assertEquals("-18.5 million tonnes CO2e", (-18_531_033.9).toEmissionsText())
    }

    @Test
    fun monthlyEmissionsAreNamedAndFlagAPartYear() {
        assertEquals(
            """
            IRL (2026) emissions by month:
            - January: 5.9 million tonnes CO2e
            - February: 5.4 million tonnes CO2e
            Only 2 months of 2026 have data so far.
            """.trimIndent(),
            formatMonthlyEmissions("IRL", "2026", listOf(2 to 5_380_000.0, 1 to 5_870_000.0)),
        )
        assertEquals("IRL (2014): no monthly data", formatMonthlyEmissions("IRL", "2014", emptyList()))
    }

    @Test
    fun rankingsCanBeByTotalOrPerPerson() {
        val rankings = listOf(
            CountryEmissionsInfo(country = "CHN", name = "China", rank = 1, emissionsQuantity = 17_179_683_185.0, emissionsPerCapita = 12.18, percentage = 29.8),
            CountryEmissionsInfo(country = "QAT", name = "Qatar", rank = 40, emissionsQuantity = 180_000_000.0, emissionsPerCapita = 60.04, percentage = 0.3),
            CountryEmissionsInfo(country = "ATA", name = "Antarctica", rank = 250, emissionsQuantity = 1_000.0, emissionsPerCapita = 0.0),
        )

        assertEquals(
            """
            Top 2 countries by total emissions in 2025:
            1. CHN China: 17179.7 million tonnes CO2e (29.8% of global emissions)
            2. QAT Qatar: 180.0 million tonnes CO2e (0.3% of global emissions)
            """.trimIndent(),
            formatRankings("2025", rankings, perCapita = false, limit = 2),
        )
        // No per-person figure means no place in the per-person ranking
        assertEquals(
            """
            Top 2 countries by emissions per person in 2025:
            1. QAT Qatar: 60.0 tonnes CO2e per person (total 180.0 million tonnes CO2e)
            2. CHN China: 12.2 tonnes CO2e per person (total 17179.7 million tonnes CO2e)
            """.trimIndent(),
            formatRankings("2025", rankings, perCapita = true, limit = 10),
        )
    }

    @Test
    fun sourcesListTheIdNeededToAskForDetails() {
        val assets = listOf(
            Asset(
                id = 3166430, name = "Dublin Airport", sector = "transportation", subsector = "international-aviation",
                emissionsQuantity = 2_917_272.7, year = 2025, centroid = Centroid(latitude = 53.428713, longitude = -6.262121),
            ),
        )

        assertEquals(
            """
            Largest emission sources in IRL (2025), largest first:
            1. Dublin Airport (source id 3166430): transportation / international-aviation; 2.9 million tonnes CO2e; at latitude 53.4287, longitude -6.2621
            """.trimIndent(),
            formatSources("IRL", assets),
        )
    }

    @Test
    fun dataCoverageSpellsOutThePartYear() {
        val coverage = DataCoverage.from(
            DataMetadata(dataVersion = "v5.12.0", releaseDate = "2026-10-28", temporalCoverage = TemporalCoverage(start = "2015-01", end = "2026-08"))
        )

        assertEquals(
            """
            Complete years available: 2016 to 2025.
            2026 is still in progress: data covers January to August only, so its totals are year to date.
            Climate TRACE data release v5.12.0, released 2026-10-28.
            """.trimIndent(),
            formatDataCoverage(coverage),
        )
    }
}
