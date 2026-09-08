package com.nexopp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexopp.io.ExportManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    currentPageNo: Int,
    totalPageCount: Int,
    defaultTitle: String,
    onDismiss: () -> Unit,
    onShare: (format: ExportManager.ExportFormat, pageIndices: List<Int>, scale: Float) -> Unit,
    onSaveToStorage: (format: ExportManager.ExportFormat, filename: String, pageIndices: List<Int>, scale: Float) -> Unit
) {
    val context = LocalContext.current
    val exportMgr = remember(context) { ExportManager(context) }

    var selectedFormat by remember { mutableStateOf(ExportManager.ExportFormat.PDF) }
    var pageScope by remember { mutableStateOf(ExportManager.PageScope.ALL) }
    var customRangeText by remember { mutableStateOf("1-$totalPageCount") }
    var selectedScale by remember { mutableStateOf(2.0f) } // 2x default for high quality

    val maxPages = totalPageCount.coerceAtLeast(1)
    val parsedIndices = remember(pageScope, customRangeText, currentPageNo, maxPages) {
        when (pageScope) {
            ExportManager.PageScope.ALL -> (0 until maxPages).toList()
            ExportManager.PageScope.CURRENT -> listOf(currentPageNo.coerceIn(0, maxPages - 1))
            ExportManager.PageScope.RANGE -> exportMgr.parsePageRange(customRangeText, maxPages)
        }
    }

    val isValidRange = parsedIndices.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Exportar y Compartir", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Format Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Formato de exportación", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedFormat == ExportManager.ExportFormat.PDF,
                            onClick = { selectedFormat = ExportManager.ExportFormat.PDF },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        ) {
                            Text("PDF Vector")
                        }
                        SegmentedButton(
                            selected = selectedFormat == ExportManager.ExportFormat.PNG,
                            onClick = { selectedFormat = ExportManager.ExportFormat.PNG },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            icon = { Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        ) {
                            Text("PNG")
                        }
                        SegmentedButton(
                            selected = selectedFormat == ExportManager.ExportFormat.JPEG,
                            onClick = { selectedFormat = ExportManager.ExportFormat.JPEG },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            icon = { Icon(Icons.Filled.Photo, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        ) {
                            Text("JPEG")
                        }
                    }
                }

                // 2. Page Range Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Páginas a exportar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = pageScope == ExportManager.PageScope.ALL,
                                onClick = { pageScope = ExportManager.PageScope.ALL }
                            )
                            Text("Documento completo ($totalPageCount páginas)", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = pageScope == ExportManager.PageScope.CURRENT,
                                onClick = { pageScope = ExportManager.PageScope.CURRENT }
                            )
                            Text("Solo la página actual (Página ${currentPageNo + 1})", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = pageScope == ExportManager.PageScope.RANGE,
                                onClick = { pageScope = ExportManager.PageScope.RANGE }
                            )
                            Text("Rango personalizado:", style = MaterialTheme.typography.bodyMedium)
                        }

                        if (pageScope == ExportManager.PageScope.RANGE) {
                            OutlinedTextField(
                                value = customRangeText,
                                onValueChange = { customRangeText = it },
                                placeholder = { Text("Ej. 1-3, 5") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                supportingText = {
                                    Text(
                                        if (isValidRange) "${parsedIndices.size} páginas seleccionadas (${parsedIndices.map { it + 1 }.joinToString(", ")})"
                                        else "Rango no válido",
                                        color = if (isValidRange) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }

                // 3. Image Resolution / Quality (Only for PNG / JPEG)
                if (selectedFormat != ExportManager.ExportFormat.PDF) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Resolución de imagen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedScale == 1.0f,
                                onClick = { selectedScale = 1.0f },
                                label = { Text("1x Normal") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedScale == 2.0f,
                                onClick = { selectedScale = 2.0f },
                                label = { Text("2x HD (Recomendado)") },
                                modifier = Modifier.weight(1.3f)
                            )
                            FilterChip(
                                selected = selectedScale == 3.0f,
                                onClick = { selectedScale = 3.0f },
                                label = { Text("3x Ultra 300 DPI") },
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val ext = when (selectedFormat) {
                            ExportManager.ExportFormat.PDF -> "pdf"
                            ExportManager.ExportFormat.PNG -> "png"
                            ExportManager.ExportFormat.JPEG -> "jpg"
                        }
                        val filename = "${defaultTitle.substringBeforeLast('.')}.$ext"
                        onSaveToStorage(selectedFormat, filename, parsedIndices, selectedScale)
                    },
                    enabled = isValidRange
                ) {
                    Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar Archivo")
                }

                Button(
                    onClick = {
                        onShare(selectedFormat, parsedIndices, selectedScale)
                    },
                    enabled = isValidRange
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
