package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.FlowLegend
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.util.toString

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun SectorEmissionsPieChart(
    assetEmissionsInfoList: List<CountryAssetEmissionsInfo>,
    modifier: Modifier = Modifier,
) {
    val filteredEmissionsList = assetEmissionsInfoList
        .filter { it.emissions > 0 }
        .sortedByDescending { it.emissions }
        .take(10)
    val values = filteredEmissionsList.map { it.emissions / 1_000_000 }
    val labels = filteredEmissionsList.mapNotNull { it.sector }
    val total = values.sum()
    val colors = generateHueColorPalette(values.size)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PieChart(
            values = values,
            modifier = modifier.padding(start = 8.dp),
            slice = { index: Int ->
                DefaultSlice(
                    color = colors[index],
                    hoverExpandFactor = 1.05f,
                    hoverElement = {
                        HoverSurface {
                            Column(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.Center)
                            ) {
                                Text(
                                    text = (values[index] / total).toPercent(1),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = values[index].toString(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                )
            },
            label = { i ->
                Text((values[i] / total).toPercent(1))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            FlowLegend(
                itemCount = labels.size,
                symbol = { i ->
                    Symbol(
                        modifier = Modifier.size(8.dp),
                        fillBrush = SolidColor(colors[i])
                    )
                },
                label = { labelIndex ->
                    Text(text = labels[labelIndex])
                },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}


fun Float.toPercent(precision: Int): String = "${(this * 100.0f).toString(precision)}%"

@Composable
fun HoverSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = Color.LightGray,
        modifier = modifier.padding(8.dp)
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            content()
        }
    }
}
