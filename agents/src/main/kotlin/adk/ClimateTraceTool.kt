package adk

import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import io.reactivex.rxjava3.core.Single
import koin
import kotlinx.coroutines.rx3.rxSingle

class ClimateTraceTool {

    companion object {
        val climateTraceRepository = koin.get<ClimateTraceRepository>()

        @JvmStatic
        fun getCountries(): Single<Map<String, String>> {
            return rxSingle {
                mapOf("countries" to climateTraceRepository.fetchCountries().toString())
            }
        }

        @JvmStatic
        fun getEmissions(countryCode: String, year: String): Single<Map<String, String>> {
            return rxSingle {
                mapOf("emissions" to climateTraceRepository.fetchCountryEmissionsInfo(countryCode, year).toString())
            }
        }
    }
}