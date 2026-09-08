// Ruta: app/src/main/java/com/nexopp/library/LibraryScreen.kt
package com.nexopp.library

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    store: LibraryStore,
    onOpenNotebook: (Notebook) -> Unit,
    onSettings: () -> Unit
) {
    // Si pulsamos el botón Atrás en la biblioteca, salimos de la aplicación
    val activity = LocalContext.current as? Activity
    BackHandler { activity?.finish() }

    var subjects by remember { mutableStateOf(store.loadSubjects()) }
    var notebooks by remember { mutableStateOf(store.loadNotebooks()) }

    var showSubjectDialog by remember { mutableStateOf(false) }
    var showNotebookDialog by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }

    fun refresh() {
        subjects = store.loadSubjects()
        notebooks = store.loadNotebooks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Apuntes") },
                actions = {
                    IconButton(onClick = { showSubjectDialog = true }) {
                        Icon(Icons.Filled.Folder, contentDescription = "Nueva Asignatura")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (subjects.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes asignaturas. ¡Crea una en el icono de la carpeta!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            subjects.forEach { subject ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { showNotebookDialog = subject.id }) {
                                Icon(Icons.Filled.Add, contentDescription = "Nuevo Cuaderno")
                            }
                        }
                    }
                }

                val subjectNotebooks = notebooks.filter { it.subjectId == subject.id }
                if (subjectNotebooks.isEmpty()) {
                    item {
                        Text("   Sin cuadernos", modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(subjectNotebooks) { notebook ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenNotebook(notebook) }
                                .padding(horizontal = 32.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(16.dp))
                            Text(notebook.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider(Modifier.padding(start = 32.dp))
                    }
                }
            }
        }

        // Diálogo para Nueva Asignatura
        if (showSubjectDialog) {
            AlertDialog(
                onDismissRequest = { showSubjectDialog = false; newName = "" },
                title = { Text("Nueva Asignatura") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            val newSubject = Subject(name = newName)
                            store.save(subjects + newSubject, notebooks)
                            refresh()
                        }
                        showSubjectDialog = false
                        newName = ""
                    }) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { showSubjectDialog = false; newName = "" }) { Text("Cancelar") }
                }
            )
        }

        // Diálogo para Nuevo Cuaderno
        if (showNotebookDialog != null) {
            AlertDialog(
                onDismissRequest = { showNotebookDialog = null; newName = "" },
                title = { Text("Nuevo Cuaderno") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre del cuaderno") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            val fileName = "${java.util.UUID.randomUUID()}.xopp"
                            val newNb = Notebook(subjectId = showNotebookDialog!!, name = newName, fileName = fileName)
                            store.save(subjects, notebooks + newNb)
                            refresh()
                        }
                        showNotebookDialog = null
                        newName = ""
                    }) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { showNotebookDialog = null; newName = "" }) { Text("Cancelar") }
                }
            )
        }
    }
}