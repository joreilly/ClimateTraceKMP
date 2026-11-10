package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class IssPosition(val latitude: Double, val longitude: Double, val timestamp: Long = -1)

// The position has been served in two shapes: flat ({"latitude": 46.6, "longitude": 112.5}),
// which is what the proxy returns today, and Open Notify's original
// ({"iss_position": {"latitude": "46.6", "longitude": "112.5"}, "timestamp": ...}) with the
// coordinates as strings. Accept either, so a change at the proxy doesn't silently stop the tracker.
internal fun parseIssPosition(json: JsonObject): IssPosition {
    val position = json["iss_position"]?.jsonObject ?: json
    return IssPosition(
        latitude = position.getValue("latitude").jsonPrimitive.double,
        longitude = position.getValue("longitude").jsonPrimitive.double,
        timestamp = (json["timestamp"] ?: position["timestamp"])?.jsonPrimitive?.longOrNull ?: -1
    )
}

internal class IssPositionApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://people-in-space-proxy.ew.r.appspot.com",
) {
    suspend fun fetchISSPosition(): IssPosition = parseIssPosition(client.get("$baseUrl/iss-now.json").body<JsonObject>())
}
