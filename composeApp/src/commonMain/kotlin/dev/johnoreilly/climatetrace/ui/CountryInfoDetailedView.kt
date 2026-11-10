package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import io.github.koalaplot.core.util.toString
import kotlin.math.roundToInt
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.carlsen.flagkit.FlagKit
import dev.johnoreilly.climatetrace.remote.Asset
import dev.johnoreilly.climatetrace.remote.DataMetadata
import dev.johnoreilly.climatetrace.ui.utils.alpha3ToAlpha2
import dev.johnoreilly.climatetrace.ui.utils.formatEmissionsQuantity
import dev.johnoreilly.climatetrace.ui.utils.sectorIcon
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsUIState
@Composable
internal fun CountryInfoDetailedView(
    viewState: CountryDetailsUIState,
    perCapitaRank: Int? = null,
    onRetry: (() -> Unit)? = null,
    onIncludeForestryChange: ((Boolean) -> Unit)? = null,
    onYearSelected: (String) -> Unit
) {
    when (viewState) {
        CountryDetailsUIState.NoCountrySelected -> {
            EmptyState(
                title = "Select a country",
                message = "Choose a country from the list to see its emissions, sectors and largest sources.",
                icon = Icons.Default.Public
            )
        }
        is CountryDetailsUIState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading emissions data…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is CountryDetailsUIState.Error -> {
            ErrorState(
                title = "Unable to Load Details",
                message = viewState.message,
                onRetry = onRetry
            )
        }
        is CountryDetailsUIState.Success -> {
            CountryInfoDetailedViewSuccess(viewState, perCapitaRank, onYearSelected, onIncludeForestryChange)
        }
    }
}


@Composable
internal fun CountryInfoDetailedViewSuccess(
    viewState: CountryDetailsUIState.Success,
    perCapitaRank: Int?,
    onYearSelected: (String) -> Unit,
    onIncludeForestryChange: ((Boolean) -> Unit)? = null
) {
    var selectedSector by remember(viewState.country.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card with flag + country label info
        CountryHeader(viewState)

        val year = viewState.year
        val countryAssetEmissionsList = viewState.countryAssetEmissionsList
        val countryEmissionInfo = viewState.countryEmissionInfo

        countryEmissionInfo?.let {
            val co2Formatted = formatEmissionsQuantity(countryEmissionInfo.emissionsQuantity)
            val percentage = "${countryEmissionInfo.percentage.toString(2)}%"
            val perCapita = "${countryEmissionInfo.emissionsPerCapita.toString(1)} t"

            // Emissions Summary Section - combines year selector and key figures
            EmissionsSummarySection(
                year = year,
                availableYears = viewState.availableYears,
                onYearSelected = onYearSelected,
                co2Value = co2Formatted,
                co2PercentChange = countryEmissionInfo.emissionsPercentChange,
                rank = countryEmissionInfo.rank,
                share = percentage,
                perCapita = perCapita,
                perCapitaRank = perCapitaRank,
                yearlyEmissions = viewState.yearlyEmissions,
                isRefreshing = viewState.isRefreshing,
                includeForestry = !viewState.excludeForestry,
                partialThroughMonth = viewState.partialThroughMonth,
                onIncludeForestryChange = onIncludeForestryChange
            )

            if (viewState.monthlyEmissions.size >= 2) {
                MonthlyEmissionsSection(year, viewState.monthlyEmissions.map { it.month to it.emissionsQuantity })
            }

            // Emissions by Sector Section
            val filteredCountryAssetEmissionsList = countryAssetEmissionsList.filter { it.sector != null }
            if (filteredCountryAssetEmissionsList.isNotEmpty()) {
                EmissionsBySectorSection(
                    countryAssetEmissionsList = countryAssetEmissionsList,
                    selectedSector = selectedSector,
                    onSectorChange = { selectedSector = it }
                )
            } else {
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No sector data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Assets Section
        if (viewState.assets.isNotEmpty()) {
            AssetsSection(
                assets = viewState.assets,
                selectedSector = selectedSector,
                onClearSector = { selectedSector = null }
            )
        }

        viewState.dataMetadata?.let { DataReleaseFooter(it) }
    }
}

@Composable
internal fun MonthlyEmissionsSection(year: String, values: List<Pair<Int, Double>>) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Emissions · $year",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))
            MonthlyEmissionsChart(values = values, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DataReleaseFooter(metadata: DataMetadata) {
    val parts = listOfNotNull(
        metadata.dataVersion?.let { "Climate TRACE data $it" },
        metadata.releaseDate?.let { "released $it" },
        metadata.temporalCoverage?.let { coverage ->
            if (coverage.start != null && coverage.end != null) "covers ${coverage.start} to ${coverage.end}" else null
        }
    )
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

// Switch for whether totals include the forestry-and-land-use sector. It's an app-wide setting:
// flipping it here also re-ranks the country list and the rankings tab.
@Composable
internal fun ForestryToggle(
    includeForestry: Boolean,
    onIncludeForestryChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = includeForestry, role = Role.Switch, onValueChange = onIncludeForestryChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Include forestry & land use", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (includeForestry) {
                    "Net totals: land-use sinks are subtracted"
                } else {
                    "Matches the totals on climatetrace.org"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = includeForestry, onCheckedChange = null)
    }
}

@Composable
private fun EmissionsSummarySection(
    year: String,
    availableYears: List<String>,
    onYearSelected: (String) -> Unit,
    co2Value: String,
    co2PercentChange: Double,
    rank: Int,
    share: String,
    perCapita: String,
    perCapitaRank: Int?,
    yearlyEmissions: Map<String, Double>,
    isRefreshing: Boolean = false,
    includeForestry: Boolean = false,
    partialThroughMonth: Int? = null,
    onIncludeForestryChange: ((Boolean) -> Unit)? = null
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Emissions Summary",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))
            if (onIncludeForestryChange != null) {
                ForestryToggle(includeForestry, onIncludeForestryChange)
                Spacer(modifier = Modifier.size(8.dp))
            }
            YearChips(year, availableYears, onYearSelected)
            // Fixed-height slot so the figures below don't jump when the bar comes and goes.
            Box(modifier = Modifier.fillMaxWidth().height(12.dp), contentAlignment = Alignment.Center) {
                if (isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (partialThroughMonth != null) {
                Text(
                    text = "$year is still in progress: figures cover January to ${monthName(partialThroughMonth, short = false)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            KeyFiguresRow(
                co2Label = if (partialThroughMonth != null) "CO₂e · Jan–${monthName(partialThroughMonth)}" else "CO₂e",
                co2Value = co2Value,
                co2PercentChange = co2PercentChange,
                rank = rank,
                share = share,
                perCapita = perCapita,
                perCapitaRank = perCapitaRank
            )

            if (yearlyEmissions.size >= 2) {
                Spacer(modifier = Modifier.size(16.dp))
                CO2TrendSparkline(
                    yearlyEmissions = yearlyEmissions,
                    selectedYear = year,
                    modifier = Modifier.fillMaxWidth(),
                    onYearSelected = onYearSelected
                )
            }
        }
    }
}

@Composable
private fun EmissionsBySectorSection(
    countryAssetEmissionsList: List<dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo>,
    selectedSector: String?,
    onSectorChange: (String?) -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Emissions by Sector",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(12.dp))
            CountryAssetEmissionsInfoTreeMapChart(
                countryAssetEmissions = countryAssetEmissionsList,
                selectedSector = selectedSector,
                onSectorChange = onSectorChange
            )
        }
    }
}

internal fun prettySectorName(sector: String): String =
    sector.replace("-", " ").replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetsSection(
    assets: List<Asset>,
    selectedSector: String?,
    onClearSector: () -> Unit
) {
    val navigator = LocalNavigator.current
    val filtered = remember(assets, selectedSector) {
        if (selectedSector == null) assets else assets.filter { it.sector == selectedSector }
    }

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedSector != null) {
                        "Top Sources · ${prettySectorName(selectedSector)}"
                    } else {
                        "Top Emission Sources"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (selectedSector != null) {
                    AssistChip(
                        onClick = onClearSector,
                        label = { Text("All sectors") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))

            if (filtered.isEmpty()) {
                Text(
                    text = "No tracked sources in this sector.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                return@Column
            }

            val mapPoints = remember(filtered) {
                filtered.mapNotNull { asset ->
                    val latitude = asset.centroid?.latitude
                    val longitude = asset.centroid?.longitude
                    if (latitude == null || longitude == null) {
                        null
                    } else {
                        MapPoint(asset.id, latitude, longitude, asset.name, asset.emissionsQuantity ?: 0.0)
                    }
                }
            }
            if (mapPoints.isNotEmpty()) {
                var selectedSourceId by remember(filtered) { mutableStateOf<Int?>(null) }
                val selectedSource = filtered.firstOrNull { it.id == selectedSourceId }
                SourceMapView(
                    points = mapPoints,
                    selectedId = selectedSourceId,
                    onPointSelected = { selectedSourceId = if (it.id == selectedSourceId) null else it.id },
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
                if (selectedSource != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigator?.push(AssetDetailScreen(selectedSource.id, selectedSource.name)) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listOfNotNull(
                                selectedSource.name,
                                selectedSource.emissionsQuantity?.let { formatEmissionsQuantity(it) }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "View details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Tap a marker to identify the source. Larger markers emit more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                HorizontalDivider()
            }

            filtered.forEachIndexed { index, asset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navigator?.push(AssetDetailScreen(asset.id, asset.name))
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = sectorIcon(asset.sector),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        asset.sector?.let {
                            Text(
                                text = it.replace("-", " ").replaceFirstChar { c -> c.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val validOwners = asset.owners?.filter { !it.name.isNullOrBlank() }
                        if (!validOwners.isNullOrEmpty()) {
                            Text(
                                text = "Owners: ${validOwners.distinctBy { it.id ?: it.name }.joinToString { it.name ?: "" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index < filtered.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
internal fun CountryHeader(viewState: CountryDetailsUIState.Success) {
    val c = viewState.country
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val flag = alpha3ToAlpha2(c.id)?.let { FlagKit.getFlag(it) }
            if (flag != null) {
                Image(
                    imageVector = flag,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 56.dp, height = 38.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(
                    text = c.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "${c.continent} • ${c.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun KeyFiguresRow(
    co2Value: String,
    co2Label: String = "CO₂e",
    co2PercentChange: Double,
    rank: Int,
    share: String,
    perCapita: String,
    perCapitaRank: Int?
) {
    val perCapitaSecondary = perCapitaRank?.let { "#$it" }
    val deltaText: String?
    val deltaPositive: Boolean
    if (co2PercentChange == 0.0) {
        deltaText = null
        deltaPositive = false
    } else {
        val sign = if (co2PercentChange > 0) "+" else ""
        deltaText = "$sign${co2PercentChange.toString(1)}% yoy"
        deltaPositive = co2PercentChange > 0
    }

    val co2Card: @Composable (Modifier) -> Unit = { modifier ->
        StatCard(
            label = co2Label,
            value = co2Value,
            secondary = deltaText,
            secondaryColor = if (deltaText == null) {
                null
            } else if (deltaPositive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = modifier
        )
    }
    val rankCard: @Composable (Modifier) -> Unit = { modifier ->
        StatCard(label = "Rank", value = "#$rank", modifier = modifier)
    }
    val perCapitaCard: @Composable (Modifier) -> Unit = { modifier ->
        StatCard(label = "Per Capita", value = perCapita, secondary = perCapitaSecondary, modifier = modifier)
    }
    val shareCard: @Composable (Modifier) -> Unit = { modifier ->
        StatCard(label = "Global Share", value = share, modifier = modifier)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Four cards across leave each one too narrow for its value on a phone, where they'd
        // truncate to "53.0…" — fall back to a 2x2 grid there.
        if (maxWidth < 440.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    co2Card(Modifier.weight(1f))
                    rankCard(Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    perCapitaCard(Modifier.weight(1f))
                    shareCard(Modifier.weight(1f))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                co2Card(Modifier.weight(1f))
                rankCard(Modifier.weight(1f))
                perCapitaCard(Modifier.weight(1f))
                shareCard(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    secondaryColor: Color? = null
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Always reserve space so all four cards share the same height
            Text(
                text = secondary ?: "—",
                modifier = if (secondary == null) Modifier.alpha(0f) else Modifier,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor ?: MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun CO2TrendSparkline(
    yearlyEmissions: Map<String, Double>,
    selectedYear: String,
    modifier: Modifier = Modifier,
    onYearSelected: (String) -> Unit = {}
) {
    val ordered = yearlyEmissions.entries.sortedBy { it.key }
    val years = ordered.map { it.key }
    val values = ordered.map { it.value }
    val minVal = values.min()
    val maxVal = values.max()
    val range = (maxVal - minVal).coerceAtLeast(1.0)
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.15f)
    val markerColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    val selectedValue = yearlyEmissions[selectedYear]
    val titleSuffix = selectedValue?.let { " — $selectedYear: ${formatEmissionsQuantity(it)}" } ?: ""

    Column(modifier = modifier) {
        Text(
            text = "Trend$titleSuffix",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(years) {
                    detectTapGestures { offset ->
                        val n = years.size
                        if (n < 2) return@detectTapGestures
                        val stepX = size.width.toFloat() / (n - 1)
                        val index = (offset.x / stepX).roundToInt().coerceIn(0, n - 1)
                        onYearSelected(years[index])
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val n = values.size
            if (n < 2) return@Canvas
            val stepX = w / (n - 1)

            drawLine(
                color = gridColor,
                start = Offset(0f, h - 0.5f),
                end = Offset(w, h - 0.5f),
                strokeWidth = 1f
            )

            val points = values.mapIndexed { i, v ->
                val x = i * stepX
                val normalized = ((v - minVal) / range).toFloat()
                val y = h - normalized * (h - 6f) - 3f
                Offset(x, y)
            }

            val areaPath = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(areaPath, color = fillColor)

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(linePath, color = lineColor, style = Stroke(width = 2f))

            val selectedIndex = ordered.indexOfFirst { it.key == selectedYear }
            if (selectedIndex >= 0) {
                val p = points[selectedIndex]
                drawLine(
                    color = guideColor,
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, h),
                    strokeWidth = 1f
                )
                drawCircle(color = Color.White, radius = 5f, center = p)
                drawCircle(color = markerColor, radius = 3.5f, center = p)
            }
        }
        Spacer(modifier = Modifier.size(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ordered.first().key,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = ordered.last().key,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearChips(selectedYear: String, availableYears: List<String>, onYearSelected: (String) -> Unit) {
    val listState = rememberLazyListState()
    val selectedIndex = availableYears.indexOf(selectedYear)
    LaunchedEffect(selectedYear) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex.coerceAtLeast(0))
        }
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(availableYears) { year ->
            FilterChip(
                selected = year == selectedYear,
                onClick = { onYearSelected(year) },
                label = { Text(year) }
            )
        }
    }
}

