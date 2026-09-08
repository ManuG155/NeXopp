// Ruta: app/src/main/java/com/nexopp/library/LibraryScreen.kt
package com.nexopp.library

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryFilter { ALL, RECENT, FAVORITES }
enum class LibraryViewMode { GRID, LIST }

val COVER_COLORS = listOf(
    0xFF1E3A8A, // Navy
    0xFF065F46, // Emerald
    0xFF831843, // Wine
    0xFF7C2D12, // Amber/Rust
    0xFF374151, // Slate
    0xFF4C1D95, // Deep Purple
    0xFF155E75, // Deep Cyan
    0xFF0F172A, // Midnight Black
)

val TEMPLATE_OPTIONS = listOf(
    "ruled" to "Rayado",
    "graph" to "Cuadriculado",
    "dotted" to "Puntos",
    "plain" to "Liso",
    "millimeter" to "Milimetrado",
    "isometric" to "Isométrico",
    "polar" to "Polar",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    store: LibraryStore,
    onOpenNotebook: (Notebook) -> Unit,
    onSettings: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    BackHandler { activity?.finish() }

    var subjects by remember { mutableStateOf(store.loadSubjects()) }
    var notebooks by remember { mutableStateOf(store.loadNotebooks()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID) }

    // Dialog states
    var showCreateNotebookDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var renamingNotebook by remember { mutableStateOf<Notebook?>(null) }
    var deletingNotebook by remember { mutableStateOf<Notebook?>(null) }
    var deletingSubject by remember { mutableStateOf<Subject?>(null) }

    fun refresh() {
        subjects = store.loadSubjects()
        notebooks = store.loadNotebooks()
    }

    // Filtered notebooks logic
    val filteredNotebooks = remember(notebooks, searchQuery, selectedFilter, selectedSubjectId) {
        notebooks.filter { nb ->
            val matchesQuery = searchQuery.isBlank() || 
                nb.name.contains(searchQuery, ignoreCase = true) ||
                subjects.find { it.id == nb.subjectId }?.name?.contains(searchQuery, ignoreCase = true) == true

            val matchesFilter = when (selectedFilter) {
                LibraryFilter.ALL -> true
                LibraryFilter.RECENT -> true // Sorted by lastModified
                LibraryFilter.FAVORITES -> nb.isFavorite
            }

            val matchesSubject = selectedSubjectId == null || nb.subjectId == selectedSubjectId

            matchesQuery && matchesFilter && matchesSubject
        }.let { list ->
            if (selectedFilter == LibraryFilter.RECENT) list.sortedByDescending { it.lastModified } else list
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "NeXopp",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Search bar input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar cuadernos…", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                .width(260.dp)
                                .height(44.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        // Toggle Grid / List view
                        IconButton(onClick = {
                            viewMode = if (viewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
                        }) {
                            Icon(
                                if (viewMode == LibraryViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "Cambiar vista"
                            )
                        }

                        IconButton(onClick = { showTrashDialog = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Papelera")
                        }

                        IconButton(onClick = onSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                        }
                    }
                )

                // Sub-header: Navigation Chips & Subjects row
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter Pills
                        item {
                            FilterChip(
                                selected = selectedFilter == LibraryFilter.ALL && selectedSubjectId == null,
                                onClick = { selectedFilter = LibraryFilter.ALL; selectedSubjectId = null },
                                label = { Text("Todos") },
                                leadingIcon = { Icon(Icons.Filled.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == LibraryFilter.RECENT && selectedSubjectId == null,
                                onClick = { selectedFilter = LibraryFilter.RECENT; selectedSubjectId = null },
                                label = { Text("Recientes") },
                                leadingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == LibraryFilter.FAVORITES,
                                onClick = { selectedFilter = LibraryFilter.FAVORITES; selectedSubjectId = null },
                                label = { Text("Favoritos") },
                                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp)) }
                            )
                        }

                        item {
                            VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                        }

                        // Subject chips
                        items(subjects) { subject ->
                            val isSelected = selectedSubjectId == subject.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSubjectId = if (isSelected) null else subject.id
                                    selectedFilter = LibraryFilter.ALL
                                },
                                label = { Text(subject.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(subject.color))
                                    )
                                },
                                trailingIcon = {
                                    val count = notebooks.count { it.subjectId == subject.id }
                                    Text(
                                        "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }

                        // Add Subject Button
                        item {
                            AssistChip(
                                onClick = { showSubjectDialog = true },
                                label = { Text("Nueva Asignatura") },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateNotebookDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo Cuaderno", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (filteredNotebooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            if (searchQuery.isNotBlank()) "No se encontraron cuadernos" else "No hay cuadernos en esta sección",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { showCreateNotebookDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crear Cuaderno")
                        }
                    }
                }
            } else if (viewMode == LibraryViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 190.dp),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotebooks, key = { it.id }) { notebook ->
                        val subject = subjects.find { it.id == notebook.subjectId }
                        NotebookGridCard(
                            notebook = notebook,
                            subject = subject,
                            onOpen = { onOpenNotebook(notebook) },
                            onToggleFavorite = {
                                store.toggleFavorite(notebook.id)
                                refresh()
                            },
                            onRename = { renamingNotebook = notebook },
                            onDuplicate = {
                                store.duplicateNotebook(notebook)
                                refresh()
                            },
                            onDelete = { deletingNotebook = notebook }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotebooks, key = { it.id }) { notebook ->
                        val subject = subjects.find { it.id == notebook.subjectId }
                        NotebookListRow(
                            notebook = notebook,
                            subject = subject,
                            onOpen = { onOpenNotebook(notebook) },
                            onToggleFavorite = {
                                store.toggleFavorite(notebook.id)
                                refresh()
                            },
                            onRename = { renamingNotebook = notebook },
                            onDuplicate = {
                                store.duplicateNotebook(notebook)
                                refresh()
                            },
                            onDelete = { deletingNotebook = notebook }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Create Notebook Dialog
    if (showCreateNotebookDialog) {
        CreateNotebookDialog(
            subjects = subjects,
            defaultSubjectId = selectedSubjectId ?: subjects.firstOrNull()?.id ?: "",
            onDismiss = { showCreateNotebookDialog = false },
            onCreate = { name, subjectId, coverColor, template ->
                val fileName = "${java.util.UUID.randomUUID()}.xopp"
                val newNb = Notebook(
                    subjectId = subjectId,
                    name = name,
                    fileName = fileName,
                    coverColor = coverColor,
                    initialTemplate = template,
                    lastModified = System.currentTimeMillis()
                )
                store.addNotebook(newNb)
                refresh()
                showCreateNotebookDialog = false
                onOpenNotebook(newNb)
            }
        )
    }

    // 2. Create / Edit Subject Dialog
    if (showSubjectDialog || editingSubject != null) {
        CreateSubjectDialog(
            subjectToEdit = editingSubject,
            onDismiss = { showSubjectDialog = false; editingSubject = null },
            onSave = { name, color ->
                if (editingSubject != null) {
                    store.updateSubject(editingSubject!!.copy(name = name, color = color))
                } else {
                    store.addSubject(Subject(name = name, color = color, order = subjects.size))
                }
                refresh()
                showSubjectDialog = false
                editingSubject = null
            }
        )
    }

    // 3. Rename Notebook Dialog
    renamingNotebook?.let { nb ->
        var newTitle by remember { mutableStateOf(nb.name) }
        AlertDialog(
            onDismissRequest = { renamingNotebook = null },
            title = { Text("Renombrar Cuaderno") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Nombre del cuaderno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) {
                        store.updateNotebook(nb.copy(name = newTitle.trim()))
                        refresh()
                    }
                    renamingNotebook = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renamingNotebook = null }) { Text("Cancelar") }
            }
        )
    }

    // 4. Delete Notebook Confirmation Dialog (Moves to Trash)
    deletingNotebook?.let { nb ->
        AlertDialog(
            onDismissRequest = { deletingNotebook = null },
            title = { Text("¿Mover cuaderno a la papelera?") },
            text = { Text("Se moverá '${nb.name}' a la papelera. Podrás restaurarlo o vaciar la papelera en cualquier momento.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.deleteNotebook(nb)
                        refresh()
                        deletingNotebook = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Mover a papelera") }
            },
            dismissButton = {
                TextButton(onClick = { deletingNotebook = null }) { Text("Cancelar") }
            }
        )
    }

    // 5. Trash Dialog
    if (showTrashDialog) {
        TrashDialog(
            store = store,
            onDismiss = { showTrashDialog = false },
            onRestored = {
                refresh()
            }
        )
    }
}

@Composable
fun NotebookGridCard(
    notebook: Notebook,
    subject: Subject?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(notebook.lastModified) { dateFormat.format(Date(notebook.lastModified)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Notebook Cover Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(notebook.coverColor),
                                Color(notebook.coverColor).copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Spine line effect
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(14.dp)
                        .background(Color.Black.copy(alpha = 0.2f))
                )

                // Paper template indicator badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 22.dp, bottom = 8.dp)
                ) {
                    Text(
                        TEMPLATE_OPTIONS.find { it.first == notebook.initialTemplate }?.second ?: "Rayado",
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Favorite Star Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        if (notebook.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (notebook.isFavorite) Color(0xFFFFB300) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Notebook Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Subject tag
                    if (subject != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(subject.color).copy(alpha = 0.15f)
                        ) {
                            Text(
                                subject.name,
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = Color(subject.color),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    // Context Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renombrar") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { menuExpanded = false; onRename() }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicar") },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { menuExpanded = false; onDuplicate() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                onClick = { menuExpanded = false; onDelete() }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Title
                Text(
                    notebook.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                // Date
                Text(
                    formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NotebookListRow(
    notebook: Notebook,
    subject: Subject?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(notebook.lastModified) { dateFormat.format(Date(notebook.lastModified)) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover swatch
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(notebook.coverColor))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notebook.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subject != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(subject.color).copy(alpha = 0.15f)
                        ) {
                            Text(
                                subject.name,
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = Color(subject.color),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Favorite star
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (notebook.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (notebook.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                )
            }

            // More Options
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opciones")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Renombrar") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicar") },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = { menuExpanded = false; onDuplicate() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateNotebookDialog(
    subjects: List<Subject>,
    defaultSubjectId: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, subjectId: String, coverColor: Long, template: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(defaultSubjectId.ifBlank { subjects.firstOrNull()?.id ?: "" }) }
    var selectedCoverColor by remember { mutableStateOf(COVER_COLORS.first()) }
    var selectedTemplate by remember { mutableStateOf("ruled") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Cuaderno", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del cuaderno") },
                    placeholder = { Text("Ej. Cálculo II - Tema 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subject Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Asignatura", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects) { sub ->
                            FilterChip(
                                selected = selectedSubjectId == sub.id,
                                onClick = { selectedSubjectId = sub.id },
                                label = { Text(sub.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(sub.color))
                                    )
                                }
                            )
                        }
                    }
                }

                // Cover Color Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Color de Portada", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        COVER_COLORS.forEach { colorVal ->
                            val isSelected = selectedCoverColor == colorVal
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable { selectedCoverColor = colorVal }
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Paper Template Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Plantilla de papel", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TEMPLATE_OPTIONS) { (key, label) ->
                            FilterChip(
                                selected = selectedTemplate == key,
                                onClick = { selectedTemplate = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedSubjectId, selectedCoverColor, selectedTemplate)
                    }
                },
                enabled = name.isNotBlank() && selectedSubjectId.isNotBlank()
            ) {
                Text("Crear Cuaderno")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CreateSubjectDialog(
    subjectToEdit: Subject? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Long) -> Unit
) {
    var name by remember { mutableStateOf(subjectToEdit?.name ?: "") }
    var selectedColor by remember { mutableStateOf(subjectToEdit?.color ?: 0xFF1E88E5) }

    val subjectColors = listOf(
        0xFF1E88E5, // Blue
        0xFF8E24AA, // Purple
        0xFF00897B, // Teal
        0xFF3949AB, // Indigo
        0xFFE53935, // Red
        0xFFFB8C00, // Orange
        0xFF43A047, // Green
        0xFF546E7A, // Slate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (subjectToEdit == null) "Nueva Asignatura" else "Editar Asignatura", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la asignatura") },
                    placeholder = { Text("Ej. Álgebra Lineal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Color distintivo", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        subjectColors.forEach { colorVal ->
                            val isSelected = selectedColor == colorVal
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable { selectedColor = colorVal }
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name.trim(), selectedColor)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (subjectToEdit == null) "Crear" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun TrashDialog(
    store: LibraryStore,
    onDismiss: () -> Unit,
    onRestored: () -> Unit
) {
    var trashItems by remember { mutableStateOf(store.loadTrash()) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }

    fun refreshTrash() {
        trashItems = store.loadTrash()
        onRestored()
    }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Papelera de Reciclaje", fontWeight = FontWeight.Bold)
                if (trashItems.isNotEmpty()) {
                    TextButton(
                        onClick = { confirmEmptyTrash = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Vaciar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        text = {
            if (trashItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("La papelera está vacía", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trashItems, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(item.notebook.coverColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(item.notebook.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "Eliminado: ${dateFormatter.format(Date(item.deletedTimestamp))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = {
                                        store.restoreFromTrash(item.id)
                                        refreshTrash()
                                    }) {
                                        Icon(Icons.Filled.Restore, contentDescription = "Restaurar", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        store.permanentlyDelete(item.id)
                                        refreshTrash()
                                    }) {
                                        Icon(Icons.Filled.DeleteForever, contentDescription = "Eliminar definitivamente", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )

    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text("¿Vaciar papelera?") },
            text = { Text("Se eliminarán definitivamente todos los cuadernos en la papelera. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.emptyTrash()
                        refreshTrash()
                        confirmEmptyTrash = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Vaciar definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmptyTrash = false }) { Text("Cancelar") }
            }
        )
    }
}