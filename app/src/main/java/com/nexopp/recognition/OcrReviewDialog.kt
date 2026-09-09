package com.nexopp.recognition

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun OcrReviewDialog(
    initialText: String,
    confidence: Double = 0.9,
    isMath: Boolean = false,
    onDismiss: () -> Unit,
    onReplaceSelection: (text: String, isLatex: Boolean) -> Unit,
    onInsertAdjacent: (text: String, isLatex: Boolean) -> Unit
) {
    var editedText by remember { mutableStateOf(initialText) }
    var mathMode by remember { mutableStateOf(isMath) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (mathMode) Icons.Filled.Calculate else Icons.Filled.Spellcheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            if (mathMode) "Reconocimiento Matemático (OCR)" else "Reconocimiento de Escritura (OCR)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                // Confidence badge & Mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Confianza: ${(confidence * 100).toInt()}% (Motor Local Offline)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Formato LaTeX", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(6.dp))
                        Switch(
                            checked = mathMode,
                            onCheckedChange = { mathMode = it }
                        )
                    }
                }

                // Editor Box
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Texto / Fórmula reconocida (puedes corregir antes de aplicar)") },
                    placeholder = { Text("Texto reconocido...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 220.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = if (mathMode) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 15.sp
                    ),
                    maxLines = 6
                )

                // Informative alert
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "La sustitución es no destructiva: podrás deshacer (Ctrl+Z o botón Deshacer) para recuperar tus trazos originales en cualquier momento.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy to clipboard
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("OCR NeXopp", editedText))
                            Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (editedText.isNotBlank()) {
                                    onInsertAdjacent(editedText, mathMode)
                                }
                            },
                            enabled = editedText.isNotBlank()
                        ) {
                            Text("Insertar al lado")
                        }

                        Button(
                            onClick = {
                                if (editedText.isNotBlank()) {
                                    onReplaceSelection(editedText, mathMode)
                                }
                            },
                            enabled = editedText.isNotBlank()
                        ) {
                            Text("Reemplazar selección")
                        }
                    }
                }
            }
        }
    }
}
