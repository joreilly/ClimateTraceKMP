package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class IssPosition(val latitude: Double, val longitude: Double, val timestamp: Long = -1)

@Serializable
data class IssResponse(val message: String, val iss_position: IssPosition, val timestamp: Long)

class IssPositionApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://people-in-space-proxy.ew.r.appspot.com",
) {
    suspend fun fetchISSPosition() = client.get("$baseUrl/iss-now.json").body<IssResponse>()
}
