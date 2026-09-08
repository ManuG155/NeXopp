// Ruta: app/src/main/java/com/nexopp/ui/ToolbarViewPopups.kt
package com.nexopp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenuItem
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

data class AudioUiState(
    val recording: Boolean = false,
    val playing: Boolean = false,
    val folderChosen: Boolean = false,
    val onToggleRecord: () -> Unit = {},
    val onStopPlayback: () -> Unit = {},
    val onChooseFolder: () -> Unit = {}
)

@Composable
internal fun AudioPopupButton(audio: AudioUiState) {
    ToolbarPopupButton(
        icon = if (audio.recording || audio.playing) Icons.Filled.Stop else Icons.Filled.Mic,
        contentDescription = "Audio",
        active = audio.recording || audio.playing
    ) { dismiss ->
        MenuHeading("Audio")
        if (audio.recording || audio.playing) {
            DropdownMenuItem(
                text = { Text(if (audio.recording) "Detener grabación" else "Detener reproducción") },
                leadingIcon = { Icon(Icons.Filled.Stop, contentDescription = null) },
                onClick = {
                    if (audio.recording) audio.onToggleRecord() else audio.onStopPlayback()
                    dismiss()
                }
            )
        } else {
            DropdownMenuItem(
                text = { Text("Iniciar grabación") },
                leadingIcon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                onClick = { audio.onToggleRecord(); dismiss() }
            )
        }
        DropdownMenuItem(
            text = { Text(if (audio.folderChosen) "Cambiar carpeta de audio" else "Elegir carpeta de audio") },
            onClick = { audio.onChooseFolder(); dismiss() }
        )
    }
}