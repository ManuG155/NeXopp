// Ruta: app/src/main/java/com/nexopp/ui/ToolbarSection.kt
package com.nexopp.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToolbarSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Text("Botones de la barra superior", style = MaterialTheme.typography.titleSmall)
    Text(
        "Apaga un botón para ocultarlo, o mantén pulsado un elemento y arrástralo arriba o abajo para reordenarlo. La barra los dibuja de izquierda a derecha.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(8.dp))

    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    var rowHeightPx by remember { mutableStateOf(0) }

    val items = orderedRailItems(settings.railOrder)
    val dropIndex = dragTargetIndex(dragIndex, dragOffset, rowHeightPx, items.size)
    items.forEachIndexed { index, item ->
        RailItemRow(
            item = item,
            shown = item.id !in settings.railHidden,
            dragging = dragIndex == index,
            dragOffset = when {
                dragIndex == index -> dragOffset
                dragIndex < 0 -> 0f
                index in (dragIndex + 1)..dropIndex -> -rowHeightPx.toFloat()
                index in dropIndex until dragIndex -> rowHeightPx.toFloat()
                else -> 0f
            },
            onShown = { shown ->
                val hidden = if (shown) settings.railHidden - item.id else settings.railHidden + item.id
                onChange(settings.copy(railHidden = hidden))
            },
            onHeight = { rowHeightPx = it },
            onDragStart = { dragIndex = index; dragOffset = 0f },
            onDrag = { dy -> dragOffset += dy },
            onDragEnd = {
                val to = dragTargetIndex(dragIndex, dragOffset, rowHeightPx, items.size)
                if (dragIndex >= 0 && to != dragIndex) {
                    onChange(
                        settings.copy(railOrder = moveRailItem(settings.railOrder, dragIndex, to - dragIndex))
                    )
                }
                dragIndex = -1
                dragOffset = 0f
            },
        )
    }
}