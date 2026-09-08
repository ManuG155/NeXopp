// Ruta: app/src/main/java/com/nexopp/ui/ToolbarViewPopups.kt
package com.nexopp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexopp.render.GuideKind
import kotlin.math.roundToInt

@Composable
internal fun ZoomPopupButton(zoom: Float, onZoomIn: () -> Unit, onZoomOut: () -> Unit, onZoomReset: () -> Unit) {
    ToolbarPopupButton(
        face = { open ->
            TextButton(onClick = open) {
                Text("${(zoom * 100).roundToInt()}%")
            }
        },
    ) { dismiss ->
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onZoomOut) { Icon(Icons.Filled.ZoomOut, contentDescription = "Alejar") }
            TextButton(onClick = onZoomReset) { Text("${(zoom * 100).roundToInt()}%") }
            IconButton(onClick = onZoomIn) { Icon(Icons.Filled.ZoomIn, contentDescription = "Acercar") }
        }
    }
}

private val BACKGROUND_STYLES: List<Pair<String, String>> = listOf(
    "plain" to "Liso",
    "lined" to "Rayado",
    "ruled" to "Pautado",
    "graph" to "Cuadriculado",
    "dotted" to "Punteado",
)

@Composable
internal fun BackgroundPopupButton(style: String?, onBackgroundStyle: (String) -> Unit) {
    ToolbarPopupButton(
        icon = Icons.Filled.GridOn,
        contentDescription = "Fondo de página",
    ) { dismiss ->
        for ((value, label) in BACKGROUND_STYLES) {
            DropdownMenuItem(
                text = { Text(label) },
                enabled = style != null,
                trailingIcon = {
                    if (value == style) Icon(Icons.Filled.Check, contentDescription = "seleccionado")
                },
                onClick = { onBackgroundStyle(value); dismiss() },
            )
        }
    }
}

@Composable
internal fun GuidePopupButton(kind: GuideKind, onKind: (GuideKind) -> Unit) {
    ToolbarPopupButton(
        icon = Icons.Filled.ChangeHistory,
        contentDescription = "Guías de dibujo",
        tint = if (kind == GuideKind.NONE) LocalContentColor.current
        else MaterialTheme.colorScheme.primary,
    ) { dismiss ->
        MenuHeading("Guía de dibujo")
        for (option in GuideKind.entries) {
            DropdownMenuItem(
                text = { Text(option.label) },
                trailingIcon = { if (option == kind) Icon(Icons.Filled.Check, contentDescription = "seleccionado") },
                onClick = { onKind(option); dismiss() },
            )
        }
    }
}