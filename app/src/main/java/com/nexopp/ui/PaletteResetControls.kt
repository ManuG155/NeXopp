// --- PaletteResetControls.kt ---
package com.nexopp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class PaletteBulkEdit { RESET, CLEAR }

@Composable
fun PaletteResetControls(palette: RadialPalette, onChange: (RadialPalette) -> Unit) {
    var pending by remember { mutableStateOf<PaletteBulkEdit?>(null) }
    Text(paletteFilledSummary(palette), style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier) {
        OutlinedButton(onClick = { pending = PaletteBulkEdit.RESET }) { Text("Restaurar por defecto") }
        OutlinedButton(
            onClick = { pending = PaletteBulkEdit.CLEAR },
            enabled = !palette.isEmpty,
        ) { Text("Limpiar todas las ranuras") }
    }
    pending?.let { edit ->
        ConfirmDialog(
            title = if (edit == PaletteBulkEdit.RESET) "¿Restaurar paleta?" else "¿Limpiar todas las ranuras?",
            text = paletteBulkEditPrompt(edit),
            confirmLabel = if (edit == PaletteBulkEdit.RESET) "Restaurar" else "Limpiar",
            onConfirm = {
                onChange(if (edit == PaletteBulkEdit.RESET) RadialPalette.default().copy(name = palette.name) else palette.cleared())
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
}

internal fun paletteFilledSummary(palette: RadialPalette): String {
    val total = RadialRing.INNER.slotCount + RadialRing.OUTER.slotCount
    return if (palette.isEmpty) "Ninguna ranura asignada — la paleta se abrirá vacía."
    else "${palette.filledCount} de $total ranuras asignadas."
}

internal fun paletteBulkEditPrompt(edit: PaletteBulkEdit): String = when (edit) {
    PaletteBulkEdit.RESET ->
        "Esto reemplaza cada ranura con las herramientas y colores por defecto. Se perderán las asignaciones actuales."
    PaletteBulkEdit.CLEAR ->
        "Esto vacía las 24 ranuras. La paleta se abrirá sin nada hasta que vuelvas a asignar ranuras."
}