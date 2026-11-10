@file:OptIn(ExperimentalTime::class)

package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

// The Koog tools are thin wrappers: what they fetch and how the answer is worded lives in
// ClimateTraceReports, which the MCP server's tools share.

private const val COUNTRY_CODES_DESCRIPTION = "ISO 3166-1 alpha-3 country code list (e.g., 'USA', 'GBR', 'IRL')"
private const val COUNTRY_CODE_DESCRIPTION = "ISO 3166-1 alpha-3 country code (e.g., 'USA', 'GBR', 'IRL')"
private const val YEAR_DESCRIPTION = "Year for which emissions occurred"
private const val INCLUDE_FORESTRY_DESCRIPTION =
    "Set true only if the user asks for net emissions including forestry and land use. Default false."

internal class GetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetEmissionsTool",
    description = "Get the total emissions for one or more countries for a particular year, along with each country's " +
        "world rank, emissions per person, share of global emissions and change on the previous year. " +
        "Use this for totals, comparisons, changes over time and per-capita figures."
) {
    @Serializable
    data class Args(
        @property:LLMDescription(COUNTRY_CODES_DESCRIPTION)
        val countryCodeList: List<String>,
        @property:LLMDescription(YEAR_DESCRIPTION)
        val year: String,
        @property:LLMDescription(INCLUDE_FORESTRY_DESCRIPTION)
        val includeForestry: Boolean = false
    )

    override suspend fun execute(args: Args): String =
        ClimateTraceReports(climateTraceRepository).countryEmissions(args.countryCodeList, args.year, args.includeForestry)
}

internal class GetAssetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetAssetEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetAssetEmissionsTool",
    description = "Get the emissions broken down by sector and subsector (e.g. power, transportation, agriculture) for one " +
        "or more countries for a particular year. Only use this when a sector breakdown is needed; use GetEmissionsTool for totals."
) {
    @Serializable
    data class Args(
        @property:LLMDescription(COUNTRY_CODES_DESCRIPTION)
        val countryCodeList: List<String>,
        @property:LLMDescription(YEAR_DESCRIPTION)
        val year: String,
        @property:LLMDescription(INCLUDE_FORESTRY_DESCRIPTION)
        val includeForestry: Boolean = false
    )

    override suspend fun execute(args: Args): String =
        ClimateTraceReports(climateTraceRepository).sectorEmissions(args.countryCodeList, args.year, args.includeForestry)
}

internal class GetMonthlyEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetMonthlyEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetMonthlyEmissionsTool",
    description = "Get the month-by-month emissions for one or more countries for a particular year. " +
        "Only use this for questions about months or seasons; use GetEmissionsTool for yearly totals."
) {
    @Serializable
    data class Args(
        @property:LLMDescription(COUNTRY_CODES_DESCRIPTION)
        val countryCodeList: List<String>,
        @property:LLMDescription(YEAR_DESCRIPTION)
        val year: String,
        @property:LLMDescription(INCLUDE_FORESTRY_DESCRIPTION)
        val includeForestry: Boolean = false
    )

    override suspend fun execute(args: Args): String =
        ClimateTraceReports(climateTraceRepository).monthlyEmissions(args.countryCodeList, args.year, args.includeForestry)
}

internal class GetRankingsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetRankingsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetRankingsTool",
    description = "Get the countries with the highest emissions in a particular year, either by total emissions or by " +
        "emissions per person. Use this for 'top emitters' and 'who emits the most' questions."
) {
    @Serializable
    data class Args(
        @property:LLMDescription(YEAR_DESCRIPTION)
        val year: String,
        @property:LLMDescription("How many countries to return, from 1 to 50. Default 10.")
        val limit: Int = 10,
        @property:LLMDescription("Set true to rank by emissions per person instead of total emissions. Default false.")
        val perCapita: Boolean = false,
        @property:LLMDescription(INCLUDE_FORESTRY_DESCRIPTION)
        val includeForestry: Boolean = false
    )

    override suspend fun execute(args: Args): String =
        ClimateTraceReports(climateTraceRepository).rankings(args.year, args.limit, args.perCapita, args.includeForestry)
}

internal class GetCountrySourcesTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetCountrySourcesTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetCountrySourcesTool",
    description = "Get the largest individual emission sources in a country (power stations, airports, factories, " +
        "mines and so on), with each one's source id, sector, emissions and location."
) {
    @Serializable
    data class Args(
        @property:LLMDescription(COUNTRY_CODE_DESCRIPTION)
        val countryCode: String,
        @property:LLMDescription("How many sources to return, from 1 to 50. Default 10.")
        val limit: Int = 10,
        @property:LLMDescription("Year for which emissions occurred. Leave out for the latest year.")
        val year: String? = null,
        @property:LLMDescription(INCLUDE_FORESTRY_DESCRIPTION)
        val includeForestry: Boolean = false
    )

    override suspend fun execute(args: Args): String =
        ClimateTraceReports(climateTraceRepository).countrySources(args.countryCode, args.year, args.limit, args.includeForestry)
}

internal class GetSourceDetailsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetSourceDetailsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetSourceDetailsTool",
    description = "Get the details of one emission source by its source id: owners, location, yearly and monthly " +
        "emissions, activity and capacity. Get source ids from GetCountrySourcesTool."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Source id, as returned by GetCountrySourcesTool")
        val sourceId: Int
    )

    override suspend fun execute(args: Args): String =
        ClimateTraceReports(climateTraceRepository).sourceDetails(args.sourceId)
}

internal class GetPopulationTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetPopulationTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetPopulationTool",
    description = "Get population data for a country by its country code"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ISO country code (e.g., 'USA', 'GBR', 'FRA')")
        val countryCode: String
    )

    override suspend fun execute(args: Args): String {
        println("Getting population for ${args.countryCode}")
        try {
            val population = climateTraceRepository.getPopulation(args.countryCode)
            return population.toString()
        } catch (e: Exception) {
            println("Error: $e")
            return ""
        }
    }
}
