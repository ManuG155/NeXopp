// Ruta: app/src/main/java/com/nexopp/ui/ToolbarPresetsPopup.kt
package com.nexopp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun PresetsPopupButton(
    presets: List<ToolPreset>,
    onPresets: (List<ToolPreset>) -> Unit,
    onActivate: (ToolPreset) -> Unit,
    onCapture: (String) -> ToolPreset,
) {
    var newName by remember { mutableStateOf("") }
    ToolbarPopupButton(
        icon = Icons.Filled.Bookmark,
        contentDescription = "Preajustes",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    ) { dismiss ->
        MenuHeading(if (presets.isEmpty()) "Sin preajustes guardados" else "Preajustes")
        presets.forEachIndexed { i, preset ->
            PresetRow(
                preset = preset,
                slot = i + 1,
                canMoveUp = i > 0,
                canMoveDown = i < presets.lastIndex,
                onActivate = { onActivate(preset); dismiss() },
                onOverwrite = {
                    onPresets(overwriteToolPreset(presets, preset.id, onCapture(preset.name)))
                },
                onMove = { delta -> onPresets(moveToolPreset(presets, i, delta)) },
                onDelete = { onPresets(removeToolPreset(presets, preset.id)) },
            )
        }
        SavePresetRow(
            name = newName,
            onName = { newName = it },
            onSave = {
                val name = newName.trim().ifEmpty { "Preajuste ${presets.size + 1}" }
                onPresets(addToolPreset(presets, onCapture(name)))
                newName = ""
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetRow(
    preset: ToolPreset,
    slot: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onActivate: () -> Unit,
    onOverwrite: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onActivate, onLongClick = onOverwrite)
                .padding(vertical = 8.dp)
                .width(180.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PresetSwatch(preset)
            Spacer(Modifier.width(12.dp))
            Text(
                "$slot.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(preset.name, style = MaterialTheme.typography.bodyMedium)
        }
        ReorderControls(
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            itemName = preset.name,
            onMove = onMove,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun PresetSwatch(preset: ToolPreset) {
    WidthDot(preset.widthPt, PEN_WIDTH_MAX, Color(preset.colorArgb), bordered = true)
}

@Composable
private fun SavePresetRow(name: String, onName: (String) -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            singleLine = true,
            label = { Text("Guardar herramienta actual como…") },
            modifier = Modifier.width(200.dp),
        )
        IconButton(onClick = onSave) {
            Icon(Icons.Filled.Add, contentDescription = "Guardar preajuste")
        }
    }
}