package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.johnoreilly.climatetrace.remote.AssetDetail
import dev.johnoreilly.climatetrace.remote.AssetOwner
import dev.johnoreilly.climatetrace.remote.AssetSubsectorRank
import dev.johnoreilly.climatetrace.remote.EmissionMeasurement
import dev.johnoreilly.climatetrace.ui.utils.formatEmissionsQuantity
import dev.johnoreilly.climatetrace.viewmodel.AssetDetailUIState
import dev.johnoreilly.climatetrace.viewmodel.AssetDetailViewModel
import io.github.koalaplot.core.util.toString
import org.koin.compose.koinInject

internal data class AssetDetailScreen(val sourceId: Int, val assetName: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinInject<AssetDetailViewModel>()
        val viewState by viewModel.viewState.collectAsState()

        LaunchedEffect(sourceId) {
            viewModel.loadAsset(sourceId)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = assetName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                when (val state = viewState) {
                    is AssetDetailUIState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading source details…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is AssetDetailUIState.Error -> {
                        ErrorState(
                            title = "Unable to Load Source",
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                    is AssetDetailUIState.Success -> {
                        AssetDetailContent(state.assetDetail, state.monthlyEmissions, state.monthlyYear)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AssetDetailContent(
    assetDetail: AssetDetail,
    monthlyEmissions: List<EmissionMeasurement> = emptyList(),
    monthlyYear: Int? = null
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Asset Header
        AssetHeader(assetDetail)

        Spacer(modifier = Modifier.size(16.dp))

        // Key Info Chips
        AssetInfoChips(assetDetail)

        Spacer(modifier = Modifier.size(16.dp))

        // Location
        val latitude = assetDetail.centroid?.latitude
        val longitude = assetDetail.centroid?.longitude
        if (latitude != null && longitude != null) {
            LocationSection(assetDetail.id, assetDetail.name, latitude, longitude)
            Spacer(modifier = Modifier.size(16.dp))
        }

        // Monthly emissions
        val monthlyValues = monthlyEmissions.mapNotNull { measurement ->
            val month = measurement.month
            val quantity = measurement.emissionsQuantity
            if (month != null && quantity != null) month to quantity else null
        }
        if (monthlyValues.size >= 2 && monthlyYear != null) {
            MonthlyEmissionsSection(monthlyYear.toString(), monthlyValues)
            Spacer(modifier = Modifier.size(16.dp))
        }

        // Ownership Section
        if (!assetDetail.owners.isNullOrEmpty()) {
            OwnershipSection(assetDetail.owners)
            Spacer(modifier = Modifier.size(16.dp))
        }

        // Emissions Summary Section
        if (!assetDetail.emissions.isNullOrEmpty()) {
            EmissionsSummarySection(assetDetail.emissions)
            Spacer(modifier = Modifier.size(16.dp))
        }

        // Sector Ranks Section
        if (!assetDetail.subsectorRanks.isNullOrEmpty()) {
            SectorRanksSection(assetDetail.subsectorRanks)
        }
    }
}

@Composable
private fun AssetHeader(assetDetail: AssetDetail) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = assetDetail.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(4.dp))
            assetDetail.sector?.let {
                Text(
                    text = it.replace("-", " ").replaceFirstChar { c -> c.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            assetDetail.country?.let {
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = "Country: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocationSection(id: Int, name: String, latitude: Double, longitude: Double) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))
            SourceMapView(
                points = listOf(MapPoint(id, latitude, longitude, name, weight = 1.0)),
                maxZoom = 8,
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "${latitude.toString(4)}, ${longitude.toString(4)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AssetInfoChips(assetDetail: AssetDetail) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        assetDetail.assetType?.takeIf { it.isNotBlank() }?.let {
            InfoTag(label = "Type", value = it, modifier = Modifier.weight(1f, fill = false))
        }
        assetDetail.sourceType?.takeIf { it.isNotBlank() }?.let {
            InfoTag(label = "Reporting Entity", value = it, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

// Read-only label/value tag. Deliberately not a chip: these have nothing to do when tapped.
@Composable
private fun InfoTag(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OwnershipSection(owners: List<AssetOwner>) {
    // Filter out owners with null/blank names
    val validOwners = owners.filter { !it.name.isNullOrBlank() }
    if (validOwners.isEmpty()) return

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Ownership",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))

            // Get unique owners by id or name
            val uniqueOwners = validOwners.distinctBy { it.id ?: it.name }

            uniqueOwners.forEachIndexed { index, owner ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = owner.name ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        owner.id?.let { id ->
                            Text(
                                text = "ID: $id",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (index < uniqueOwners.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun EmissionsSummarySection(emissions: List<EmissionMeasurement>) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Emissions Summary",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))

            emissions.filter { it.gas == "co2e_100yr" || it.gas == "co2" }.forEach { emission ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = emission.gas?.uppercase() ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    emission.emissionsQuantity?.let { quantity ->
                        val formattedQuantity = formatEmissionsQuantity(quantity)
                        Text(
                            text = formattedQuantity,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorRanksSection(subsectorRanks: List<AssetSubsectorRank>) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Sector Rankings by Year",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))

            subsectorRanks.sortedByDescending { it.year }.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = entry.year.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "#${entry.rank}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
