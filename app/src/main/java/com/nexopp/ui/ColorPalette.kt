// --- ColorPalette.kt ---
package com.nexopp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Stable
class ColorPaletteState(
    private val settings: AppSettings,
    private val onSettingsChange: (AppSettings) -> Unit,
) {
    val custom: Int get() = settings.customColor
    val recents: List<Int> get() = settings.recentColors

    fun note(color: Int, asPen: Boolean = false) = onSettingsChange(settings.withColorUsed(color, asPen))
    fun redefineCustom(color: Int) = onSettingsChange(settings.copy(customColor = color))
}

@Composable
fun rememberColorPaletteState(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
): ColorPaletteState = remember(settings, onSettingsChange) {
    ColorPaletteState(settings, onSettingsChange)
}

@Composable
fun ColorPaletteRows(
    selected: Int?,
    palette: ColorPaletteState,
    onPick: (Int) -> Unit,
    onEditCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PaletteHint("Toca para elegir · mantén pulsado ✎ para editar")
        SwatchRow {
            for (c in PEN_COLORS) {
                ColorSwatch(color = c, selected = c == selected, onClick = { onPick(c) })
            }
            ColorSwatch(
                color = palette.custom,
                selected = palette.custom == selected,
                onClick = { onPick(palette.custom) },
                onLongClick = onEditCustom,
                editable = true,
            )
        }
        if (palette.recents.isNotEmpty()) {
            PaletteHint("Recientes")
            SwatchRow {
                for (c in palette.recents) {
                    ColorSwatch(color = c, selected = c == selected, onClick = { onPick(c) })
                }
            }
        }
    }
}

@Composable
fun CustomColorEditor(
    visible: Boolean,
    palette: ColorPaletteState,
    onDismiss: () -> Unit,
    onRedefine: (Int) -> Unit = { palette.redefineCustom(it) },
) {
    if (!visible) return
    CustomColorPickerDialog(
        initial = palette.custom,
        onConfirm = { newColor -> onRedefine(newColor); onDismiss() },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun PaletteHint(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}