@file:OptIn(ExperimentalTime::class)

package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.annotations.LLMDescription
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.Country
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


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
        val result = climateTraceRepository.fetchCountryEmissionsInfo(args.countryCodeList, args.year)
        return result.joinToString {
            it.emissions.co2.toString()
        }
    }
}


class GetAssetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetAssetEmissionsTool.Args>() {
    @Serializable
    data class Args(
        @property:LLMDescription("ISO country code list (e.g., 'USA', 'GBR', 'FRA')")
        val countryCodeList: List<String>,
    )
    override val argsSerializer = Args.serializer()
    override val description = "Get the asset emission data for a country."

    override suspend fun doExecute(args: Args): String {
        return climateTraceRepository.fetchCountryAssetEmissionsInfo(args.countryCodeList).toString()
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
) : SimpleTool<CurrentDatetimeTool.Args>() {
    @Serializable
    data class Args(
        @property:LLMDescription("The timezone to get the current date and time in (e.g., 'UTC', 'America/New_York', 'Europe/London'). Defaults to UTC.")
        val timezone: String = "UTC"
    )

    override val argsSerializer = Args.serializer()

    override val name = "current_datetime"
    override val description = "Get the current date and time in the specified timezone"

    override suspend fun doExecute(args: Args): String {
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
