package dev.johnoreilly.climatetrace.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val id: String,
    val name: String,
    val continent: String,
)

@Serializable
data class Asset(
    val id: Int,
    val name: String,
    val country: String? = null,
    val assetType: String? = null,
    val sector: String? = null,
    val subsector: String? = null,
    val sourceType: String? = null,
    val owners: List<AssetOwner>? = null,
    val emissionsQuantity: Double? = null,
    val gas: String? = null,
    val year: Int? = null,
    val centroid: Centroid? = null,
)

@Serializable
data class AssetOwner(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class EmissionMeasurement(
    val gas: String? = null,
    val activityUnits: String? = null,
    val activity: Double? = null,
    val emissionsFactorUnits: String? = null,
    val capacityUnits: String? = null,
    val capacity: Double? = null,
    val capacityFactor: Double? = null,
    val emissionsFactor: Double? = null,
    val emissionsQuantity: Double? = null,
    val year: Int? = null,
    val month: Int? = null,
    val sector: String? = null,
    val subsector: String? = null,
)

@Serializable
data class AssetSubsectorRank(
    val year: Int,
    val rank: Int,
)

@Serializable
data class AssetDetail(
    val id: Int,
    val name: String,
    val country: String? = null,
    val sector: String? = null,
    val assetType: String? = null,
    val sourceType: String? = null,
    val owners: List<AssetOwner>? = null,
    val emissions: List<EmissionMeasurement>? = null,
    val subsectorRanks: List<AssetSubsectorRank>? = null,
    val centroid: Centroid? = null,
)

@Serializable
data class Centroid(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val srid: Int? = null,
)

@Serializable
data class CountryEmissionsInfo(
    val country: String,
    val rank: Int,
    val emissionsQuantity: Double = 0.0,
    val emissionsPerCapita: Double = 0.0,
    val percentage: Double = 0.0,
    val gas: String = "",
)

@Serializable
data class CountryEmissionsRankingsResponse(
    val rankings: List<CountryEmissionsInfo> = emptyList(),
)

@Serializable
data class CountryAssetEmissionsInfo(
    val sector: String? = null,
    val subsector: String? = null,
    val gas: String? = null,
    val emissionsQuantity: Double = 0.0,
    val percentage: Double = 0.0,
)

@Serializable
data class AggregatedEmissionsOverview(
    val summaries: List<CountryAssetEmissionsInfo> = emptyList(),
)

@Serializable
data class AggregatedEmissionsResponse(
    val sectors: AggregatedEmissionsOverview? = null,
    val subsectors: AggregatedEmissionsOverview? = null,
)

class ClimateTraceApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.climatetrace.org/v7",
) {
    suspend fun fetchContinents() = client.get("$baseUrl/definitions/continents").body<List<String>>()
    suspend fun fetchCountries() = client.get("$baseUrl/definitions/countries").body<List<Country>>()
    suspend fun fetchSectors() = client.get("$baseUrl/definitions/sectors").body<List<String>>()
    suspend fun fetchSubSectors() = client.get("$baseUrl/definitions/subsectors").body<List<String>>()
    suspend fun fetchGases() = client.get("$baseUrl/definitions/gases").body<List<String>>()

    suspend fun fetchAssets() = client.get("$baseUrl/sources").body<List<Asset>>()

    suspend fun fetchCountryEmissionsInfo(countryCodeList: List<String>, year: String): List<CountryEmissionsInfo> {
        val response = client.get("$baseUrl/rankings/countries") {
            url {
                parameters.append("start", "$year-01-01")
                parameters.append("end", "$year-12-31")
            }
        }.body<CountryEmissionsRankingsResponse>()
        return response.rankings.filter { it.country in countryCodeList }
    }

    suspend fun fetchCountryAssetEmissionsInfo(countryCode: String, year: String): List<CountryAssetEmissionsInfo> {
        val response = fetchCountryEmissionsResponse(countryCode, year)
        val sectorSummaries = response.sectors?.summaries ?: emptyList()
        val subsectorSummaries = response.subsectors?.summaries ?: emptyList()
        return sectorSummaries + subsectorSummaries
    }

    suspend fun fetchCountryAssetEmissionsInfo(countryCodeList: List<String>, year: String): Map<String, List<CountryAssetEmissionsInfo>> {
        return countryCodeList.associateWith { fetchCountryAssetEmissionsInfo(it, year) }
    }

    suspend fun fetchCountryEmissionsResponse(countryCode: String, year: String): AggregatedEmissionsResponse {
        return client.get("$baseUrl/sources/emissions") {
            url {
                parameters.append("gadmId", countryCode)
                parameters.append("year", year)
            }
        }.body<AggregatedEmissionsResponse>()
    }

    suspend fun fetchAssetDetail(sourceId: Int): AssetDetail = client.get("$baseUrl/sources/$sourceId").body<AssetDetail>()

    suspend fun fetchAssetsByCountry(countryCode: String, limit: Int = 20): List<Asset> {
        return client.get("$baseUrl/sources") {
            url {
                parameters.append("gadmId", countryCode)
                parameters.append("limit", limit.toString())
            }
        }.body<List<Asset>>()
    }
}
