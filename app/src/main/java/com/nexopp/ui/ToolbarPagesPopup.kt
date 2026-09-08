// Ruta: app/src/main/java/com/nexopp/ui/ToolbarPagesPopup.kt
package com.nexopp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexopp.render.PageStacker
import kotlin.math.roundToInt

@Composable
internal fun PagesPopupButton(callbacks: ToolbarPagesCallbacks) {
    var open by remember { mutableStateOf(false) }
    var sizing by remember { mutableStateOf(false) }
    ToolbarPopupButton(
        icon = Icons.Filled.Description,
        contentDescription = "Páginas",
    ) { dismiss ->
        PageNavRow(callbacks.pageCount, callbacks.currentPage, callbacks.onGoToPage, dismiss)
        DropdownMenuItem(
            text = { Text("Añadir página") },
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
            onClick = { callbacks.onAddPage(); dismiss() },
        )
        DropdownMenuItem(
            text = { Text("Eliminar página") },
            leadingIcon = { Icon(Icons.Filled.Remove, contentDescription = null) },
            enabled = callbacks.pageCount > 1,
            onClick = { callbacks.onRemovePage(); dismiss() },
        )
        PageClipboardItems(
            pageCount = callbacks.pageCount,
            selectedPages = callbacks.selectedPages,
            copiedPages = callbacks.copiedPages,
            onCopySelectedPages = { callbacks.onCopySelectedPages(); dismiss() },
            onDeleteSelectedPages = { callbacks.onDeleteSelectedPages(); dismiss() },
            onClearPageSelection = { callbacks.onClearPageSelection(); dismiss() },
            onPastePages = { callbacks.onPastePages(); dismiss() },
        )
        HorizontalDivider()
        PagesPerRowRow(callbacks.pageColumns, callbacks.onPageColumns)
        OverviewModeRow(callbacks.pageColumns, callbacks.pagesEditMode, callbacks.onPagesEditMode)
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Tamaño de página…") },
            leadingIcon = { Icon(Icons.Filled.AspectRatio, contentDescription = null) },
            trailingIcon = { callbacks.pageSize?.let { Text(pageSizeLabel(it.first, it.second)) } },
            enabled = callbacks.pageSize != null,
            onClick = { sizing = true; dismiss() },
        )
    }
    if (sizing && callbacks.pageSize != null) {
        PageSizeDialog(
            initialWidthPt = callbacks.pageSize.first,
            initialHeightPt = callbacks.pageSize.second,
            onConfirm = { w, h -> callbacks.onPageSize(w, h); sizing = false },
            onDismiss = { sizing = false },
        )
    }
}

@Composable
private fun PageNavRow(pageCount: Int, currentPage: Int, onGoToPage: (Int) -> Unit, dismiss: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onGoToPage(currentPage - 1); dismiss() },
            enabled = currentPage > 0,
        ) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Página anterior") }
        Text(
            "Página " + pageLabel(currentPage, pageCount),
            style = MaterialTheme.typography.bodyMedium,
        )
        IconButton(
            onClick = { onGoToPage(currentPage + 1); dismiss() },
            enabled = currentPage < pageCount - 1,
        ) { Icon(Icons.Filled.ChevronRight, contentDescription = "Página siguiente") }
    }
}

@Composable
private fun PageClipboardItems(
    pageCount: Int,
    selectedPages: Int,
    copiedPages: Int,
    onCopySelectedPages: () -> Unit,
    onDeleteSelectedPages: () -> Unit,
    onClearPageSelection: () -> Unit,
    onPastePages: () -> Unit,
) {
    if (selectedPages > 0) {
        DropdownMenuItem(
            text = { Text(if (selectedPages == 1) "Copiar 1 seleccionada" else "Copiar $selectedPages seleccionadas") },
            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
            onClick = onCopySelectedPages,
        )
        DropdownMenuItem(
            text = { Text(if (selectedPages == 1) "Eliminar 1 seleccionada" else "Eliminar $selectedPages seleccionadas") },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            enabled = selectedPages < pageCount,
            onClick = onDeleteSelectedPages,
        )
        DropdownMenuItem(
            text = { Text("Limpiar selección") },
            leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
            onClick = onClearPageSelection,
        )
    }
    if (copiedPages > 0) {
        DropdownMenuItem(
            text = { Text(if (copiedPages == 1) "Pegar página" else "Pegar $copiedPages páginas") },
            leadingIcon = { Icon(Icons.Filled.ContentPaste, contentDescription = null) },
            onClick = onPastePages,
        )
    }
}

private data class PagePreset(val name: String, val widthPt: Double, val heightPt: Double)

private val PAGE_PRESETS: List<PagePreset> = listOf(
    PagePreset("A4", 595.276, 841.89),
    PagePreset("A5", 419.528, 595.276),
    PagePreset("Letter", 612.0, 792.0),
    PagePreset("Legal", 612.0, 1008.0),
)

private enum class SizeUnit(val label: String, val perPt: Double) {
    MM("mm", 25.4 / 72.0),
    IN("in", 1.0 / 72.0),
    PT("pt", 1.0);

    fun fromPt(pt: Double): Double = pt * perPt
    fun toPt(value: Double): Double = value / perPt
}

private fun fmtDim(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}

private fun pageSizeLabel(widthPt: Double, heightPt: Double): String {
    fun near(a: Double, b: Double) = kotlin.math.abs(a - b) <= 1.0
    PAGE_PRESETS.firstOrNull {
        (near(it.widthPt, widthPt) && near(it.heightPt, heightPt)) ||
            (near(it.widthPt, heightPt) && near(it.heightPt, widthPt))
    }?.let { return it.name }
    return "${fmtDim(widthPt * SizeUnit.MM.perPt)}×${fmtDim(heightPt * SizeUnit.MM.perPt)} mm"
}

@Composable
private fun PageSizeDialog(
    initialWidthPt: Double,
    initialHeightPt: Double,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var unit by remember { mutableStateOf(SizeUnit.MM) }
    var widthPt by remember { mutableStateOf(initialWidthPt) }
    var heightPt by remember { mutableStateOf(initialHeightPt) }
    var widthText by remember { mutableStateOf(fmtDim(unit.fromPt(initialWidthPt))) }
    var heightText by remember { mutableStateOf(fmtDim(unit.fromPt(initialHeightPt))) }
    fun resync() {
        widthText = fmtDim(unit.fromPt(widthPt))
        heightText = fmtDim(unit.fromPt(heightPt))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tamaño de página") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (preset in PAGE_PRESETS) {
                        TextButton(onClick = {
                            widthPt = preset.widthPt; heightPt = preset.heightPt; resync()
                        }) { Text(preset.name) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Unidades:", style = MaterialTheme.typography.labelMedium)
                    for (u in SizeUnit.entries) {
                        TextButton(onClick = { if (u != unit) { unit = u; resync() } }) {
                            Text(
                                u.label,
                                fontWeight = if (u == unit) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { entered ->
                            widthText = entered
                            entered.toDoubleOrNull()?.let { widthPt = unit.toPt(it) }
                        },
                        label = { Text("Ancho") },
                        suffix = { Text(unit.label) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(120.dp),
                    )
                    IconButton(onClick = {
                        val w = widthPt; widthPt = heightPt; heightPt = w; resync()
                    }) { Icon(Icons.Filled.SwapHoriz, contentDescription = "Intercambiar ancho y alto") }
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { entered ->
                            heightText = entered
                            entered.toDoubleOrNull()?.let { heightPt = unit.toPt(it) }
                        },
                        label = { Text("Alto") },
                        suffix = { Text(unit.label) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(widthPt, heightPt) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagesPerRowRow(pageColumns: Int, onPageColumns: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Páginas por fila", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageStacker.COLUMN_CHOICES.forEach { n ->
                FilterChip(
                    selected = n == pageColumns,
                    onClick = { onPageColumns(n) },
                    label = { Text("$n") },
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewModeRow(pageColumns: Int, editMode: Boolean, onEditMode: (Boolean) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Modo de vista general", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = !editMode,
                enabled = pageColumns > 1,
                onClick = { onEditMode(false) },
                label = { Text("Ver") },
                modifier = Modifier.padding(end = 4.dp),
            )
            FilterChip(
                selected = editMode,
                enabled = pageColumns > 1,
                onClick = { onEditMode(true) },
                label = { Text("Editar") },
            )
        }
    }
}