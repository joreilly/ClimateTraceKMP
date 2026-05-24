@file:OptIn(ExperimentalTime::class)

package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

class GetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetEmissionsTool",
    description = "Get the emission data for a country for a particular year."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ISO country code list (e.g., 'USA', 'GBR', 'FRA')")
        val countryCodeList: List<String>,
        @property:LLMDescription("Year for which emissions occurred")
        val year: String
    )

    override suspend fun execute(args: Args): String {
        return climateTraceRepository.fetchCountryEmissionsInfo(args.countryCodeList, args.year).joinToString {
            it.emissionsQuantity.toString()
        }
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

