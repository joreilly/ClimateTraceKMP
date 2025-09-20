package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import kotlinx.serialization.Serializable


class GetCountriesTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<ToolArgs.Empty>() {
    override val argsSerializer = ToolArgs.Empty.serializer()

    override val descriptor = ToolDescriptor(
        name = "get_countries",
        description = "Get the list of countries",
    )

    override suspend fun doExecute(args: ToolArgs.Empty): String {
        val countries = climateTraceRepository.fetchCountries()
        return countries.joinToString { "${it.name}, ${it.alpha3}\n" }
    }
}


class GetEmissionsTool(val climateTraceRepository: ClimateTraceRepository) : SimpleTool<GetEmissionsTool.Args>() {
    @Serializable
    data class Args(val countryCode: String, val year: String) : ToolArgs

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "get_emissions",
        description = "Get the emission data for a country for a particular year.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "countryCode", description = "country code", type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "year", description = "year", type = ToolParameterType.String
            )
        ),
    )

    override suspend fun doExecute(args: Args): String {
        return climateTraceRepository.fetchCountryEmissionsInfo(args.countryCode, args.year).joinToString {
            it.emissions.co2.toString()
        }
    }
}