package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class CountryResponse(
    val population: Long
)

class PopulationApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://restcountries.com/v3.1/alpha/"

) {
    suspend fun getPopulation(countryCode: String): Long {
        val response = client.get("$baseUrl$countryCode")
        val countries = response.body<List<CountryResponse>>()
        return countries.firstOrNull()?.population ?: 0L
    }
}