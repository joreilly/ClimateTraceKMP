package dev.johnoreilly.climatetrace.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector

fun sectorIcon(sector: String?): ImageVector = when (sector?.lowercase()) {
    "power" -> Icons.Filled.Bolt
    "transportation" -> Icons.Filled.DirectionsCar
    "buildings" -> Icons.Filled.Apartment
    "fossil-fuel-operations" -> Icons.Filled.LocalFireDepartment
    "mineral-extraction" -> Icons.Filled.Terrain
    "manufacturing" -> Icons.Filled.Factory
    "agriculture" -> Icons.Filled.Agriculture
    "forestry-and-land-use" -> Icons.Filled.Park
    "waste" -> Icons.Filled.Delete
    "fluorinated-gases" -> Icons.Filled.Science
    else -> Icons.Filled.Category
}
