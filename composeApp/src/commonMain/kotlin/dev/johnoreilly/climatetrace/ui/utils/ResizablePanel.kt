package dev.johnoreilly.climatetrace.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SplitterState {
    var isResizing by mutableStateOf(false)
    var isResizeEnabled by mutableStateOf(true)
}


class PanelState {
    val collapsedSize = 40.dp
    var expandedSize by mutableStateOf(250.dp)
    val expandedSizeMin = 120.dp
    var isExpanded by mutableStateOf(true)
    val splitter = SplitterState()
}

@Composable
fun ResizablePanel(
    modifier: Modifier,
    state: PanelState,
    title: String,
    content: @Composable () -> Unit,
) {
    val alpha = animateFloatAsState(
        if (state.isExpanded) 1f else 0f,
        SpringSpec(stiffness = Spring.StiffnessLow),
    ).value

    Box(modifier) {
        Column {
            Row(
                modifier = Modifier.height(32.dp).padding(6.dp)
                    .clickable { state.isExpanded = !state.isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (state.isExpanded) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                    contentDescription = if (state.isExpanded) "Collapse" else "Expand",
                    tint = LocalContentColor.current,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 2.dp, end = 2.dp, bottom = 2.dp)
                )
            }

            if (state.isExpanded) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Gray)
                )

                Column(Modifier.fillMaxSize().padding(top = 4.dp).graphicsLayer(alpha = alpha)) {
                    content()
                }
            }
        }
    }
}
