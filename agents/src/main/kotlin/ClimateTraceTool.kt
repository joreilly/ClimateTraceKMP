import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository



@LLMDescription("Tools for getting climate emission information")
class ClimateTraceTool : ToolSet {
    val climateTraceRepository = koin.get<ClimateTraceRepository>()

    @Tool
    @LLMDescription("Get the list of countries")
    suspend fun getCountries(): List<String> {
        val countries = climateTraceRepository.fetchCountries()
        return countries.map { "${it.name}, ${it.alpha3}" }
    }


    @Tool(customName = "getEmissions")
    @LLMDescription("Get the emission data for a country for a particular year.")
    suspend fun getEmissions(
        @LLMDescription("country code") countryCode: String,
        @LLMDescription("year") year: String
    ): List<String> {
        return climateTraceRepository.fetchCountryEmissionsInfo(countryCode, year).map {
            it.emissions.co2.toString()
        }
    }
}