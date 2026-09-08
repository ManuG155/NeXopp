// --- PaletteActionPicker.kt ---
package com.nexopp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteActionPickerSheet(
    slot: RadialSlot,
    current: PaletteAction?,
    palette: ColorPaletteState,
    presets: List<ToolPreset> = emptyList(),
    palettes: List<RadialPalette> = emptyList(),
    onPick: (PaletteAction?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(modifier = Modifier.padding(horizontal = 20.dp)) {
            item {
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(slot.title(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        current?.let { "Actualmente: ${it.describeAction(presets)}" } ?: "Actualmente vacía",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item { ChoiceRow("Limpiar ranura", onClick = { onPick(null) }) }
            item { PickerHeader("Color") }
            item {
                ColorPaletteRows(
                    selected = (current as? PaletteAction.SetColor)?.argb,
                    palette = palette,
                    onPick = { onPick(PaletteAction.SetColor(it)) },
                    onEditCustom = {},
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            item { PickerHeader("Grosor") }
            item {
                WidthChoice(
                    initial = (current as? PaletteAction.SetWidth)?.widthPt,
                    onPick = { onPick(PaletteAction.SetWidth(it)) },
                )
            }
            for (group in paletteActionGroups(presets, palettes)) {
                item(key = group.title) { PickerHeader(group.title) }
                items(group.choices, key = { group.title + it.label }) { choice ->
                    ChoiceRow(
                        label = choice.label,
                        selected = choice.action == current,
                        onClick = { onPick(choice.action) },
                    )
                }
            }
            item { Column(Modifier.padding(bottom = 32.dp)) {} }
        }
    }
}

private fun RadialSlot.title(): String {
    val ringName = if (ring == RadialRing.INNER) "Anillo interior" else "Anillo exterior"
    return "$ringName · ranura ${index + 1} de ${ring.slotCount}"
}

@Composable
private fun PickerHeader(text: String) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        if (selected) "✓ $label" else label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}

@Composable
private fun WidthChoice(initial: Float?, onPick: (Float) -> Unit) {
    var value by remember { mutableStateOf(initial ?: DEFAULT_PALETTE_WIDTH) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Slider(
            value = value,
            onValueChange = { value = it.coerceIn(PEN_WIDTH_MIN, PEN_WIDTH_MAX) },
            valueRange = PEN_WIDTH_MIN..PEN_WIDTH_MAX,
        )
        TextButton(onClick = { onPick(value) }) { Text("Fijar grosor en ${ptLabel(value)} pt") }
    }
}

private const val DEFAULT_PALETTE_WIDTH: Float = 1.5f