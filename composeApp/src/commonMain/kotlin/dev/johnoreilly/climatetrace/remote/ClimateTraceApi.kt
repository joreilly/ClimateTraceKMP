package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the result of an API call to fetch assets.
 * @property assets A list of [Asset] objects.
 */
@Serializable
data class AssetsResult(val assets: List<Asset>)

/**
 * Represents a single asset.
 * @property id The unique identifier of the asset.
 * @property name The name of the asset.
 * @property assetType The type of the asset.
 * @property sector The sector to which the asset belongs.
 * @property thumbnail A URL to a thumbnail image for the asset.
 */
@Serializable
data class Asset(
    @SerialName("Id")
    val id: String,
    @SerialName("Name")
    val name: String,
    @SerialName("AssetType")
    val assetType: String,
    @SerialName("Sector")
    val sector: String,
    @SerialName("Thumbnail")
    val thumbnail: String,
)


@Serializable
data class Country(
    val alpha3: String,
    val alpha2: String,
    val name: String,
    val continent: String,
)

@Serializable
data class CountryEmissionsInfo(
    val country: String,
    val rank: Int,
    val emissions: EmissionInfo,
    val worldEmissions: EmissionInfo
)

@Serializable
data class CountryAssetEmissionsInfo(
    @SerialName("Country")
    val country: String? = null,
    @SerialName("Emissions")
    val emissions: Float = 0f,
    @SerialName("Sector")
    val sector: String? = null
)


@Serializable
data class EmissionInfo(
    val co2: Float,
    val co2e_100yr: Float,
    val co2e_20yr: Float
)



class ClimateTraceApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.climatetrace.org/v6",
)  {
    suspend fun fetchContinents() = client.get("$baseUrl/definitions/continents").body<List<String>>()
    suspend fun fetchCountries() = client.get("$baseUrl/definitions/countries").body<List<Country>>()
    suspend fun fetchSectors() = client.get("$baseUrl/definitions/sectors").body<List<String>>()
    suspend fun fetchSubSectors() = client.get("$baseUrl/definitions/subsectors").body<List<String>>()
    suspend fun fetchGases() = client.get("$baseUrl/definitions/gases").body<List<String>>()

    // TODO need to implement paging on top of this
    suspend fun fetchAssets() = client.get("$baseUrl/assets").body<AssetsResult>()


    suspend fun fetchCountryEmissionsInfo(countryCodeList: List<String>, year: String): List<CountryEmissionsInfo> {
        return client.get("$baseUrl/country/emissions") {
            url {
                parameters.append("countries", countryCodeList.joinToString(","))
                parameters.append("since", year)
                parameters.append("to", year)
            }
        }.body<List<CountryEmissionsInfo>>()
    }

    suspend fun fetchCountryAssetEmissionsInfo(countryCodeList: List<String>): Map<String, List<CountryAssetEmissionsInfo>> {
        return client.get("$baseUrl/assets/emissions") {
            url {
                parameters.append("countries", countryCodeList.joinToString(","))
            }
        }.body<Map<String, List<CountryAssetEmissionsInfo>>>()
    }

    suspend fun fetchCountryAssetEmissionsInfo(countryCode: String) = client.get("$baseUrl/assets/emissions?countries=$countryCode").body<Map<String, List<CountryAssetEmissionsInfo>>>()[countryCode] ?: emptyList()
}