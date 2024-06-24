package dev.johnoreilly.climatetrace.data

import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.Country
import io.github.xxfast.kstore.KStore


class ClimateTraceRepository(
    private val store: KStore<List<Country>>,
    private val api: ClimateTraceApi
) {
    suspend fun fetchCountries() : List<Country> {
        val countries: List<Country>? = store.get()
        if (countries.isNullOrEmpty()) return api.fetchCountries().also { store.set(it) }
        return countries
    }

    suspend fun fetchCountryEmissionsInfo(countryCode: String, year: String) = api.fetchCountryEmissionsInfo(countryCode, year)
    suspend fun fetchCountryAssetEmissionsInfo(countryCode: String) = api.fetchCountryAssetEmissionsInfo(countryCode)
}
