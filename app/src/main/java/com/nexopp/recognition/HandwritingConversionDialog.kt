package com.nexopp.recognition

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog for reviewing, editing, and inserting recognized handwriting text or LaTeX formulas.
 */
@Composable
fun HandwritingConversionDialog(
    initialResult: RecognitionResult,
    onDismiss: () -> Unit,
    onReplaceStrokesWithText: (text: String, isLatex: Boolean) -> Unit,
    onInsertTextElement: (text: String, isLatex: Boolean) -> Unit
) {
    val context = LocalContext.current
    var isMathMode by remember { mutableStateOf(initialResult.latex != null) }
    var currentText by remember { mutableStateOf(if (isMathMode && initialResult.latex != null) initialResult.latex!! else initialResult.text) }
    var selectedCandidateIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .widthIn(min = 480.dp, max = 620.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMathMode) Icons.Filled.Functions else Icons.Filled.TextFields,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isMathMode) "Reconocimiento Matemático (LaTeX)" else "Reconocimiento de Escritura",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${initialResult.strokeCount} trazos analizados • Local y Privado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Mode switcher tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isMathMode,
                        onClick = {
                            isMathMode = false
                            currentText = initialResult.text
                        },
                        label = { Text("Texto Normal") },
                        leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = isMathMode,
                        onClick = {
                            isMathMode = true
                            currentText = initialResult.latex ?: initialResult.text
                        },
                        label = { Text("Fórmula LaTeX") },
                        leadingIcon = { Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Candidate suggestions row
                if (initialResult.candidates.isNotEmpty()) {
                    Text(
                        text = "Sugerencias y variantes:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(initialResult.candidates.indices.toList()) { idx ->
                            val cand = initialResult.candidates[idx]
                            val candText = if (isMathMode && cand.latex != null) cand.latex else cand.text
                            val isSelected = currentText == candText
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    selectedCandidateIndex = idx
                                    currentText = candText
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = candText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = if (isMathMode) FontFamily.Monospace else FontFamily.Default,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${(cand.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Text / LaTeX Editor field
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text(if (isMathMode) "Código LaTeX / Expresión" else "Texto Reconocido") },
                    textStyle = if (isMathMode) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 5,
                    trailingIcon = {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Handwriting", currentText))
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )

                Spacer(Modifier.height(20.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    OutlinedButton(
                        onClick = {
                            onInsertTextElement(currentText, isMathMode)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Insertar")
                    }

                    Button(
                        onClick = {
                            onReplaceStrokesWithText(currentText, isMathMode)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reemplazar Trazos")
                    }
                }
            }
        }
    }
}
