package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import dev.johnoreilly.climatetrace.ui.utils.alpha3ToAlpha2
import dev.johnoreilly.climatetrace.ui.utils.formatEmissionsQuantity
import dev.johnoreilly.climatetrace.ui.utils.sectorIcon
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsUIState
@Composable
fun CountryInfoDetailedView(
    viewState: CountryDetailsUIState,
    perCapitaRank: Int? = null,
    onYearSelected: (String) -> Unit
) {
    when (viewState) {
        CountryDetailsUIState.NoCountrySelected -> {
            Column(
                modifier = Modifier.fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(text = "No Country Selected.", style = MaterialTheme.typography.titleMedium)
            }
        }
        is CountryDetailsUIState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        }
        is CountryDetailsUIState.Error -> { Text("Error") }
        is CountryDetailsUIState.Success -> {
            CountryInfoDetailedViewSuccess(viewState, perCapitaRank, onYearSelected)
        }
    }
}


@Composable
fun CountryInfoDetailedViewSuccess(
    viewState: CountryDetailsUIState.Success,
    perCapitaRank: Int?,
    onYearSelected: (String) -> Unit
) {
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
                yearlyEmissions = viewState.yearlyEmissions
            )

            // Emissions by Sector Section
            val filteredCountryAssetEmissionsList = countryAssetEmissionsList.filter { it.sector != null }
            if (filteredCountryAssetEmissionsList.isNotEmpty()) {
                EmissionsBySectorSection(countryAssetEmissionsList)
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
            AssetsSection(viewState.assets)
        }
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
    yearlyEmissions: Map<String, Double>
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
            YearChips(year, availableYears, onYearSelected)
            Spacer(modifier = Modifier.size(12.dp))
            KeyFiguresRow(
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
private fun EmissionsBySectorSection(countryAssetEmissionsList: List<dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo>) {
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
            CountryAssetEmissionsInfoTreeMapChart(countryAssetEmissionsList)
        }
    }
}

@Composable
private fun AssetsSection(assets: List<Asset>) {
    val navigator = LocalNavigator.current

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Top Emission Sources",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))

            assets.forEachIndexed { index, asset ->
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
                                contentDescription = asset.sector,
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
                        contentDescription = "View details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index < assets.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CountryHeader(viewState: CountryDetailsUIState.Success) {
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
                    contentDescription = c.name,
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
private fun KeyFiguresRow(
    co2Value: String,
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "CO₂e",
            value = co2Value,
            secondary = deltaText,
            secondaryColor = if (deltaText == null) {
                null
            } else if (deltaPositive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.weight(1f)
        )
        StatCard(label = "Rank", value = "#$rank", modifier = Modifier.weight(1f))
        StatCard(
            label = "Per Capita",
            value = perCapita,
            secondary = perCapitaSecondary,
            modifier = Modifier.weight(1f)
        )
        StatCard(label = "Share", value = share, modifier = Modifier.weight(1f))
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
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor ?: MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CO2TrendSparkline(
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
fun YearChips(selectedYear: String, availableYears: List<String>, onYearSelected: (String) -> Unit) {
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

