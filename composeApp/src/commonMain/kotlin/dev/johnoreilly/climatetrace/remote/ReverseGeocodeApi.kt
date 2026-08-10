package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ReverseGeocodeResult(
    val countryCode: String? = null,
    val countryName: String? = null,
)

@Serializable
private data class NominatimAddress(
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
)

@Serializable
private data class NominatimReverseResponse(
    val address: NominatimAddress? = null,
)

// OpenStreetMap's Nominatim reverse geocoder: free, no API key, and (unlike BigDataCloud's
// "client-side only" endpoint we used originally) fine with being called from a server-side /
// multiplatform HTTP client as long as requests stay modest and carry an identifying
// User-Agent - see https://operations.osmfoundation.org/policies/nominatim/. The shared
// HttpClient sends that header by default (see Koin.kt).
// zoom=3 caps detail at country level; coordinates over open ocean return no address.
class ReverseGeocodeApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://nominatim.openstreetmap.org/reverse",
) {
    suspend fun reverseGeocode(latitude: Double, longitude: Double): ReverseGeocodeResult {
        val response = client.get(baseUrl) {
            url {
                parameters.append("format", "jsonv2")
                parameters.append("lat", latitude.toString())
                parameters.append("lon", longitude.toString())
                parameters.append("zoom", "3")
            }
        }.body<NominatimReverseResponse>()
        return ReverseGeocodeResult(
            countryCode = response.address?.countryCode,
            countryName = response.address?.country
        )
    }
}
