package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.johnoreilly.climatetrace.ui.utils.formatEmissionsQuantity
import kotlin.math.abs

private const val MONTHS = 12

private val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private val FullMonthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

internal fun monthName(month: Int, short: Boolean = true): String =
    (if (short) MonthNames else FullMonthNames).getOrNull(month - 1) ?: month.toString()

// One bar per month, drawn from a zero baseline so net-negative months (land-use sinks) hang
// below it. Tapping a bar shows that month's figure; until then the caption names the peak.
// `values` are (month 1..12, tonnes). The chart always spans January to December, so a year that
// is still in progress shows its remaining months as empty slots rather than stretching to fit.
@Composable
internal fun MonthlyEmissionsChart(
    values: List<Pair<Int, Double>>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return
    var selectedMonth by remember(values) { mutableStateOf<Int?>(null) }
    val peak = values.maxBy { it.second }
    val shown = values.firstOrNull { it.first == selectedMonth }
    val caption = if (shown != null) {
        "${monthName(shown.first)}: ${formatEmissionsQuantity(shown.second)}"
    } else {
        "Peak in ${monthName(peak.first)}: ${formatEmissionsQuantity(peak.second)}"
    }

    val barColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val hi = maxOf(0.0, values.maxOf { it.second })
    val lo = minOf(0.0, values.minOf { it.second })
    val range = (hi - lo).takeIf { it > 0.0 } ?: 1.0

    Column(modifier = modifier) {
        Text(
            text = caption,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .semantics {
                    contentDescription = "Monthly emissions. " + values.joinToString {
                        "${monthName(it.first)} ${formatEmissionsQuantity(it.second)}"
                    }
                }
                .pointerInput(values) {
                    detectTapGestures { offset ->
                        val slot = size.width.toFloat() / MONTHS
                        val tapped = (offset.x / slot).toInt().coerceIn(0, MONTHS - 1) + 1
                        if (values.any { it.first == tapped }) {
                            selectedMonth = if (tapped == selectedMonth) null else tapped
                        }
                    }
                }
        ) {
            val slot = size.width / MONTHS
            val barWidth = slot * 0.64f
            val zeroY = (hi / range).toFloat() * size.height
            values.forEach { (month, value) ->
                val index = month - 1
                val barHeight = (abs(value) / range).toFloat() * size.height
                val top = if (value >= 0) zeroY - barHeight else zeroY
                val dimmed = selectedMonth != null && selectedMonth != month
                drawRect(
                    color = barColor.copy(alpha = if (dimmed) 0.35f else 1f),
                    topLeft = Offset(index * slot + (slot - barWidth) / 2, top),
                    // Keep a sliver visible for months that round to nothing at this scale.
                    size = Size(barWidth, barHeight.coerceAtLeast(1f))
                )
            }
            drawLine(
                color = baselineColor,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx()
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            val monthsWithData = values.map { it.first }.toSet()
            (1..MONTHS).forEach { month ->
                Text(
                    text = monthName(month).take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (month in monthsWithData) 1f else 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
