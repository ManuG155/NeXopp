// Ruta: app/src/main/java/com/nexopp/stem/TableDialog.kt
package com.nexopp.stem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nexopp.format.model.Element
import com.nexopp.render.TableBuilder
import com.nexopp.ui.CleanColorSwatch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDialog(
    originX: Double = 60.0,
    originY: Double = 100.0,
    onDismiss: () -> Unit,
    onInsertTable: (List<Element>) -> Unit
) {
    var rows by remember { mutableStateOf(3) }
    var cols by remember { mutableStateOf(3) }
    var colWidth by remember { mutableStateOf(110.0) }
    var rowHeight by remember { mutableStateOf(32.0) }
    val cellValues = remember { mutableStateMapOf<Pair<Int, Int>, String>() }
    var selectedColor by remember { mutableStateOf(0xFF000000.toInt()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header with Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Insertar Tabla",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Quick Dimensions Controls (Rows & Cols)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Rows Controller
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Filas: $rows", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick = { if (rows > 1) rows-- },
                                    modifier = Modifier.size(36.dp)
                                ) { Icon(Icons.Filled.Remove, contentDescription = "Menos filas", modifier = Modifier.size(20.dp)) }
                                IconButton(
                                    onClick = { if (rows < 20) rows++ },
                                    modifier = Modifier.size(36.dp)
                                ) { Icon(Icons.Filled.Add, contentDescription = "Más filas", modifier = Modifier.size(20.dp)) }
                            }
                        }

                        // Columns Controller
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Columnas: $cols", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick = { if (cols > 1) cols-- },
                                    modifier = Modifier.size(36.dp)
                                ) { Icon(Icons.Filled.Remove, contentDescription = "Menos columnas", modifier = Modifier.size(20.dp)) }
                                IconButton(
                                    onClick = { if (cols < 12) cols++ },
                                    modifier = Modifier.size(36.dp)
                                ) { Icon(Icons.Filled.Add, contentDescription = "Más columnas", modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Interactive Spacious Grid Editor
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    val hScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .horizontalScroll(hScroll)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(rows) { r ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (c in 0 until cols) {
                                        val cellKey = r to c
                                        val cellText = cellValues[cellKey] ?: ""
                                        val isHeader = r == 0

                                        OutlinedTextField(
                                            value = cellText,
                                            onValueChange = { cellValues[cellKey] = it },
                                            placeholder = {
                                                Text(
                                                    if (isHeader) "Encabezado ${c + 1}" else "Dato ($r, $c)",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = if (isHeader) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = if (isHeader) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .width(160.dp)
                                                .height(52.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom: Color & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Color bordes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        listOf(
                            0xFF000000.toInt(),
                            0xFF1E88E5.toInt(),
                            0xFF43A047.toInt(),
                            0xFFE53935.toInt(),
                            0xFF8E24AA.toInt()
                        ).forEach { colorInt ->
                            CleanColorSwatch(
                                color = colorInt,
                                selected = selectedColor == colorInt,
                                onClick = { selectedColor = colorInt },
                                size = 32.dp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Cancelar", fontSize = 14.sp)
                        }
                        Button(
                            onClick = {
                                val spec = TableBuilder.TableSpec(
                                    rows = rows,
                                    cols = cols,
                                    x = originX,
                                    y = originY,
                                    colWidth = colWidth,
                                    rowHeight = rowHeight,
                                    borderColor = selectedColor,
                                    cellTexts = cellValues.toMap()
                                )
                                val tableElements = TableBuilder.build(spec)
                                onInsertTable(tableElements)
                                onDismiss()
                            },
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Insertar en Página", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
