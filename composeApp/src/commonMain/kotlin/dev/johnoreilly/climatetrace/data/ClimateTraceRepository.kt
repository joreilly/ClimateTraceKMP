package dev.johnoreilly.climatetrace.data

import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.PopulationApi
import io.github.xxfast.kstore.KStore


class ClimateTraceRepository(
    private val store: KStore<List<Country>>,
    private val climateTraceApi: ClimateTraceApi,
    private val populationApi: PopulationApi
) {
    suspend fun fetchCountries() : List<Country> {
        val countries: List<Country>? = store.get()
        if (countries.isNullOrEmpty()) return climateTraceApi.fetchCountries().also { store.set(it) }
        return countries
    }

    suspend fun fetchCountryEmissionsInfo(countryCode: String, year: String) = climateTraceApi.fetchCountryEmissionsInfo(listOf(countryCode), year)
    suspend fun fetchCountryEmissionsInfo(countryCodeList: List<String>, year: String) = climateTraceApi.fetchCountryEmissionsInfo(countryCodeList, year)

    suspend fun fetchCountryAssetEmissionsInfo(countryCode: String) = climateTraceApi.fetchCountryAssetEmissionsInfo(countryCode)
    suspend fun fetchCountryAssetEmissionsInfo(countryCodeList: List<String>) = climateTraceApi.fetchCountryAssetEmissionsInfo(countryCodeList)

    suspend fun fetchAssetDetail(sourceId: Int) = climateTraceApi.fetchAssetDetail(sourceId)

    suspend fun fetchAssetsByCountry(countryCode: String, limit: Int = 20) = climateTraceApi.fetchAssetsByCountry(countryCode, limit)

    suspend fun getPopulation(countryCode: String) = populationApi.getPopulation(countryCode)
}
