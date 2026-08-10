package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

// Low zoom level (45deg/tile) keeps the 3x3 tile grid stable across most ISS
// polls, so we're not re-fetching OSM tiles on every position update.
private const val MAP_ZOOM = 3

// Renders a 3x3 grid of OpenStreetMap tiles (https://tile.openstreetmap.org)
// centered on the given coordinates, with a marker positioned at the exact
// sub-tile pixel offset using the standard slippy-map tile math:
// https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
// The modifier determines the overall (square) size - e.g. fillMaxWidth().aspectRatio(1f) -
// and each of the 9 tiles is sized to a third of that.
@Composable
fun IssMapView(latitude: Double, longitude: Double, modifier: Modifier = Modifier) {
    val tilesPerAxis = 1 shl MAP_ZOOM

    val latRad = latitude * PI / 180.0
    val xTileFrac = (longitude + 180.0) / 360.0 * tilesPerAxis
    val yTileFrac = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * tilesPerAxis

    val originXTile = floor(xTileFrac).toInt() - 1
    val originYTile = (floor(yTileFrac).toInt() - 1).coerceIn(0, tilesPerAxis - 1)

    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val tileSize = maxWidth / 3

        Column {
            for (row in 0..2) {
                Row {
                    for (col in 0..2) {
                        // Longitude wraps around the antimeridian; latitude does not.
                        val tileX = ((originXTile + col) % tilesPerAxis + tilesPerAxis) % tilesPerAxis
                        val tileY = (originYTile + row).coerceIn(0, tilesPerAxis - 1)
                        AsyncImage(
                            model = "https://tile.openstreetmap.org/$MAP_ZOOM/$tileX/$tileY.png",
                            contentDescription = null,
                            modifier = Modifier.size(tileSize)
                        )
                    }
                }
            }
        }

        val tileSizePx = tileSize.value.toDouble()
        val markerX = (xTileFrac - originXTile) * tileSizePx
        val markerY = (yTileFrac - originYTile) * tileSizePx

        Icon(
            imageVector = Icons.Default.SatelliteAlt,
            contentDescription = "ISS position",
            tint = Color.Red,
            modifier = Modifier
                .offset(x = markerX.dp - 12.dp, y = markerY.dp - 12.dp)
                .size(24.dp)
        )
    }
}
