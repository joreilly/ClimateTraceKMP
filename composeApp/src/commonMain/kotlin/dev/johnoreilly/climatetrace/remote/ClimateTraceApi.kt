package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Asset(
    @SerialName("Id")
    val id: String,
    @SerialName("Name")
    val name: String,
    @SerialName("AssetType")
    val assetType: String,
    @SerialName("Thumbnail")
    val thumbnail: String,
)

@Serializable
data class AssetsResult(val assets: List<Asset>)


class ClimateTraceApi(
    var baseUrl: String = "https://api.climatetrace.org/v4/assets",
)  {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { isLenient = true; ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchAssets() = client.get("$baseUrl").body<AssetsResult>()
}