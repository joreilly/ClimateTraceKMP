package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import by.overpass.treemapchart.compose.TreemapChart
import by.overpass.treemapchart.core.tree.Tree
import by.overpass.treemapchart.core.tree.tree
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.util.toString

@Composable
fun CountryAssetEmissionsInfoTreeMapChart(countryAssetEmissions: List<CountryAssetEmissionsInfo>) {
    val leaves = remember(countryAssetEmissions) { buildAssetLeaves(countryAssetEmissions) }
    val tree = remember(leaves) { buildAssetTree(leaves) }
    var selectedSector by remember(countryAssetEmissions) { mutableStateOf<String?>(null) }

    val toggle: (String) -> Unit = { sector ->
        selectedSector = if (selectedSector == sector) null else sector
    }

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.height(360.dp).fillMaxWidth()) {
            TreemapChart(
                data = tree,
                evaluateItem = ChartNode::value
            ) { node, groupContent ->
                val export = node.data
                if (node.children.isEmpty() && export is ChartNode.Leaf) {
                    LeafItem(
                        item = export,
                        dimmed = selectedSector != null && selectedSector != export.name,
                        onClick = { toggle(export.name) }
                    )
                } else if (export is ChartNode.Section) {
                    SectionItem(export.color) {
                        groupContent(node)
                    }
                }
            }
        }
        if (leaves.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            SectorLegend(leaves, selectedSector, toggle)
        }
    }
}

@Composable
private fun SectorLegend(
    leaves: List<ChartNode.Leaf>,
    selectedSector: String?,
    onSectorTap: (String) -> Unit
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
                    .clickable { onSectorTap(leaf.name) }
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
fun LeafItem(
    item: ChartNode.Leaf,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    onClick: (ChartNode.Leaf) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .border(0.5.dp, Color.White)
            .background(item.color.copy(alpha = if (dimmed) 0.3f else 1f))
            .clickable { onClick(item) }
            .padding(4.dp),
    ) {
        ShrinkableHidableText(
            text = "${item.name}\n${item.percentage.toPercent(2)}",
            minSize = 6.sp,
        )
    }
}

fun Double.toPercent(precision: Int): String = "${(this * 100.0f).toString(precision)}%"


@Composable
fun SectionItem(
    sectionColor: Color?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (sectionColor != null) {
        Box(
            modifier = modifier
                .background(sectionColor)
        ) {
            content()
        }
    } else {
        content()
    }
}


fun buildAssetLeaves(assetEmissionInfoList: List<CountryAssetEmissionsInfo>): List<ChartNode.Leaf> {
    val filtered = assetEmissionInfoList
        .filter { it.emissionsQuantity > 0 && it.sector != null }
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

fun buildAssetTree(leaves: List<ChartNode.Leaf>): Tree<ChartNode> {
    val total = leaves.sumOf { it.value }
    return tree(
        ChartNode.Section(
            name = "Total",
            value = total,
            percentage = 1.0,
            color = null,
        ),
    ) {
        leaves.forEach { leaf -> node(leaf) }
    }
}



@Suppress("LongParameterList")
@Composable
fun ShrinkableHidableText(
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