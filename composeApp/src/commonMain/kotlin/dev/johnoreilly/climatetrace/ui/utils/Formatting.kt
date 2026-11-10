package dev.johnoreilly.climatetrace.ui.utils

import kotlin.math.abs
import kotlin.math.roundToInt

internal fun formatEmissionsQuantity(tonnes: Double): String {
    // Net figures can be negative (land-use sinks), so pick the unit from the magnitude.
    val magnitude = abs(tonnes)
    return when {
        magnitude >= 1_000_000_000 -> "${(tonnes / 1_000_000_000 * 100).roundToInt() / 100.0} Gt"
        magnitude >= 1_000_000 -> "${(tonnes / 1_000_000 * 100).roundToInt() / 100.0} Mt"
        magnitude >= 1_000 -> "${(tonnes / 1_000 * 100).roundToInt() / 100.0} kt"
        else -> "${(tonnes * 100).roundToInt() / 100.0} t"
    }
}
