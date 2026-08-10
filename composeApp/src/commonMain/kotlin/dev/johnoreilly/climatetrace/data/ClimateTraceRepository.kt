package dev.johnoreilly.climatetrace.data

import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.IssPosition
import dev.johnoreilly.climatetrace.remote.IssPositionApi
import dev.johnoreilly.climatetrace.remote.PopulationApi
import dev.johnoreilly.climatetrace.remote.ReverseGeocodeApi
import dev.johnoreilly.climatetrace.ui.utils.alpha2ToAlpha3
import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive


class ClimateTraceRepository(
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

    suspend fun fetchCountryEmissionsInfo(countryCode: String, year: String) = climateTraceApi.fetchCountryEmissionsInfo(listOf(countryCode), year)
    suspend fun fetchCountryEmissionsInfo(countryCodeList: List<String>, year: String) = climateTraceApi.fetchCountryEmissionsInfo(countryCodeList, year)

    suspend fun fetchCountryAssetEmissionsInfo(countryCode: String, year: String) = climateTraceApi.fetchCountryAssetEmissionsInfo(countryCode, year)
    suspend fun fetchCountryAssetEmissionsInfo(countryCodeList: List<String>, year: String) = climateTraceApi.fetchCountryAssetEmissionsInfo(countryCodeList, year)

    suspend fun fetchAssetDetail(sourceId: Int) = climateTraceApi.fetchAssetDetail(sourceId)

    suspend fun fetchAssetsByCountry(countryCode: String, limit: Int = 20) = climateTraceApi.fetchAssetsByCountry(countryCode, 20)

    suspend fun fetchRankings(year: String) = climateTraceApi.fetchRankings(year)

    suspend fun getPopulation(countryCode: String) = populationApi.getPopulation(countryCode)

    fun pollISSPosition(): Flow<IssPosition> = flow {
        while (true) {
            try {
                val position = issPositionApi.fetchISSPosition().iss_position
                if (currentCoroutineContext().isActive) {
                    emit(position)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // ignore transient failures, next poll will retry
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
