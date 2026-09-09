package com.nexopp.document.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*

enum class StructureTab {
    TOC,
    BOOKMARKS,
    LINKS
}

/**
 * Tablet modal dialog for Table of Contents (Índice), Named Bookmarks, and Internal Links.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentStructureDialog(
    initialStructure: DocumentStructure,
    totalPages: Int,
    currentPageIndex: Int,
    onDismiss: () -> Unit,
    onSaveStructure: (DocumentStructure) -> Unit,
    onNavigateToPage: (pageIndex: Int) -> Unit,
    onInsertLinkElement: (label: String, targetPageIndex: Int) -> Unit
) {
    var activeTab by remember { mutableStateOf(StructureTab.TOC) }
    var structure by remember { mutableStateOf(initialStructure) }

    // Dialog state for adding/editing TOC entry
    var showAddTocDialog by remember { mutableStateOf(false) }
    var editingTocEntry by remember { mutableStateOf<TocEntry?>(null) }
    var tocTitleInput by remember { mutableStateOf("") }
    var tocPageInput by remember { mutableStateOf((currentPageIndex + 1).toString()) }
    var tocLevelInput by remember { mutableStateOf(0) }

    // Dialog state for adding/editing Named Bookmark
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<NamedBookmark?>(null) }
    var bookmarkTitleInput by remember { mutableStateOf("") }
    var bookmarkPageInput by remember { mutableStateOf((currentPageIndex + 1).toString()) }

    // Dialog state for creating Internal Link
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var linkLabelInput by remember { mutableStateOf("") }
    var linkTargetPageInput by remember { mutableStateOf((currentPageIndex + 1).toString()) }

    fun updateAndSave(newStructure: DocumentStructure) {
        structure = newStructure
        onSaveStructure(newStructure)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .widthIn(min = 550.dp, max = 700.dp)
                .heightIn(min = 480.dp, max = 680.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (activeTab) {
                                        StructureTab.TOC -> Icons.Filled.FormatListNumbered
                                        StructureTab.BOOKMARKS -> Icons.Filled.Bookmark
                                        StructureTab.LINKS -> Icons.Filled.Link
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Estructura del Cuaderno",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Índice, Marcadores nombrados y Enlaces internos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeTab == StructureTab.TOC,
                        onClick = { activeTab = StructureTab.TOC },
                        label = { Text("Índice (${structure.tocEntries.size})") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = activeTab == StructureTab.BOOKMARKS,
                        onClick = { activeTab = StructureTab.BOOKMARKS },
                        label = { Text("Marcadores (${structure.namedBookmarks.size})") },
                        leadingIcon = { Icon(Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = activeTab == StructureTab.LINKS,
                        onClick = { activeTab = StructureTab.LINKS },
                        label = { Text("Enlaces (${structure.internalLinks.size})") },
                        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Action Bar for the active tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (activeTab) {
                            StructureTab.TOC -> "Contenidos y Secciones:"
                            StructureTab.BOOKMARKS -> "Páginas Importantes Marcadas:"
                            StructureTab.LINKS -> "Enlaces creados en el documento:"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = {
                            when (activeTab) {
                                StructureTab.TOC -> {
                                    editingTocEntry = null
                                    tocTitleInput = ""
                                    tocPageInput = (currentPageIndex + 1).toString()
                                    tocLevelInput = 0
                                    showAddTocDialog = true
                                }
                                StructureTab.BOOKMARKS -> {
                                    editingBookmark = null
                                    bookmarkTitleInput = ""
                                    bookmarkPageInput = (currentPageIndex + 1).toString()
                                    showAddBookmarkDialog = true
                                }
                                StructureTab.LINKS -> {
                                    linkLabelInput = ""
                                    linkTargetPageInput = (currentPageIndex + 1).toString()
                                    showAddLinkDialog = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (activeTab) {
                                StructureTab.TOC -> "Nueva Entrada"
                                StructureTab.BOOKMARKS -> "Nuevo Marcador"
                                StructureTab.LINKS -> "Insertar Enlace"
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Content list per Tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeTab) {
                        // --- 1. TOC Tab ---
                        StructureTab.TOC -> {
                            if (structure.tocEntries.isEmpty()) {
                                EmptyState(
                                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    message = "El cuaderno aún no tiene entradas en el índice.\nCrea secciones o temas para saltar rápidamente."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(structure.tocEntries) { index, entry ->
                                        val indent = (entry.level * 20).dp
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = indent)
                                                .clickable {
                                                    onNavigateToPage(entry.pageIndex)
                                                    onDismiss()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "${entry.pageIndex + 1}",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = entry.title,
                                                        style = if (entry.level == 0) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = {
                                                        editingTocEntry = entry
                                                        tocTitleInput = entry.title
                                                        tocPageInput = (entry.pageIndex + 1).toString()
                                                        tocLevelInput = entry.level
                                                        showAddTocDialog = true
                                                    }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                                    }

                                                    IconButton(onClick = {
                                                        val updated = structure.tocEntries.filter { it.id != entry.id }
                                                        updateAndSave(structure.copy(tocEntries = updated))
                                                    }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- 2. BOOKMARKS Tab ---
                        StructureTab.BOOKMARKS -> {
                            if (structure.namedBookmarks.isEmpty()) {
                                EmptyState(
                                    icon = Icons.Filled.BookmarkAdd,
                                    message = "No hay marcadores nombrados en este cuaderno.\nAñade notas clave para identificarlas al instante."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(structure.namedBookmarks) { bm ->
                                        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(bm.timestamp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onNavigateToPage(bm.pageIndex)
                                                    onDismiss()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Bookmark,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFB300),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = bm.title.ifBlank { "Marcador Página ${bm.pageIndex + 1}" },
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "Página ${bm.pageIndex + 1} • $dateStr",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                IconButton(onClick = {
                                                    val updated = structure.namedBookmarks.filter { it.id != bm.id }
                                                    updateAndSave(structure.copy(namedBookmarks = updated))
                                                }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- 3. LINKS Tab ---
                        StructureTab.LINKS -> {
                            if (structure.internalLinks.isEmpty()) {
                                EmptyState(
                                    icon = Icons.Filled.AddLink,
                                    message = "No se han creado enlaces en el documento.\nPuedes insertar botones que salten a demostraciones o temas."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(structure.internalLinks) { link ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onNavigateToPage(link.targetPageIndex)
                                                    onDismiss()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Link,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = link.label,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "Enlace a página ${link.targetPageIndex + 1}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                IconButton(onClick = {
                                                    val updated = structure.internalLinks.filter { it.id != link.id }
                                                    updateAndSave(structure.copy(internalLinks = updated))
                                                }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    // Modal: Add / Edit TOC Entry
    if (showAddTocDialog) {
        AlertDialog(
            onDismissRequest = { showAddTocDialog = false },
            title = { Text(if (editingTocEntry != null) "Editar Entrada del Índice" else "Añadir al Índice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tocTitleInput,
                        onValueChange = { tocTitleInput = it },
                        label = { Text("Título de la sección o tema") },
                        placeholder = { Text("Ej: Álgebra Lineal, Teorema de Stokes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tocPageInput,
                            onValueChange = { tocPageInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Página de inicio") },
                            placeholder = { Text("1 - $totalPages") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nivel:", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = tocLevelInput == 0,
                                    onClick = { tocLevelInput = 0 },
                                    label = { Text("Tema") }
                                )
                                FilterChip(
                                    selected = tocLevelInput == 1,
                                    onClick = { tocLevelInput = 1 },
                                    label = { Text("Subtema") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pageNum = (tocPageInput.toIntOrNull() ?: 1).coerceIn(1, maxOf(1, totalPages))
                        val pageIdx = pageNum - 1
                        if (tocTitleInput.isNotBlank()) {
                            if (editingTocEntry != null) {
                                val updated = structure.tocEntries.map {
                                    if (it.id == editingTocEntry!!.id) it.copy(title = tocTitleInput.trim(), pageIndex = pageIdx, level = tocLevelInput) else it
                                }
                                updateAndSave(structure.copy(tocEntries = updated))
                            } else {
                                val newEntry = TocEntry(
                                    title = tocTitleInput.trim(),
                                    pageIndex = pageIdx,
                                    level = tocLevelInput,
                                    order = structure.tocEntries.size
                                )
                                updateAndSave(structure.copy(tocEntries = structure.tocEntries + newEntry))
                            }
                        }
                        showAddTocDialog = false
                    },
                    enabled = tocTitleInput.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTocDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Add / Edit Named Bookmark
    if (showAddBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text(if (editingBookmark != null) "Editar Marcador" else "Nuevo Marcador Nombrado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bookmarkTitleInput,
                        onValueChange = { bookmarkTitleInput = it },
                        label = { Text("Nombre / Descripción del marcador") },
                        placeholder = { Text("Ej: Demostración Clave, Dudas para examen") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bookmarkPageInput,
                        onValueChange = { bookmarkPageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Página objetivo") },
                        placeholder = { Text("1 - $totalPages") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pageNum = (bookmarkPageInput.toIntOrNull() ?: 1).coerceIn(1, maxOf(1, totalPages))
                        val pageIdx = pageNum - 1
                        val newBm = NamedBookmark(
                            title = bookmarkTitleInput.trim().ifBlank { "Marcador Página ${pageIdx + 1}" },
                            pageIndex = pageIdx
                        )
                        val updated = structure.namedBookmarks.filter { it.pageIndex != pageIdx } + newBm
                        updateAndSave(structure.copy(namedBookmarks = updated))
                        showAddBookmarkDialog = false
                    }
                ) {
                    Text("Guardar Marcador")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Add Internal Link
    if (showAddLinkDialog) {
        AlertDialog(
            onDismissRequest = { showAddLinkDialog = false },
            title = { Text("Insertar Enlace en la Página") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = linkLabelInput,
                        onValueChange = { linkLabelInput = it },
                        label = { Text("Texto del enlace") },
                        placeholder = { Text("Ej: Ver Demostración → Pág. 27") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = linkTargetPageInput,
                        onValueChange = { linkTargetPageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Página destino al pulsar") },
                        placeholder = { Text("1 - $totalPages") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pageNum = (linkTargetPageInput.toIntOrNull() ?: 1).coerceIn(1, maxOf(1, totalPages))
                        val pageIdx = pageNum - 1
                        val label = linkLabelInput.trim().ifBlank { "Ir a Página ${pageIdx + 1}" }
                        onInsertLinkElement(label, pageIdx)
                        val newLink = InternalLink(
                            label = label,
                            targetPageIndex = pageIdx,
                            sourcePageIndex = currentPageIndex
                        )
                        updateAndSave(structure.copy(internalLinks = structure.internalLinks + newLink))
                        showAddLinkDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Insertar en Página")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLinkDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
