package dev.johnoreilly.climatetrace.agent

import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
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
            "DEU (2023): 690.0 million tonnes CO2e\n" +
                "JPN (2023): 1263.4 million tonnes CO2e\n" +
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
}
