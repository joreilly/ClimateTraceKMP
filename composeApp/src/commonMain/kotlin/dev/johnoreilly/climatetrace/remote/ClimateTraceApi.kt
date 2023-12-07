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

/*
{
"Id": 25452242,
"Name": "Taichung power station",
"NativeId": "TRRACTMTIE",
"Country": "TWN",
"Sector": "electricity-generation",
"AssetType": "coal, oil",
 */

@Serializable
data class AssetsResult(val assets: List<Asset>)


@Serializable
data class AstroResult(val message: String, val number: Int, val people: List<Assignment>)

@Serializable
data class Assignment(val craft: String, val name: String, var personImageUrl: String? = "", var personBio: String? = "")

@Serializable
data class IssPosition(val latitude: Double, val longitude: Double)

@Serializable
data class IssResponse(val message: String, val iss_position: IssPosition, val timestamp: Long)

class ClimateTraceApi(
    var baseUrl: String = "https://people-in-space-proxy.ew.r.appspot.com",
)  {

    val baseUrl2 = "https://api.climatetrace.org/v4/assets"

    val client = HttpClient() {
        install(ContentNegotiation) {
            json(Json { isLenient = true; ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchAssets() = client.get("$baseUrl2").body<AssetsResult>()

    suspend fun fetchPeople() = client.get("$baseUrl/astros.json").body<AstroResult>()
    suspend fun fetchISSPosition() = client.get("$baseUrl/iss-now.json").body<IssResponse>()
}