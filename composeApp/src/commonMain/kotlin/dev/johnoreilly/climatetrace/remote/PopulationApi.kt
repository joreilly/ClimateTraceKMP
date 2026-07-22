package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class PopulationApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.worldbank.org/v2/country/"
) {
    /**
     * Fetches total population for a country from the World Bank API (keyless).
     *
     * The endpoint returns a two-element array of the form
     * `[ { pagination metadata }, [ { observation }, ... ] ]`, so the payload
     * can't be modelled as a single homogeneous list — we read the second
     * element (the observations array) and take the most recent value.
     */
    suspend fun getPopulation(countryCode: String): Long {
        val response = client.get(
            "$baseUrl$countryCode/indicator/SP.POP.TOTL?format=json&mrv=1"
        )
        val payload = response.body<JsonArray>()
        val observations = payload.getOrNull(1) as? JsonArray ?: return 0L
        val latest = observations.firstOrNull()?.jsonObject ?: return 0L
        return latest["value"]?.jsonPrimitive?.longOrNull ?: 0L
    }
}
