package adk

import com.google.adk.kt.annotations.Param
import com.google.adk.kt.annotations.Tool
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import koin

class ClimateTraceTool {
    private val climateTraceRepository = koin.get<ClimateTraceRepository>()

    @Tool
    suspend fun getCountries(): Map<String, String> {
        return mapOf("countries" to climateTraceRepository.fetchCountries().toString())
    }

    @Tool
    suspend fun getEmissions(
        @Param("Three letter country code") countryCode: String,
        @Param("Year to fetch emissions data for") year: String,
    ): Map<String, String> {
        return mapOf("emissions" to climateTraceRepository.fetchCountryEmissionsInfo(countryCode, year).toString())
    }
}
