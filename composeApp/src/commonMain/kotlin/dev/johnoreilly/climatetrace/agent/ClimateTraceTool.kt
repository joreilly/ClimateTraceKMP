@file:OptIn(ExperimentalTime::class)

package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.ui.utils.alpha2ToAlpha3
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong
import kotlin.time.ExperimentalTime

// Tool results are read by the LLM, so label every figure and pre-convert to the units the
// system prompt asks for -- small on-device models misread raw tonnes like 1.8631950821185857E8.
private fun Double.oneDecimal() = (this * 10).roundToLong() / 10.0
private fun Double.toMillionTonnes() = "${(this / 1_000_000).oneDecimal()} million tonnes CO2e"

/** One line per requested country, in request order (the API returns ranking order). */
internal fun formatCountryEmissions(countryCodes: List<String>, year: String, infos: List<CountryEmissionsInfo>): String {
    val byCountry = infos.associateBy { it.country }
    return countryCodes.joinToString("\n") { code ->
        val info = byCountry[code]
        if (info == null) "$code ($year): no data" else "$code ($year): ${info.emissionsQuantity.toMillionTonnes()}"
    }
}

/** Sectors largest first, then the top subsectors; the API mixes both in one list. */
internal fun formatSectorEmissions(countryCode: String, year: String, summaries: List<CountryAssetEmissionsInfo>): String {
    val (subsectors, sectors) = summaries.partition { it.subsector != null }
    return buildString {
        appendLine("$countryCode ($year) emissions by sector, largest first:")
        sectors.sortedByDescending { it.emissionsQuantity }.forEach {
            appendLine("- ${it.sector}: ${it.emissionsQuantity.toMillionTonnes()} (${it.percentage.oneDecimal()}%)")
        }
        appendLine("Top subsectors:")
        subsectors.sortedByDescending { it.emissionsQuantity }.take(10).forEach {
            appendLine("- ${it.sector} / ${it.subsector}: ${it.emissionsQuantity.toMillionTonnes()} (${it.percentage.oneDecimal()}%)")
        }
    }.trimEnd()
}

private const val COUNTRY_CODES_DESCRIPTION = "ISO 3166-1 alpha-3 country code list (e.g., 'USA', 'GBR', 'IRL')"

// Models often send alpha-2 codes ('IE'); the API only accepts alpha-3.
internal fun normalizeCountryCode(code: String): String =
    code.trim().uppercase().let { if (it.length == 2) alpha2ToAlpha3(it) ?: it else it }

internal class GetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetEmissionsTool",
    description = "Get the total emissions for one or more countries for a particular year. " +
        "Use this for totals, comparisons, changes over time and per-capita figures (with PopulationAgent)."
) {
    @Serializable
    data class Args(
        @property:LLMDescription(COUNTRY_CODES_DESCRIPTION)
        val countryCodeList: List<String>,
        @property:LLMDescription("Year for which emissions occurred")
        val year: String
    )

    override suspend fun execute(args: Args): String {
        val codes = args.countryCodeList.map(::normalizeCountryCode)
        return formatCountryEmissions(codes, args.year, climateTraceRepository.fetchCountryEmissionsInfo(codes, args.year))
    }
}


internal class GetAssetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetAssetEmissionsTool.Args>(
    argsType = typeToken<Args>(),
    name = "GetAssetEmissionsTool",
    description = "Get the emissions broken down by sector (e.g. power, transportation, agriculture) for one or more " +
        "countries for a particular year. Only use this when a sector breakdown is needed; use GetEmissionsTool for totals."
) {
    @Serializable
    data class Args(
        @property:LLMDescription(COUNTRY_CODES_DESCRIPTION)
        val countryCodeList: List<String>,
        @property:LLMDescription("Year for which emissions occurred")
        val year: String
    )

    override suspend fun execute(args: Args): String {
        return args.countryCodeList.map(::normalizeCountryCode).map { code ->
            // Report a bad code back to the model so it can retry, rather than failing the whole call
            try {
                formatSectorEmissions(code, args.year, climateTraceRepository.fetchCountryAssetEmissionsInfo(code, args.year))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "$code (${args.year}): no sector data found; check this is an ISO 3166-1 alpha-3 country code"
            }
        }.joinToString("\n\n")
    }
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

