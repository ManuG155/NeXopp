package com.nexopp.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    store: LibraryStore,
    onOpenNotebook: (Notebook?) -> Unit,
    onSettings: () -> Unit
) {
    var subjects by remember { mutableStateOf(store.loadSubjects()) }
    var notebooks by remember { mutableStateOf(store.loadNotebooks()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Apuntes") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenNotebook(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (subjects.isEmpty() && notebooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes cuadernos. ¡Toca el botón + para empezar!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("Aquí irán las carpetas y cuadernos", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}