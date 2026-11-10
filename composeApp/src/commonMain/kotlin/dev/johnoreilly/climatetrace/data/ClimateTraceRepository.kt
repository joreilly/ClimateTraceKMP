package dev.johnoreilly.climatetrace.data

import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryEmissionsRankingsResponse
import dev.johnoreilly.climatetrace.remote.DataMetadata
import dev.johnoreilly.climatetrace.remote.IssPosition
import dev.johnoreilly.climatetrace.remote.IssPositionApi
import dev.johnoreilly.climatetrace.remote.PopulationApi
import dev.johnoreilly.climatetrace.remote.ReverseGeocodeApi
import dev.johnoreilly.climatetrace.ui.utils.alpha2ToAlpha3
import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC


// The calendar years the app offers, derived from the API's /metadata temporal coverage: the
// most recent ones, ending with the current year even when it is still in progress.
data class DataCoverage(
    val years: List<String>,
    // The latest *complete* year, and so the default: a part-year total isn't comparable.
    val latestYear: String,
    val metadata: DataMetadata? = null,
    // Set when the last of `years` is still in progress, with the last month it has data for.
    val partialYear: String? = null,
    val partialThroughMonth: Int? = null,
) {
    val completeYears: List<String> get() = years.filter { it != partialYear }

    companion object {
        private const val MAX_YEARS_BACK = 10

        // Used when /metadata can't be reached.
        val Fallback = DataCoverage(years = (2016..2025).map { it.toString() }, latestYear = "2025")

        fun from(metadata: DataMetadata): DataCoverage {
            val start = metadata.temporalCoverage?.start?.split("-")?.mapNotNull { it.toIntOrNull() }
            val end = metadata.temporalCoverage?.end?.split("-")?.mapNotNull { it.toIntOrNull() }
            val startYear = start?.getOrNull(0)
            val endYear = end?.getOrNull(0)
            if (startYear == null || endYear == null) return Fallback.copy(metadata = metadata)
            // "2015-01" starts a full year; anything later in the year doesn't. Likewise for the end.
            val endMonth = end.getOrNull(1) ?: 12
            val firstFull = if ((start.getOrNull(1) ?: 1) == 1) startYear else startYear + 1
            val lastFull = if (endMonth == 12) endYear else endYear - 1
            if (lastFull < firstFull) return Fallback.copy(metadata = metadata)
            val partialYear = endYear.takeIf { it > lastFull }
            val lastListed = partialYear ?: lastFull
            val firstListed = maxOf(firstFull, lastListed - MAX_YEARS_BACK)
            return DataCoverage(
                years = (firstListed..lastListed).map { it.toString() },
                latestYear = lastFull.toString(),
                metadata = metadata,
                partialYear = partialYear?.toString(),
                partialThroughMonth = endMonth.takeIf { partialYear != null }
            )
        }
    }
}

// Public for mcp-server, not needed from Swift
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
class ClimateTraceRepository internal constructor(
    private val store: KStore<List<Country>>,
    private val climateTraceApi: ClimateTraceApi,
    private val populationApi: PopulationApi,
    private val issPositionApi: IssPositionApi,
    private val reverseGeocodeApi: ReverseGeocodeApi,
) {
    suspend fun fetchCountries() : List<Country> {
        val countries: List<Country>? = store.get()
        if (countries.isNullOrEmpty()) return climateTraceApi.fetchCountries().also { store.set(it) }
        return countries
    }

    // Whether totals leave out the forestry-and-land-use sector, as climatetrace.org does by
    // default. App-wide so the list, the rankings and the detail screen always agree.
    private val _excludeForestry = MutableStateFlow(true)
    val excludeForestry: StateFlow<Boolean> = _excludeForestry.asStateFlow()

    fun setExcludeForestry(exclude: Boolean) {
        _excludeForestry.value = exclude
    }

    private var dataCoverage: DataCoverage? = null

    // Never throws: falls back to a fixed range of years (and tries again on the next call).
    suspend fun fetchDataCoverage(): DataCoverage {
        dataCoverage?.let { return it }
        return try {
            DataCoverage.from(climateTraceApi.fetchMetadata()).also { dataCoverage = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DataCoverage.Fallback
        }
    }

    // One country's figures for a year are a row of that year's global rankings, which is a
    // large response. Caching it per (year, forestry setting) means the trend line's request
    // per year is paid once, not once for every country opened.
    private val rankingsCache = RequestCache<Pair<String, Boolean>, CountryEmissionsRankingsResponse>(
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    suspend fun fetchRankings(year: String, excludeForestry: Boolean = false): CountryEmissionsRankingsResponse =
        rankingsCache.get(year to excludeForestry) { climateTraceApi.fetchRankings(year, excludeForestry) }

    suspend fun fetchCountryEmissionsInfo(countryCode: String, year: String) = fetchCountryEmissionsInfo(listOf(countryCode), year)
    suspend fun fetchCountryEmissionsInfo(countryCode: String, year: String, excludeForestry: Boolean) = fetchCountryEmissionsInfo(listOf(countryCode), year, excludeForestry)
    suspend fun fetchCountryEmissionsInfo(countryCodeList: List<String>, year: String, excludeForestry: Boolean = false) =
        fetchRankings(year, excludeForestry).rankings.filter { it.country in countryCodeList }

    suspend fun fetchCountryAssetEmissionsInfo(countryCode: String, year: String) = climateTraceApi.fetchCountryAssetEmissionsInfo(countryCode, year)
    suspend fun fetchCountryAssetEmissionsInfo(countryCodeList: List<String>, year: String) = climateTraceApi.fetchCountryAssetEmissionsInfo(countryCodeList, year)

    suspend fun fetchCountryEmissionsBreakdown(countryCode: String, year: String, excludeForestry: Boolean) = climateTraceApi.fetchCountryEmissionsResponse(countryCode, year, excludeForestry)

    suspend fun fetchAssetDetail(sourceId: Int) = climateTraceApi.fetchAssetDetail(sourceId)

    suspend fun fetchAssetMonthlyEmissions(sourceId: Int, year: Int) = climateTraceApi.fetchAssetMonthlyEmissions(sourceId, year)

    suspend fun fetchAssetsByCountry(countryCode: String, limit: Int = 20, excludeForestry: Boolean = false, year: String? = null) = climateTraceApi.fetchAssetsByCountry(countryCode, limit, excludeForestry, year)

    suspend fun getPopulation(countryCode: String) = populationApi.getPopulation(countryCode)

    // Emits the position every poll, or the failure if it couldn't be fetched, and keeps polling
    // either way: the collector decides whether a failure is worth telling the user about.
    fun pollISSPosition(): Flow<Result<IssPosition>> = flow {
        while (true) {
            val result = try {
                Result.success(issPositionApi.fetchISSPosition())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (currentCoroutineContext().isActive) {
                emit(result)
            }
            delay(ISS_POLL_INTERVAL_MS)
        }
    }

    // Returns the ISO alpha-3 country code (matching ClimateTrace's Country.id) for the given
    // coordinates, or null if they're over open ocean / no country match was found.
    suspend fun reverseGeocodeCountry(latitude: Double, longitude: Double): String? {
        val alpha2 = reverseGeocodeApi.reverseGeocode(latitude, longitude).countryCode
        return alpha2?.let { alpha2ToAlpha3(it) }
    }

    companion object {
        private const val ISS_POLL_INTERVAL_MS = 10_000L
    }
}
