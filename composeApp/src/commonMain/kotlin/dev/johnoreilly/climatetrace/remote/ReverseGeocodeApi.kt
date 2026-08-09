package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class ReverseGeocodeResult(
    val countryCode: String? = null,
    val countryName: String? = null,
)

// BigDataCloud's client-side reverse geocode endpoint: free, no API key, CORS-enabled.
// countryCode/countryName come back blank when the coordinates are over open ocean.
class ReverseGeocodeApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.bigdatacloud.net/data/reverse-geocode-client",
) {
    suspend fun reverseGeocode(latitude: Double, longitude: Double): ReverseGeocodeResult {
        return client.get(baseUrl) {
            url {
                parameters.append("latitude", latitude.toString())
                parameters.append("longitude", longitude.toString())
                parameters.append("localityLanguage", "en")
            }
        }.body<ReverseGeocodeResult>()
    }
}
