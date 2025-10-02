package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.annotations.LLMDescription
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.Country
import kotlinx.serialization.Serializable


class GetCountryTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetCountryTool.Args>() {
    @Serializable
    data class Args(
        @property:LLMDescription("Country name")
        val countryName: String
    )

    override val argsSerializer = Args.serializer()
    override val description = "Look up country code using country name"

    private var countryList: List<Country>? = null

    override suspend fun doExecute(args: Args): String {
        try {
            if (countryList == null) {
                countryList = climateTraceRepository.fetchCountries()
            }
            return countryList?.firstOrNull { it.name == args.countryName }?.alpha3 ?: ""
        } catch (e: Exception) {
            println("Error: $e")
            return ""
        }
    }
}


class GetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetEmissionsTool.Args>() {
    @Serializable
    data class Args(
        @property:LLMDescription("ISO country code list (e.g., 'USA', 'GBR', 'FRA')")
        val countryCodeList: List<String>,
        @property:LLMDescription("Year for which emissions occurred")
        val year: String
    )

    override val argsSerializer = Args.serializer()
    override val description = "Get the emission data for a country for a particular year."

    override suspend fun doExecute(args: Args): String {
        try {
            return climateTraceRepository.fetchCountryEmissionsInfo(args.countryCodeList, args.year).joinToString {
                it.emissions.co2.toString()
            }
        } catch (e: Exception) {
            println("Error: $e")
            return ""
        }
    }
}


class GetPopulationTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetPopulationTool.Args>() {
    @Serializable
    data class Args(val countryCode: String)

    override val argsSerializer = Args.serializer()
    override val description = "Get population data for a country by its country code"

    override val descriptor = ToolDescriptor(
        name = "GetPopulationTool",
        description = "Get population data for a country by its country code",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "countryCode", description = "ISO country code (e.g., 'USA', 'GBR', 'FRA')", type = ToolParameterType.String
            )
        ),
    )

    override suspend fun doExecute(args: Args): String {
        try {
            val population = climateTraceRepository.getPopulation(args.countryCode)
            return population.toString()
        } catch (e: Exception) {
            println("Error: $e")
            return ""
        }
    }
}
