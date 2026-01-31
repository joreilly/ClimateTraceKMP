@file:OptIn(ExperimentalTime::class)

package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.Country
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class GetCountryTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetCountryTool.Args>(
    argsSerializer = Args.serializer(),
    name = "GetCountryTool",
    description = "Look up country code using country name"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Country name")
        val countryName: String
    )

    private var countryList: List<Country>? = null

    override suspend fun execute(args: Args): String {
        try {
            if (countryList == null) {
                countryList = climateTraceRepository.fetchCountries()
            }
            val result = countryList?.firstOrNull { it.name == args.countryName }?.alpha3 ?: ""
            return result
        } catch (e: Exception) {
            println("Error: $e")
            return ""
        }
    }
}


class GetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetEmissionsTool.Args>(
    argsSerializer = Args.serializer(),
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
            it.emissions.co2.toString()
        }
    }
}


class GetAssetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetAssetEmissionsTool.Args>(
    argsSerializer = Args.serializer(),
    name = "GetAssetEmissionsTool",
    description = "Get the asset emission data for a country."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ISO country code list (e.g., 'USA', 'GBR', 'FRA')")
        val countryCodeList: List<String>,
    )

    override suspend fun execute(args: Args): String {
        return climateTraceRepository.fetchCountryAssetEmissionsInfo(args.countryCodeList).toString()
    }
}

class GetPopulationTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetPopulationTool.Args>(
    argsSerializer = Args.serializer(),
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


/**
 * Tool for getting the current date and time
 */
class CurrentDatetimeTool(
    val defaultTimeZone: TimeZone = TimeZone.UTC,
    val clock: Clock = Clock.System,
) : SimpleTool<CurrentDatetimeTool.Args>(
    argsSerializer = Args.serializer(),
    name = "current_datetime",
    description = "Get the current date and time in the specified timezone"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The timezone to get the current date and time in (e.g., 'UTC', 'America/New_York', 'Europe/London'). Defaults to UTC.")
        val timezone: String = "UTC"
    )

    override suspend fun execute(args: Args): String {
        val zoneId = try {
            TimeZone.of(args.timezone)
        } catch (_: Exception) {
            defaultTimeZone
        }

        val now = clock.now()
        val localDateTime = now.toLocalDateTime(zoneId)
        val offset = zoneId.offsetAt(now)

        val time = localDateTime.time
        val timeStr = "${time.hour.toString().padStart(2, '0')}:${
            time.minute.toString().padStart(2, '0')
        }:${time.second.toString().padStart(2, '0')}"

        return "${localDateTime.date}T$timeStr$offset"
    }
}
