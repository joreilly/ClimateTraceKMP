import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
actual fun CountryAssetEmissionsInfoTreeMapChart(countryAssetEmissions: List<CountryAssetEmissionsInfo>) {
    var tree by remember { mutableStateOf<Tree<ChartNode>?>(null) }

    LaunchedEffect(countryAssetEmissions) {
        tree = buildAssetTree(countryAssetEmissions ?: emptyList())
    }

    Column(Modifier.height(500.dp).fillMaxWidth(0.8f)) {
        tree?.let {
            TreemapChart(
                data = it,
                evaluateItem = ChartNode::value
            ) { node, groupContent ->
                val export = node.data
                if (node.children.isEmpty() && export is ChartNode.Leaf) {
                    LeafItem(item = export, onClick = { })
                } else if (export is ChartNode.Section) {
                    SectionItem(export.color) {
                        groupContent(node)
                    }
                }
            }
        }
    }
}


@Composable
fun LeafItem(
    item: ChartNode.Leaf,
    modifier: Modifier = Modifier,
    onClick: (ChartNode.Leaf) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .border(0.5.dp, Color.White)
            .background(item.color)
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




suspend fun buildAssetTree(assetEmissionInfoList: List<CountryAssetEmissionsInfo>): Tree<ChartNode> = withContext(
    Dispatchers.Default) {
    val filteredList = assetEmissionInfoList
        .filter { it.emissions > 0 }
        .sortedByDescending(CountryAssetEmissionsInfo::emissions)
        .take(10)

    val colors = generateHueColorPalette(filteredList.size)

    val total = filteredList.sumOf { it.emissions.toDouble() }  //.sumOf(CountryAssetEmissionsInfo::emissions)
    tree(
        ChartNode.Section(
            name = "Total",
            value = total,
            percentage = 1.0,
            color = null,
        ),
    ) {
        assetEmissionInfoList
            .filter { it.emissions > 0 }
            .sortedByDescending(CountryAssetEmissionsInfo::emissions)
            .take(10)
            .forEachIndexed { index, assetEmissionInfo ->
                val productPercentage = assetEmissionInfo.emissions / total
                node(
                    ChartNode.Leaf(
                        name = assetEmissionInfo.sector,
                        value = assetEmissionInfo.emissions.toDouble(),
                        percentage = productPercentage,
                        color = colors[index]
                    ),
                )
            }

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
    style: TextStyle = androidx.compose.material.MaterialTheme.typography.body1,
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