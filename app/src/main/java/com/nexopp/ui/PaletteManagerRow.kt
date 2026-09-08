// --- PaletteManagerRow.kt ---
package com.nexopp.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PaletteManagerRow(
    set: PaletteSet,
    editing: Int,
    onEdit: (Int) -> Unit,
    onSet: (PaletteSet) -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        set.palettes.forEachIndexed { i, palette ->
            FilterChip(
                selected = i == editing,
                onClick = { onEdit(i) },
                label = { Text(paletteChipLabel(palette, i == set.activeIndex)) },
            )
        }
    }
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onSet(addPalette(set)); onEdit(set.palettes.size) }) {
            Icon(Icons.Filled.Add, contentDescription = "Añadir paleta")
        }
        IconButton(onClick = { renaming = true }) {
            Icon(Icons.Filled.Edit, contentDescription = "Renombrar paleta")
        }
        ReorderControls(
            canMoveUp = editing > 0,
            canMoveDown = editing < set.palettes.lastIndex,
            vertical = false,
            itemName = set.palettes.getOrNull(editing)?.name.orEmpty(),
            onMove = { delta ->
                onSet(movePalette(set, editing, delta))
                onEdit(editing + delta)
            },
            onDelete = { deleting = true },
        )
    }
    OutlinedButton(
        onClick = { onSet(activatePalette(set, editing)) },
        enabled = editing != set.activeIndex,
        modifier = Modifier.padding(top = 4.dp),
    ) { Text("Usar esta paleta en el stylus") }
    if (renaming) {
        RenameDialog(
            title = "Renombrar paleta",
            label = "Nombre",
            initialValue = set.palettes.getOrNull(editing)?.name.orEmpty(),
            onConfirm = { onSet(renamePalette(set, editing, it)); renaming = false },
            onDismiss = { renaming = false },
        )
    }
    if (deleting) {
        val name = set.palettes.getOrNull(editing)?.name.orEmpty()
        ConfirmDialog(
            title = "¿Eliminar \"$name\"?",
            text = "Se perderán sus asignaciones. Las demás paletas no se verán afectadas.",
            confirmLabel = "Eliminar",
            onConfirm = {
                onSet(removePalette(set, editing))
                onEdit((editing - 1).coerceAtLeast(0))
                deleting = false
            },
            onDismiss = { deleting = false },
        )
    }
}

internal fun paletteChipLabel(palette: RadialPalette, isActive: Boolean): String =
    if (isActive) "● ${palette.name}" else palette.name