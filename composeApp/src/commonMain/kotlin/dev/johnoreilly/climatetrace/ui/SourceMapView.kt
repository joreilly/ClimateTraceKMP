package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan

internal data class MapPoint(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    // Relative size of the marker (e.g. emissions); only its ratio to the other points matters.
    val weight: Double = 0.0
)

private const val TILE_DP = 256
private const val MIN_ZOOM = 1
private val MarkerTouchTarget = 36.dp

// Slippy-map tile maths: https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
private fun tileX(longitude: Double, zoom: Int): Double = (longitude + 180.0) / 360.0 * (1 shl zoom)

private fun tileY(latitude: Double, zoom: Int): Double {
    val latRad = latitude.coerceIn(-85.05, 85.05) * PI / 180.0
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
}

// The deepest zoom at which every point still fits inside the view, with room for the markers.
internal fun zoomToFit(points: List<MapPoint>, widthDp: Float, heightDp: Float, maxZoom: Int): Int {
    for (zoom in maxZoom downTo MIN_ZOOM + 1) {
        val xs = points.map { tileX(it.longitude, zoom) }
        val ys = points.map { tileY(it.latitude, zoom) }
        val fitsX = (xs.max() - xs.min()) * TILE_DP <= widthDp - MarkerTouchTarget.value
        val fitsY = (ys.max() - ys.min()) * TILE_DP <= heightDp - MarkerTouchTarget.value
        if (fitsX && fitsY) return zoom
    }
    return MIN_ZOOM
}

// A static OpenStreetMap view framed around `points`, each drawn as a tappable marker sized by
// its weight. Not pannable: it picks the zoom that fits everything and stays there. The
// modifier must give it a bounded size.
@Composable
internal fun SourceMapView(
    points: List<MapPoint>,
    modifier: Modifier = Modifier,
    selectedId: Int? = null,
    maxZoom: Int = 10,
    onPointSelected: ((MapPoint) -> Unit)? = null
) {
    if (points.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value
        val zoom = remember(points, widthDp, heightDp, maxZoom) { zoomToFit(points, widthDp, heightDp, maxZoom) }
        val tilesPerAxis = 1 shl zoom

        val xs = points.map { tileX(it.longitude, zoom) }
        val ys = points.map { tileY(it.latitude, zoom) }
        // Top-left corner of the view, in tile units.
        val left = (xs.min() + xs.max()) / 2 - widthDp / TILE_DP / 2
        val top = (ys.min() + ys.max()) / 2 - heightDp / TILE_DP / 2

        val tiles = buildList {
            for (ty in floor(top).toInt()..floor(top + heightDp / TILE_DP).toInt()) {
                if (ty !in 0 until tilesPerAxis) continue
                for (tx in floor(left).toInt()..floor(left + widthDp / TILE_DP).toInt()) {
                    // Longitude wraps around the antimeridian; latitude does not.
                    val wrappedX = (tx % tilesPerAxis + tilesPerAxis) % tilesPerAxis
                    add(Triple("https://tile.openstreetmap.org/$zoom/$wrappedX/$ty.png", tx - left, ty - top))
                }
            }
        }

        val maxWeight = points.maxOf { it.weight }.takeIf { it > 0 } ?: 1.0
        // Biggest first so small markers stay tappable on top; the selected one goes last.
        val ordered = points.indices.sortedWith(
            compareBy<Int> { points[it].id == selectedId }.thenByDescending { points[it].weight }
        )

        // Tiles and markers are positioned by hand: each child reports its own size and is
        // placed at its dp offset from the top-left corner (markers centred on their point).
        val offsets: List<Pair<Double, Double>> =
            tiles.map { it.second * TILE_DP to it.third * TILE_DP } +
                ordered.map {
                    (xs[it] - left) * TILE_DP - MarkerTouchTarget.value / 2 to
                        (ys[it] - top) * TILE_DP - MarkerTouchTarget.value / 2
                }

        Layout(
            content = {
                tiles.forEach { (url, _, _) ->
                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(TILE_DP.dp))
                }
                ordered.forEach { index ->
                    val point = points[index]
                    val diameter = (10 + 12 * sqrt((point.weight / maxWeight).coerceIn(0.0, 1.0))).dp
                    Marker(
                        label = point.label,
                        diameter = diameter,
                        selected = point.id == selectedId,
                        onClick = onPointSelected?.let { { it(point) } }
                    )
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(Constraints()) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val (x, y) = offsets[index]
                    placeable.place(x.dp.toPx().roundToInt(), y.dp.toPx().roundToInt())
                }
            }
        }

        // Required by the OSM tile licence.
        Text(
            text = "© OpenStreetMap",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.White.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun Marker(label: String, diameter: Dp, selected: Boolean, onClick: (() -> Unit)?) {
    val fill = if (selected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(MarkerTouchTarget)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = "Show $label", onClick = onClick)
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(if (selected) diameter + 4.dp else diameter)
                .background(fill.copy(alpha = if (selected) 1f else 0.8f), CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}
