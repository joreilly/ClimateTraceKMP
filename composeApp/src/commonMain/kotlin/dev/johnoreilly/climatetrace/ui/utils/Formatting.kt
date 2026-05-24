package dev.johnoreilly.climatetrace.ui.utils

import kotlin.math.roundToInt

fun formatEmissionsQuantity(tonnes: Double): String {
    return when {
        tonnes >= 1_000_000_000 -> "${(tonnes / 1_000_000_000 * 100).roundToInt() / 100.0} Gt"
        tonnes >= 1_000_000 -> "${(tonnes / 1_000_000 * 100).roundToInt() / 100.0} Mt"
        tonnes >= 1_000 -> "${(tonnes / 1_000 * 100).roundToInt() / 100.0} kt"
        else -> "${(tonnes * 100).roundToInt() / 100.0} t"
    }
}
