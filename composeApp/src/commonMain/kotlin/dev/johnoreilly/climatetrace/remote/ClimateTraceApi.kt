package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetsResult(val assets: List<Asset>)

@Serializable
data class Asset(
    @SerialName("Id")
    val id: Int,
    @SerialName("Name")
    val name: String,
    @SerialName("NativeId")
    val nativeId: String? = null,
    @SerialName("Country")
    val country: String? = null,
    @SerialName("AssetType")
    val assetType: String? = null,
    @SerialName("Sector")
    val sector: String? = null,
    @SerialName("ReportingEntity")
    val reportingEntity: String? = null,
    @SerialName("Thumbnail")
    val thumbnail: String? = null,
    @SerialName("Owners")
    val owners: List<AssetOwner>? = null,
    @SerialName("EmissionsSummary")
    val emissionsSummary: List<EmissionMeasurement>? = null,
)

@Serializable
data class AssetOwner(
    @SerialName("CompanyId")
    val companyId: String? = null,
    @SerialName("CompanyName")
    val companyName: String? = null
)

@Serializable
data class EmissionMeasurement(
    @SerialName("Gas")
    val gas: String? = null,
    @SerialName("ActivityUnits")
    val activityUnits: String? = null,
    @SerialName("Activity")
    val activity: Double? = null,
    @SerialName("EmissionsFactorUnits")
    val emissionsFactorUnits: String? = null,
    @SerialName("CapacityUnits")
    val capacityUnits: String? = null,
    @SerialName("Capacity")
    val capacity: Double? = null,
    @SerialName("CapacityFactor")
    val capacityFactor: Double? = null,
    @SerialName("EmissionsFactor")
    val emissionsFactor: Double? = null,
    @SerialName("EmissionsQuantity")
    val emissionsQuantity: Double? = null,
    @SerialName("Year")
    val year: Int? = null
)

@Serializable
data class AssetDetail(
    @SerialName("Id")
    val id: Int,
    @SerialName("Name")
    val name: String,
    @SerialName("NativeId")
    val nativeId: String? = null,
    @SerialName("Country")
    val country: String? = null,
    @SerialName("Sector")
    val sector: String? = null,
    @SerialName("AssetType")
    val assetType: String? = null,
    @SerialName("ReportingEntity")
    val reportingEntity: String? = null,
    @SerialName("Thumbnail")
    val thumbnail: String? = null,
    @SerialName("Owners")
    val owners: List<AssetOwner>? = null,
    @SerialName("EmissionsDetails")
    val emissionsDetails: List<EmissionMeasurement>? = null,
    @SerialName("EmissionsSummary")
    val emissionsSummary: List<EmissionMeasurement>? = null,
    @SerialName("SectorRanks")
    val sectorRanks: Map<String, Int>? = null,
    @SerialName("Centroid")
    val centroid: Centroid? = null
)

@Serializable
data class Centroid(
    @SerialName("Geometry")
    val geometry: List<Double>? = null,
    @SerialName("SRID")
    val srid: Int? = null
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

    suspend fun fetchAssetDetail(sourceId: Int): AssetDetail = client.get("$baseUrl/assets/$sourceId").body<AssetDetail>()

    suspend fun fetchAssetsByCountry(countryCode: String, limit: Int = 20): AssetsResult {
        return client.get("$baseUrl/assets") {
            url {
                parameters.append("countries", countryCode)
                parameters.append("limit", limit.toString())
            }
        }.body<AssetsResult>()
    }
}