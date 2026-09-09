// Ruta: app/src/main/java/com/nexopp/stem/TableDialog.kt
package com.nexopp.stem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
    var colWidth by remember { mutableStateOf(80.0) }
    var rowHeight by remember { mutableStateOf(26.0) }
    val cellValues = remember { mutableStateMapOf<Pair<Int, Int>, String>() }
    var selectedColor by remember { mutableStateOf(0xFF000000.toInt()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Insertar Tabla",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Quick Dimensions Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Rows Controller
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Filas: $rows", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { if (rows > 1) rows-- },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.Filled.Remove, contentDescription = "Menos filas", modifier = Modifier.size(16.dp)) }
                            IconButton(
                                onClick = { if (rows < 20) rows++ },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.Filled.Add, contentDescription = "Más filas", modifier = Modifier.size(16.dp)) }
                        }

                        // Columns Controller
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Columnas: $cols", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { if (cols > 1) cols-- },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.Filled.Remove, contentDescription = "Menos columnas", modifier = Modifier.size(16.dp)) }
                            IconButton(
                                onClick = { if (cols < 12) cols++ },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.Filled.Add, contentDescription = "Más columnas", modifier = Modifier.size(16.dp)) }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Interactive Grid Editor
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        items(rows) { r ->
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                items(cols) { c ->
                                    val cellKey = r to c
                                    val cellText = cellValues[cellKey] ?: ""
                                    val isHeader = r == 0

                                    OutlinedTextField(
                                        value = cellText,
                                        onValueChange = { cellValues[cellKey] = it },
                                        placeholder = {
                                            Text(
                                                if (isHeader) "Encabezado" else "Dato",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = if (isHeader) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                            unfocusedContainerColor = if (isHeader) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else Color.Transparent,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(44.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(Modifier.width(12.dp))
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
                        }
                    ) {
                        Text("Insertar en Página")
                    }
                }
            }
        }
    }
}
