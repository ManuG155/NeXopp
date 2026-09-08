// --- ListRowControls.kt ---
package com.nexopp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ReorderControls(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean = true,
    vertical: Boolean = true,
    itemName: String,
    onMove: (delta: Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (vertical) {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(Icons.Filled.ArrowDropUp, contentDescription = "Subir $itemName")
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Bajar $itemName")
            }
        } else {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mover $itemName antes")
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mover $itemName después")
            }
        }
        IconButton(onClick = onDelete, enabled = canDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar $itemName")
        }
    }
}