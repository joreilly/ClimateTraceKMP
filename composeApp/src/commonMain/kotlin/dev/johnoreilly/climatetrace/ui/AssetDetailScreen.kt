package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
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
import dev.johnoreilly.climatetrace.remote.EmissionMeasurement
import dev.johnoreilly.climatetrace.viewmodel.AssetDetailUIState
import dev.johnoreilly.climatetrace.viewmodel.AssetDetailViewModel
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private fun formatEmissionsQuantity(quantity: Double): String {
    return when {
        quantity >= 1_000_000_000 -> "${(quantity / 1_000_000_000 * 100).roundToInt() / 100.0} Gt"
        quantity >= 1_000_000 -> "${(quantity / 1_000_000 * 100).roundToInt() / 100.0} Mt"
        quantity >= 1_000 -> "${(quantity / 1_000 * 100).roundToInt() / 100.0} kt"
        else -> "${(quantity * 100).roundToInt() / 100.0} t"
    }
}

data class AssetDetailScreen(val sourceId: Int, val assetName: String) : Screen {
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
                            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is AssetDetailUIState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is AssetDetailUIState.Success -> {
                        AssetDetailContent(state.assetDetail)
                    }
                }
            }
        }
    }
}

@Composable
fun AssetDetailContent(assetDetail: AssetDetail) {
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

        // Ownership Section
        if (!assetDetail.owners.isNullOrEmpty()) {
            OwnershipSection(assetDetail.owners)
            Spacer(modifier = Modifier.size(16.dp))
        }

        // Emissions Summary Section
        if (!assetDetail.emissionsSummary.isNullOrEmpty()) {
            EmissionsSummarySection(assetDetail.emissionsSummary)
            Spacer(modifier = Modifier.size(16.dp))
        }

        // Sector Ranks Section
        if (!assetDetail.sectorRanks.isNullOrEmpty()) {
            SectorRanksSection(assetDetail.sectorRanks)
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
private fun AssetInfoChips(assetDetail: AssetDetail) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        assetDetail.assetType?.takeIf { it.isNotBlank() }?.let {
            AssistChip(
                onClick = {},
                label = {
                    Column {
                        Text(
                            text = "Type",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }
        assetDetail.reportingEntity?.let {
            AssistChip(
                onClick = {},
                label = {
                    Column {
                        Text(
                            text = "Reporting Entity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }
    }
}

@Composable
private fun OwnershipSection(owners: List<AssetOwner>) {
    // Filter out owners with null/blank names
    val validOwners = owners.filter { !it.companyName.isNullOrBlank() }
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

            // Get unique owners by companyId or companyName
            val uniqueOwners = validOwners.distinctBy { it.companyId ?: it.companyName }

            uniqueOwners.forEachIndexed { index, owner ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = owner.companyName ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        owner.companyId?.let { id ->
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
private fun SectorRanksSection(sectorRanks: Map<String, Int>) {
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

            sectorRanks.entries.sortedByDescending { it.key }.forEach { (year, rank) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = year,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
