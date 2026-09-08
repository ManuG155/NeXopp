// Ruta: app/src/main/java/com/nexopp/ui/ToolbarPopup.kt
package com.nexopp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

internal val ToolbarButtonSize = 44.dp

@Composable
internal fun MenuHeading(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ToolbarPopupButton(
    icon: ImageVector,
    contentDescription: String,
    heading: String? = null,
    active: Boolean = false,
    tint: androidx.compose.ui.graphics.Color? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val iconTint = tint ?: if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current
    Box {
        Box(
            modifier = Modifier
                .size(ToolbarButtonSize)
                .clip(RoundedCornerShape(12.dp))
                .then(if (active) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                .combinedClickable(
                    onClick = { open = true },
                    onLongClick = onLongClick,
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = iconTint)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (heading != null) MenuHeading(heading)
            content { open = false }
        }
    }
}

@Composable
internal fun ToolbarPopupButton(
    face: @Composable (open: () -> Unit) -> Unit,
    heading: String? = null,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        face { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (heading != null) MenuHeading(heading)
            content { open = false }
        }
    }
}