package dev.johnoreilly.climatetrace.agent

import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.data.DataCoverage
import dev.johnoreilly.climatetrace.remote.Asset
import dev.johnoreilly.climatetrace.remote.AssetDetail
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo
import dev.johnoreilly.climatetrace.remote.EmissionMeasurement
import dev.johnoreilly.climatetrace.ui.utils.alpha2ToAlpha3
import kotlinx.coroutines.CancellationException
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.native.HiddenFromObjC

// Everything here is text for an LLM to read, shared by the in-app Koog agent's tools and the
// MCP server's. So: label every figure, name the unit, and say what basis the totals are on --
// small on-device models misread raw tonnes like 1.8631950821185857E8.

private val MonthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private fun monthName(month: Int) = MonthNames.getOrNull(month - 1) ?: "month $month"

private fun Double.oneDecimal() = (this * 10).roundToLong() / 10.0
private fun Double.fourDecimals() = (this * 10_000).roundToLong() / 10_000.0

// Country totals are best read in millions of tonnes, but a single source or a small sector
// would round to "0.0 million", so step down a unit rather than lose the figure.
internal fun Double.toEmissionsText(): String {
    val magnitude = abs(this)
    return when {
        magnitude >= 1_000_000 -> "${(this / 1_000_000).oneDecimal()} million tonnes CO2e"
        magnitude >= 1_000 -> "${(this / 1_000).oneDecimal()} thousand tonnes CO2e"
        else -> "${this.oneDecimal()} tonnes CO2e"
    }
}

// Models often send alpha-2 codes ('IE'); the API only accepts alpha-3.
internal fun normalizeCountryCode(code: String): String =
    code.trim().uppercase().let { if (it.length == 2) alpha2ToAlpha3(it) ?: it else it }

internal fun basisNote(includeForestry: Boolean): String = if (includeForestry) {
    "Totals include the forestry and land use sector, so they are net of land-use carbon sinks."
} else {
    "Totals exclude the forestry and land use sector, as on climatetrace.org."
}

/** One line per requested country, in request order (the API returns ranking order). */
internal fun formatCountryEmissions(countryCodes: List<String>, year: String, infos: List<CountryEmissionsInfo>): String {
    val byCountry = infos.associateBy { it.country }
    return countryCodes.joinToString("\n") { code ->
        val info = byCountry[code] ?: return@joinToString "$code ($year): no data"
        buildList {
            add(info.emissionsQuantity.toEmissionsText())
            add("rank ${info.rank} of all countries")
            if (info.emissionsPerCapita != 0.0) add("${info.emissionsPerCapita.oneDecimal()} tonnes CO2e per person")
            if (info.percentage != 0.0) add("${info.percentage.oneDecimal()}% of global emissions")
            if (info.emissionsPercentChange != 0.0) add("${info.emissionsPercentChange.oneDecimal()}% change on the previous year")
        }.joinToString(prefix = "$code ($year): ", separator = "; ")
    }
}

/** Sectors largest first, then the top subsectors; the API mixes both in one list. */
internal fun formatSectorEmissions(countryCode: String, year: String, summaries: List<CountryAssetEmissionsInfo>): String {
    val (subsectors, sectors) = summaries.partition { it.subsector != null }
    return buildString {
        appendLine("$countryCode ($year) emissions by sector, largest first:")
        sectors.sortedByDescending { it.emissionsQuantity }.forEach {
            appendLine("- ${it.sector}: ${it.emissionsQuantity.toEmissionsText()} (${it.percentage.oneDecimal()}%)")
        }
        appendLine("Top subsectors:")
        subsectors.sortedByDescending { it.emissionsQuantity }.take(10).forEach {
            appendLine("- ${it.sector} / ${it.subsector}: ${it.emissionsQuantity.toEmissionsText()} (${it.percentage.oneDecimal()}%)")
        }
    }.trimEnd()
}

/** `months` are (month 1..12, tonnes). */
internal fun formatMonthlyEmissions(subject: String, year: String, months: List<Pair<Int, Double>>): String {
    if (months.isEmpty()) return "$subject ($year): no monthly data"
    return buildString {
        appendLine("$subject ($year) emissions by month:")
        months.sortedBy { it.first }.forEach { (month, tonnes) ->
            appendLine("- ${monthName(month)}: ${tonnes.toEmissionsText()}")
        }
        if (months.size < 12) appendLine("Only ${months.size} months of $year have data so far.")
    }.trimEnd()
}

internal fun formatRankings(year: String, rankings: List<CountryEmissionsInfo>, perCapita: Boolean, limit: Int): String {
    val ordered = if (perCapita) {
        rankings.filter { it.emissionsPerCapita > 0 }.sortedByDescending { it.emissionsPerCapita }
    } else {
        rankings.sortedByDescending { it.emissionsQuantity }
    }.take(limit)
    if (ordered.isEmpty()) return "No rankings data for $year"
    val measure = if (perCapita) "emissions per person" else "total emissions"
    return buildString {
        appendLine("Top ${ordered.size} countries by $measure in $year:")
        ordered.forEachIndexed { index, info ->
            val label = listOfNotNull(info.country, info.name).joinToString(" ")
            val figures = if (perCapita) {
                "${info.emissionsPerCapita.oneDecimal()} tonnes CO2e per person (total ${info.emissionsQuantity.toEmissionsText()})"
            } else {
                "${info.emissionsQuantity.toEmissionsText()} (${info.percentage.oneDecimal()}% of global emissions)"
            }
            appendLine("${index + 1}. $label: $figures")
        }
    }.trimEnd()
}

internal fun formatSources(countryCode: String, assets: List<Asset>): String {
    if (assets.isEmpty()) return "$countryCode: no emission sources found"
    val year = assets.firstNotNullOfOrNull { it.year }
    return buildString {
        appendLine("Largest emission sources in $countryCode${year?.let { " ($it)" }.orEmpty()}, largest first:")
        assets.forEachIndexed { index, asset ->
            val details = listOfNotNull(
                listOfNotNull(asset.sector, asset.subsector).joinToString(" / ").takeIf { it.isNotEmpty() },
                asset.emissionsQuantity?.toEmissionsText(),
                asset.centroid?.let { c ->
                    val lat = c.latitude
                    val lon = c.longitude
                    if (lat != null && lon != null) "at latitude ${lat.fourDecimals()}, longitude ${lon.fourDecimals()}" else null
                }
            ).joinToString("; ")
            appendLine("${index + 1}. ${asset.name} (source id ${asset.id}): $details")
        }
    }.trimEnd()
}

internal fun formatSourceDetails(detail: AssetDetail, monthly: List<EmissionMeasurement>, monthlyYear: Int?): String = buildString {
    appendLine("${detail.name} (source id ${detail.id})")
    detail.country?.let { appendLine("Country: $it") }
    listOfNotNull(detail.sector, detail.subsector).joinToString(" / ").takeIf { it.isNotEmpty() }?.let { appendLine("Sector: $it") }
    detail.assetType?.takeIf { it.isNotBlank() }?.let { appendLine("Type: $it") }
    detail.owners.orEmpty().mapNotNull { it.name?.takeIf(String::isNotBlank) }.distinct().takeIf { it.isNotEmpty() }
        ?.let { appendLine("Owners: ${it.joinToString()}") }
    detail.centroid?.let { c ->
        val lat = c.latitude
        val lon = c.longitude
        if (lat != null && lon != null) appendLine("Location: latitude ${lat.fourDecimals()}, longitude ${lon.fourDecimals()}")
    }
    detail.emissions.orEmpty().filter { it.emissionsQuantity != null }.sortedBy { it.year }.forEach { e ->
        val extras = listOfNotNull(
            e.activity?.let { "activity ${it.oneDecimal()} ${e.activityUnits.orEmpty()}".trim() },
            e.capacity?.let { "capacity ${it.oneDecimal()} ${e.capacityUnits.orEmpty()}".trim() }
        ).joinToString("; ")
        appendLine("Emissions ${e.year ?: ""}: ${e.emissionsQuantity!!.toEmissionsText()}${if (extras.isEmpty()) "" else " ($extras)"}".replace("  ", " "))
    }
    detail.subsectorRanks.orEmpty().maxByOrNull { it.year }?.let {
        appendLine("Rank among sources in its subsector worldwide (${it.year}): ${it.rank}")
    }
    val months = monthly.mapNotNull { m ->
        val month = m.month
        val quantity = m.emissionsQuantity
        if (month != null && quantity != null) month to quantity else null
    }
    if (months.isNotEmpty() && monthlyYear != null) {
        appendLine(formatMonthlyEmissions(detail.name, monthlyYear.toString(), months))
    }
}.trimEnd()

internal fun formatDataCoverage(coverage: DataCoverage): String = buildString {
    val complete = coverage.completeYears
    appendLine("Complete years available: ${complete.first()} to ${complete.last()}.")
    val partialYear = coverage.partialYear
    val throughMonth = coverage.partialThroughMonth
    if (partialYear != null && throughMonth != null) {
        appendLine("$partialYear is still in progress: data covers January to ${monthName(throughMonth)} only, so its totals are year to date.")
    }
    coverage.metadata?.dataVersion?.let { version ->
        appendLine("Climate TRACE data release $version${coverage.metadata.releaseDate?.let { ", released $it" }.orEmpty()}.")
    }
}.trimEnd()

/**
 * Plain-text answers to the questions an LLM asks about the Climate TRACE data. Each method
 * fetches what it needs and never throws for bad input: it says what was wrong so the model can
 * correct itself and call again.
 *
 * `includeForestry` defaults to false everywhere, matching the app and climatetrace.org.
 */
// Public for mcp-server, not needed from Swift
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
class ClimateTraceReports(private val repository: ClimateTraceRepository) {

    suspend fun dataCoverage(): String = formatDataCoverage(repository.fetchDataCoverage())

    suspend fun countryEmissions(countryCodes: List<String>, year: String, includeForestry: Boolean = false): String =
        report("No emissions data found for $year") {
            val codes = countryCodes.map(::normalizeCountryCode)
            val infos = repository.fetchCountryEmissionsInfo(codes, year, excludeForestry = !includeForestry)
            withNotes(formatCountryEmissions(codes, year, infos), year, includeForestry)
        }

    suspend fun sectorEmissions(countryCodes: List<String>, year: String, includeForestry: Boolean = false): String {
        val sections = countryCodes.map(::normalizeCountryCode).map { code ->
            // Report a bad code back to the model so it can retry, rather than failing the whole call
            report("$code ($year): no sector data found; check this is an ISO 3166-1 alpha-3 country code") {
                val breakdown = repository.fetchCountryEmissionsBreakdown(code, year, excludeForestry = !includeForestry)
                formatSectorEmissions(code, year, breakdown.sectors?.summaries.orEmpty() + breakdown.subsectors?.summaries.orEmpty())
            }
        }
        return withNotes(sections.joinToString("\n\n"), year, includeForestry)
    }

    suspend fun monthlyEmissions(countryCodes: List<String>, year: String, includeForestry: Boolean = false): String {
        val sections = countryCodes.map(::normalizeCountryCode).map { code ->
            report("$code ($year): no monthly data found; check this is an ISO 3166-1 alpha-3 country code") {
                val breakdown = repository.fetchCountryEmissionsBreakdown(code, year, excludeForestry = !includeForestry)
                formatMonthlyEmissions(code, year, breakdown.totals?.timeseries.orEmpty().map { it.month to it.emissionsQuantity })
            }
        }
        return sections.joinToString("\n\n") + "\n" + basisNote(includeForestry)
    }

    suspend fun rankings(year: String, limit: Int = 10, perCapita: Boolean = false, includeForestry: Boolean = false): String =
        report("No rankings data found for $year") {
            val rankings = repository.fetchRankings(year, excludeForestry = !includeForestry).rankings
            withNotes(formatRankings(year, rankings, perCapita, limit.coerceIn(1, MAX_RANKINGS)), year, includeForestry)
        }

    suspend fun countrySources(countryCode: String, year: String? = null, limit: Int = 10, includeForestry: Boolean = false): String {
        val code = normalizeCountryCode(countryCode)
        return report("$code: no emission sources found; check this is an ISO 3166-1 alpha-3 country code") {
            val assets = repository.fetchAssetsByCountry(code, limit.coerceIn(1, MAX_SOURCES), excludeForestry = !includeForestry, year = year)
            formatSources(code, assets) + "\nUse a source id to ask for that source's details."
        }
    }

    suspend fun sourceDetails(sourceId: Int): String =
        report("No emission source found with id $sourceId") {
            val detail = repository.fetchAssetDetail(sourceId)
            val year = detail.emissions?.mapNotNull { it.year }?.maxOrNull()
            // The monthly breakdown is a bonus: without it the details still stand.
            val monthly = if (year == null) emptyList() else optional { repository.fetchAssetMonthlyEmissions(sourceId, year) }.orEmpty()
            formatSourceDetails(detail, monthly, year)
        }

    private suspend fun withNotes(body: String, year: String, includeForestry: Boolean): String {
        val coverage = repository.fetchDataCoverage()
        val throughMonth = coverage.partialThroughMonth
        val partialNote = if (year == coverage.partialYear && throughMonth != null) {
            "\n$year is still in progress: these figures cover January to ${monthName(throughMonth)} only."
        } else {
            ""
        }
        return body + "\n" + basisNote(includeForestry) + partialNote
    }

    private suspend fun report(onFailure: String, block: suspend () -> String): String =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onFailure
        }

    private suspend fun <T> optional(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private companion object {
        const val MAX_RANKINGS = 50
        const val MAX_SOURCES = 50
    }
}
