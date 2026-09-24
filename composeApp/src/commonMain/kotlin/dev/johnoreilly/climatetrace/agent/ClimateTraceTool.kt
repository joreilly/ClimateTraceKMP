@file:OptIn(ExperimentalTime::class)

package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import io.github.koalaplot.core.util.toString
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

class GetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetEmissionsTool",
    description = "Get total emissions for one or more countries for a particular year. " +
        "Pass all the countries needed in a single call."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ISO country code list (e.g., 'USA', 'GBR', 'FRA')")
        val countryCodeList: List<String>,
        @property:LLMDescription("Year for which emissions occurred")
        val year: String
    )

    // Labelled per country (the bare numbers used before couldn't be attributed, so the LLM fell
    // back to one call per country) and already converted to the unit the agent reports in.
    override suspend fun execute(args: Args): String =
        climateTraceRepository.fetchCountryEmissionsInfo(args.countryCodeList, args.year)
            .formatPerCountry(args.countryCodeList) { "${(it.emissionsQuantity / 1_000_000).toString(2)} Mt CO2e" }
}

class GetPerCapitaEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetPerCapitaEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetPerCapitaEmissionsTool",
    description = "Get per capita emissions (tonnes of CO2e per person) for one or more countries for a " +
        "particular year. Pass all the countries needed in a single call. Use this rather than " +
        "dividing emissions by population."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ISO country code list (e.g., 'USA', 'GBR', 'FRA')")
        val countryCodeList: List<String>,
        @property:LLMDescription("Year for which emissions occurred")
        val year: String
    )

    // ClimateTrace's own per-capita figure (as shown on the Rankings screen), from the same single
    // rankings request as GetEmissionsTool - no population lookups needed.
    override suspend fun execute(args: Args): String =
        climateTraceRepository.fetchCountryEmissionsInfo(args.countryCodeList, args.year)
            .formatPerCountry(args.countryCodeList) { "${it.emissionsPerCapita.toString(2)} t CO2e per person" }
}

private fun List<CountryEmissionsInfo>.formatPerCountry(
    requested: List<String>,
    value: (CountryEmissionsInfo) -> String,
): String {
    val found = associateBy { it.country }
    return requested.joinToString("\n") { code ->
        found[code]?.let { "$code (${it.name ?: code}): ${value(it)}" } ?: "$code: no data"
    }
}


class GetAssetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetAssetEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetAssetEmissionsTool",
    description = "Get the asset emission data for a country."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ISO country code list (e.g., 'USA', 'GBR', 'FRA')")
        val countryCodeList: List<String>,
        @property:LLMDescription("Year for which emissions occurred")
        val year: String
    )

    override suspend fun execute(args: Args): String {
        return climateTraceRepository.fetchCountryAssetEmissionsInfo(args.countryCodeList, args.year).toString()
    }
}

class GetPopulationTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetPopulationTool.Args>(
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

