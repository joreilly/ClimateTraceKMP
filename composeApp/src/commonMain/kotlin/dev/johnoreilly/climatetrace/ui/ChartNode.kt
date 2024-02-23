package dev.johnoreilly.climatetrace.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
sealed class ChartNode {

    abstract val name: String

    abstract val value: Double

    abstract val percentage: Double

    @Stable
    data class Leaf(
        override val name: String,
        override val value: Double,
        override val percentage: Double,
        val color: Color,
    ) : ChartNode()

    @Stable
    data class Section(
        override val name: String,
        override val value: Double,
        override val percentage: Double,
        val color: Color?,
    ) : ChartNode()
}