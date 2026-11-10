package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import by.overpass.treemapchart.compose.TreemapChart
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.util.toString

// Treemap of a country's emissions by sector. Tapping a sector drills into it: the chart and
// legend switch to that sector's subsectors until "All sectors" is tapped. The drilled-into
// sector is `selectedSector`, which the caller also uses to filter the sources list.
@Composable
internal fun CountryAssetEmissionsInfoTreeMapChart(
    countryAssetEmissions: List<CountryAssetEmissionsInfo>,
    selectedSector: String? = null,
    onSectorChange: (String?) -> Unit = {}
) {
    val sectorLeaves = remember(countryAssetEmissions) { buildAssetLeaves(countryAssetEmissions) }
    val selectedLeaf = sectorLeaves.firstOrNull { it.name == selectedSector }
    val subsectorLeaves = remember(countryAssetEmissions, selectedLeaf) {
        selectedLeaf?.let { buildSubsectorLeaves(countryAssetEmissions, it) }.orEmpty()
    }
    // A sector the API gives no breakdown for stays on the sector view, highlighted.
    val drilledInto = selectedLeaf?.takeIf { subsectorLeaves.isNotEmpty() }
    val leaves = if (drilledInto != null) subsectorLeaves else sectorLeaves

    val toggle: (String) -> Unit = { sector ->
        onSectorChange(if (selectedSector == sector) null else sector)
    }

    Column(Modifier.fillMaxWidth()) {
        if (drilledInto != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onSectorChange(null) },
                    label = { Text("All sectors") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${prettySectorName(drilledInto.name)} · ${formatMt(drilledInto.value)}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (sectorLeaves.isNotEmpty()) {
            Text(
                text = "Tap a sector to see its subsectors and sources.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(Modifier.height(360.dp).fillMaxWidth()) {
            if (leaves.isNotEmpty()) {
                TreemapChart(Modifier.fillMaxSize()) {
                    leaves.forEach { leaf ->
                        LeafItem(
                            item = leaf,
                            modifier = Modifier.weight(leaf.value),
                            dimmed = drilledInto == null && selectedSector != null && selectedSector != leaf.name,
                            isSelected = drilledInto == null && selectedSector == leaf.name,
                            // Subsectors are the bottom level: nothing further to open.
                            onClick = if (drilledInto == null) { { toggle(leaf.name) } } else null
                        )
                    }
                }
            }
        }
        if (leaves.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            SectorLegend(
                leaves = leaves,
                selectedSector = if (drilledInto == null) selectedSector else null,
                onSectorTap = if (drilledInto == null) toggle else null
            )
        }
    }
}

@Composable
private fun SectorLegend(
    leaves: List<ChartNode.Leaf>,
    selectedSector: String?,
    onSectorTap: ((String) -> Unit)?
) {
    Column(Modifier.fillMaxWidth()) {
        leaves.forEach { leaf ->
            val isSelected = leaf.name == selectedSector
            val dimmed = selectedSector != null && !isSelected
            val rowBackground = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBackground, shape = RoundedCornerShape(6.dp))
                    .then(if (onSectorTap != null) Modifier.clickable { onSectorTap(leaf.name) } else Modifier)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            leaf.color.copy(alpha = if (dimmed) 0.35f else 1f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                val labelAlpha = if (dimmed) 0.5f else 1f
                Text(
                    text = leaf.name.replace("-", " ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = labelAlpha),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatMt(leaf.value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = labelAlpha)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = leaf.percentage.toPercent(1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = labelAlpha),
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun formatMt(tonnes: Double): String {
    val mt = tonnes / 1_000_000.0
    return when {
        mt >= 100 -> "${mt.toInt()} Mt"
        mt >= 1 -> "${(mt * 10).toInt() / 10.0} Mt"
        else -> "${(tonnes / 1_000.0).toInt()} kt"
    }
}



@Composable
internal fun LeafItem(
    item: ChartNode.Leaf,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .border(
                width = if (isSelected) 3.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White,
            )
            .background(item.color.copy(alpha = if (dimmed) 0.3f else 1f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp),
    ) {
        ShrinkableHidableText(
            text = "${item.name}\n${item.percentage.toPercent(2)}",
            minSize = 6.sp,
        )
    }
}

internal fun Double.toPercent(precision: Int): String = "${(this * 100.0f).toString(precision)}%"




internal fun buildAssetLeaves(assetEmissionInfoList: List<CountryAssetEmissionsInfo>): List<ChartNode.Leaf> {
    // API returns both sector totals (subsector == null) and a parallel subsector
    // breakdown that sums to the same totals. Keep only sector-level rows so the
    // treemap doesn't double-count the same emissions at two granularities.
    val filtered = assetEmissionInfoList
        .filter { it.emissionsQuantity > 0 && it.sector != null && it.subsector == null }
        .sortedByDescending(CountryAssetEmissionsInfo::emissionsQuantity)
        .take(10)
    val total = filtered.sumOf { it.emissionsQuantity }.takeIf { it > 0 } ?: return emptyList()
    val colors = generateHueColorPalette(filtered.size)
    return filtered.mapIndexed { index, info ->
        ChartNode.Leaf(
            name = info.sector ?: "",
            value = info.emissionsQuantity,
            percentage = info.emissionsQuantity / total,
            color = colors[index]
        )
    }
}




// The subsectors that make up one sector, shaded from the sector's own colour so the drilled-in
// view still reads as "inside" the tile that was tapped. Percentages are shares of the sector.
// Names are display-ready ("Enteric fermentation cattle pasture"), unlike sector leaves, whose
// names double as the API's sector ids.
internal fun buildSubsectorLeaves(
    assetEmissionInfoList: List<CountryAssetEmissionsInfo>,
    sector: ChartNode.Leaf
): List<ChartNode.Leaf> {
    val subsectors = assetEmissionInfoList
        .filter { it.sector == sector.name && it.subsector != null && it.emissionsQuantity > 0 }
        .sortedByDescending(CountryAssetEmissionsInfo::emissionsQuantity)
    val total = subsectors.sumOf { it.emissionsQuantity }.takeIf { it > 0 } ?: return emptyList()
    val shown = subsectors.take(12)
    return shown.mapIndexed { index, info ->
        ChartNode.Leaf(
            name = prettySectorName(info.subsector ?: ""),
            value = info.emissionsQuantity,
            percentage = info.emissionsQuantity / total,
            color = lerp(sector.color, Color.White, 0.65f * index / shown.size)
        )
    }
}

@Suppress("LongParameterList")
@Composable
internal fun ShrinkableHidableText(
    text: String,
    minSize: TextUnit,
    modifier: Modifier = Modifier,
    shrinkSizeFactor: Float = 0.9F,
    textAlign: TextAlign = TextAlign.Center,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    var fontStyle by remember { mutableStateOf(style) }
    var shouldDraw by remember { mutableStateOf(false) }
    val show by remember { derivedStateOf { fontStyle.fontSize >= minSize } }
    if (show) {
        Text(
            text = text,
            modifier = modifier.drawWithContent {
                if (shouldDraw) {
                    drawContent()
                }
            },
            textAlign = textAlign,
            onTextLayout = { result ->
                if (result.hasVisualOverflow) {
                    fontStyle = fontStyle.copy(
                        fontSize = fontStyle.fontSize * shrinkSizeFactor,
                        letterSpacing = if (fontStyle.letterSpacing.isUnspecified) {
                            fontStyle.letterSpacing
                        } else {
                            fontStyle.letterSpacing * shrinkSizeFactor
                        },
                    )
                } else {
                    shouldDraw = true
                }
            },
            style = fontStyle,
        )
    }
}