// Ruta: app/src/main/java/com/nexopp/ui/TabStrip.kt
package com.nexopp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class TabsUiState(
    val titles: List<String> = emptyList(),
    val dotColors: List<Int?> = emptyList(),
    val activeIndex: Int = 0,
    val onSelect: (Int) -> Unit = {},
    val onClose: (Int) -> Unit = {},
    val onNew: () -> Unit = {},
    val onMove: (Int) -> Unit = {},
    val onMirror: (Int) -> Unit = {},
    val onReorder: (Int, Int) -> Unit = { _, _ -> },
    val onOverview: () -> Unit = {},
    val preview: (Int, Int, (Bitmap?) -> Unit) -> Unit = { _, _, done -> done(null) },
)

@Composable
fun TabStrip(state: TabsUiState, modifier: Modifier = Modifier) {
    if (state.titles.isEmpty()) return
    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val scroll = rememberScrollState()
    var activeBounds by remember { mutableStateOf(0f to 0f) }
    var viewportWidth by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(activeBounds, viewportWidth, scroll.maxValue) {
        val (left, width) = activeBounds
        if (viewportWidth <= 0f || width <= 0f) return@LaunchedEffect
        val target = when {
            left < scroll.value -> left
            left + width > scroll.value + viewportWidth -> left + width - viewportWidth
            else -> return@LaunchedEffect
        }
        scroll.animateScrollTo(target.toInt().coerceIn(0, scroll.maxValue))
    }
    Row(
        modifier = modifier
            .height(TAB_STRIP_HEIGHT)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .onSizeChanged { viewportWidth = it.width.toFloat() }
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.titles.forEachIndexed { index, title ->
            TabChip(
                title = title,
                dotColor = state.dotColors.getOrNull(index),
                selected = index == state.activeIndex,
                onSelect = { state.onSelect(index) },
                onClose = { state.onClose(index) },
                onMove = { state.onMove(index) },
                onMirror = { state.onMirror(index) },
                dragOffset = if (index == dragIndex) dragOffset else 0f,
                onDragStart = { dragIndex = index; dragOffset = 0f },
                onDrag = { dx, width ->
                    if (dragIndex >= 0) {
                        dragOffset += dx
                        while (dragOffset > width && dragIndex < state.titles.lastIndex) {
                            state.onReorder(dragIndex, dragIndex + 1)
                            dragIndex++
                            dragOffset -= width
                        }
                        while (dragOffset < -width && dragIndex > 0) {
                            state.onReorder(dragIndex, dragIndex - 1)
                            dragIndex--
                            dragOffset += width
                        }
                        dragOffset = dragOffset.coerceIn(-width, width)
                    }
                },
                onDragEnd = { dragIndex = -1; dragOffset = 0f },
                onBounds = { left, width -> activeBounds = left to width },
            )
        }
        IconButton(onClick = state.onNew, modifier = Modifier.size(TAB_TOUCH_TARGET)) {
            Icon(Icons.Filled.Add, contentDescription = "Nuevo documento", modifier = Modifier.size(TAB_NEW_ICON))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabChip(
    title: String,
    dotColor: Int?,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onMove: () -> Unit,
    onMirror: () -> Unit,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDrag: (dx: Float, chipWidth: Float) -> Unit,
    onDragEnd: () -> Unit,
    onBounds: (left: Float, width: Float) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val drag = rememberUpdatedState(Triple(onDragStart, onDrag, onDragEnd))
    var chipWidth by remember { mutableFloatStateOf(1f) }
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = TAB_CHIP_VERTICAL_PADDING)
            .heightIn(min = TAB_TOUCH_TARGET)
            .graphicsLayer { translationX = dragOffset }
            .onSizeChanged { chipWidth = it.width.toFloat().coerceAtLeast(1f) }
            .onGloballyPositioned {
                if (selected) onBounds(it.positionInParent().x, it.size.width.toFloat())
            }
            .tabChipInteractions(selected, onSelect, { menuOpen = true }, drag, chipWidth)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabChipMenu(expanded = menuOpen, onDismiss = { menuOpen = false }, onMove = onMove, onMirror = onMirror)
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(TAB_DOT_SIZE)
                    .clip(CircleShape)
                    .background(Color(dotColor)),
            )
        }
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 64.dp, max = 180.dp),
        )
        TabCloseButton(title = title, selected = selected, onClose = onClose)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.tabChipInteractions(
    selected: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
    drag: androidx.compose.runtime.State<Triple<() -> Unit, (Float, Float) -> Unit, () -> Unit>>,
    chipWidth: Float,
): Modifier {
    return this
        .clip(RoundedCornerShape(8.dp))
        .background(
            if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
        .combinedClickable(onClick = onSelect, onLongClick = onLongClick)
        .pointerInput(selected) {
            if (!selected) return@pointerInput
            detectHorizontalDragGestures(
                onDragStart = { drag.value.first },
                onDragEnd = { drag.value.third },
                onDragCancel = { drag.value.third },
            ) { change, dx ->
                change.consume()
                drag.value.second(dx, chipWidth)
            }
        }
}

@Composable
private fun TabChipMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
    onMirror: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Mover a la otra vista") },
            onClick = { onDismiss(); onMove() },
        )
        DropdownMenuItem(
            text = { Text("Duplicar en la otra vista") },
            onClick = { onDismiss(); onMirror() },
        )
    }
}

@Composable
private fun TabCloseButton(
    title: String,
    selected: Boolean,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(TAB_TOUCH_TARGET).clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = "Cerrar $title",
            modifier = Modifier.size(TAB_CLOSE_ICON),
            tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        )
    }
}

private val TAB_DOT_SIZE = 6.dp
private val TAB_TOUCH_TARGET = 36.dp
private val TAB_CLOSE_ICON = 16.dp
private val TAB_NEW_ICON = 18.dp
private val TAB_CHIP_VERTICAL_PADDING = 3.dp
private val TAB_STRIP_HEIGHT = TAB_TOUCH_TARGET + TAB_CHIP_VERTICAL_PADDING * 2