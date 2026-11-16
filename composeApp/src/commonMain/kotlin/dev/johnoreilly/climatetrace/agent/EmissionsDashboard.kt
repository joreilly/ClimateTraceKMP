@file:OptIn(ExperimentalObjCRefinement::class)

package dev.johnoreilly.climatetrace.agent

import dev.johnoreilly.climatetrace.remote.AggregatedEmissionsResponse
import kotlinx.serialization.Serializable
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

// Figures for the MCP server's emissions dashboard (an MCP App). The view draws these as
// charts; the model gets the same figures as text alongside, in [DashboardReport.text].
// Public for mcp-server, not needed from Swift.

@HiddenFromObjC
@Serializable
data class EmissionsDashboard(
    val year: String,
    val includeForestry: Boolean,
    /** Years the view can switch to. */
    val years: List<String>,
    /** Set when [year] is still in progress: the last month it has data for. */
    val partialThroughMonth: Int? = null,
    val countries: List<CountryDashboard>,
)

@HiddenFromObjC
@Serializable
data class CountryDashboard(
    val code: String,
    val name: String? = null,
    val totalTonnes: Double? = null,
    /** Largest first. */
    val sectors: List<SectorTotal> = emptyList(),
    /** In month order. */
    val monthly: List<MonthTotal> = emptyList(),
    /** Why there are no figures, when there are none. */
    val error: String? = null,
)

@HiddenFromObjC
@Serializable
data class SectorTotal(val sector: String, val tonnes: Double, val percentage: Double)

@HiddenFromObjC
@Serializable
data class MonthTotal(val month: Int, val tonnes: Double)

@HiddenFromObjC
data class DashboardReport(val dashboard: EmissionsDashboard, val text: String)

internal fun countryDashboard(code: String, name: String?, breakdown: AggregatedEmissionsResponse): CountryDashboard {
    // The sectors overview can also carry subsector rows; only whole sectors make up the total.
    val sectors = breakdown.sectors?.summaries.orEmpty()
        .filter { it.subsector == null && it.sector != null }
        .sortedByDescending { it.emissionsQuantity }
        .map { SectorTotal(it.sector!!, it.emissionsQuantity, it.percentage) }
    return CountryDashboard(
        code = code,
        name = name,
        totalTonnes = breakdown.totals?.summaries?.firstOrNull()?.emissionsQuantity
            ?: sectors.takeIf { it.isNotEmpty() }?.sumOf { it.tonnes },
        sectors = sectors,
        monthly = breakdown.totals?.timeseries.orEmpty()
            .sortedBy { it.month }
            .map { MonthTotal(it.month, it.emissionsQuantity) },
    )
}
