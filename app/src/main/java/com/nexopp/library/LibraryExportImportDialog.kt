package com.nexopp.library

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryExportImportDialog(
    store: LibraryStore,
    onDismiss: () -> Unit,
    onLibraryUpdated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var operationStatus by remember { mutableStateOf<String?>(null) }
    var importResult by remember { mutableStateOf<LibraryImportResult?>(null) }
    var exportResult by remember { mutableStateOf<LibraryExportResult?>(null) }

    val defaultExportName = remember {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        "FiXmy_Notes_Biblioteca_$dateStr.zip"
    }

    // SAF CreateDocument for Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            operationStatus = "Exportando biblioteca completa a ZIP..."
            importResult = null
            exportResult = null
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    LibraryZipManager.exportLibraryToUri(context, store, uri)
                }
                isProcessing = false
                exportResult = result
                if (result.success) {
                    Toast.makeText(
                        context,
                        "Biblioteca exportada: ${result.totalNotebooks} cuadernos en ${result.totalFolders} carpetas.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        result.errorMessage ?: "Error al exportar la biblioteca.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // SAF OpenDocument for Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            operationStatus = "Importando biblioteca desde ZIP..."
            importResult = null
            exportResult = null
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            LibraryZipManager.importLibrary(store, stream, context.cacheDir)
                        } ?: LibraryImportResult(
                            success = false,
                            importedNotebooksCount = 0,
                            importedFoldersCount = 0,
                            errorMessage = "No se pudo leer el archivo seleccionado."
                        )
                    } catch (e: Exception) {
                        LibraryImportResult(
                            success = false,
                            importedNotebooksCount = 0,
                            importedFoldersCount = 0,
                            errorMessage = e.localizedMessage ?: "Error al abrir el archivo ZIP."
                        )
                    }
                }
                isProcessing = false
                importResult = result
                if (result.success) {
                    onLibraryUpdated()
                    Toast.makeText(
                        context,
                        "Importación finalizada: ${result.importedNotebooksCount} cuadernos añadidos.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        result.errorMessage ?: "Error al importar el archivo ZIP.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .widthIn(min = 520.dp, max = 680.dp)
                .heightIn(max = 700.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Exportar e Importar Biblioteca",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Archivos ZIP portátiles con carpetas comprensibles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Action Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Export Card
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Exportar Biblioteca",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                "Empaqueta todos tus cuadernos .xopp organizados en sus respectivas carpetas dentro de un archivo ZIP comprensible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { exportLauncher.launch(defaultExportName) },
                                enabled = !isProcessing,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Guardar como ZIP")
                            }
                        }
                    }

                    // Import Card
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    "Importar Biblioteca",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                "Restaura o añade cuadernos y carpetas desde un archivo ZIP exportado o creado por ti con carpetas y archivos .xopp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                                enabled = !isProcessing,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Seleccionar ZIP")
                            }
                        }
                    }
                }

                // Processing indicator
                if (isProcessing) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            Text(
                                text = operationStatus ?: "Procesando archivo ZIP...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Export Result Summary
                exportResult?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (res.success) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (res.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = if (res.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    if (res.success) "Exportación completada con éxito" else "Error al exportar",
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.success) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            if (res.success) {
                                Text(
                                    "Se empaquetaron ${res.totalNotebooks} cuadernos en ${res.totalFolders} carpetas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1B5E20)
                                )
                            } else {
                                Text(
                                    res.errorMessage ?: "No se pudo generar el archivo ZIP.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Import Result Summary
                importResult?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (res.success) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (res.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = if (res.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    if (res.success) "Importación completada con éxito" else "Error al importar",
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.success) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            if (res.success) {
                                Text(
                                    "Cuadernos importados: ${res.importedNotebooksCount} • Carpetas creadas: ${res.importedFoldersCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1B5E20)
                                )
                                if (res.skippedFilesCount > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Se omitieron ${res.skippedFilesCount} archivo(s) no válidos:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        res.skippedFiles.take(5).forEach { f ->
                                            Text(
                                                "• $f",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (res.skippedFiles.size > 5) {
                                            Text(
                                                "...y ${res.skippedFiles.size - 5} más.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    res.errorMessage ?: "No se pudo leer el archivo ZIP.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Info footer
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Puedes guardar el ZIP en Descargas o Google Drive, o importar archivos ZIP con tus propios cuadernos .xopp en carpetas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
