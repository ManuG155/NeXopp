package com.nexopp.stem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexopp.format.model.Element

val STEM_PLOT_COLORS = listOf(
    0xFF1976D2.toInt(), // Blue
    0xFFD32F2F.toInt(), // Red
    0xFF388E3C.toInt(), // Green
    0xFF7B1FA2.toInt(), // Purple
    0xFFF57C00.toInt(), // Orange
    0xFF000000.toInt(), // Black
)

val PRESET_FORMULAS = listOf(
    "sin(x)" to "Seno",
    "cos(x)" to "Coseno",
    "x^2" to "Parábola",
    "x^3 - 3*x" to "Cúbica",
    "1/x" to "Hipérbola",
    "exp(-x^2)" to "Gaussiana",
    "sqrt(abs(x))" to "Raíz",
    "sin(x)/x" to "Sinc",
)

@Composable
fun FunctionPlotterDialog(
    originX: Double,
    originY: Double,
    onDismiss: () -> Unit,
    onInsertPlot: (List<Element>) -> Unit
) {
    var formula by remember { mutableStateOf("sin(x)") }
    var xMinStr by remember { mutableStateOf("-5") }
    var xMaxStr by remember { mutableStateOf("5") }
    var yMinStr by remember { mutableStateOf("-3") }
    var yMaxStr by remember { mutableStateOf("3") }
    var selectedColor by remember { mutableStateOf(STEM_PLOT_COLORS.first()) }
    var drawAxes by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Graficador de Funciones STEM", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Formula input
                OutlinedTextField(
                    value = formula,
                    onValueChange = { formula = it },
                    label = { Text("Función f(x)") },
                    prefix = { Text("f(x) = ", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Math Symbol Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("x", "^2", "+", "-", "*", "/", "sin(", "cos(", "tan(", "sqrt(", "exp(", "ln(", "abs(", "pi").forEach { symbol ->
                        AssistChip(
                            onClick = { formula += symbol },
                            label = { Text(symbol, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Preset formulas
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Preajustes comunes", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PRESET_FORMULAS.forEach { (presetFormula, name) ->
                            FilterChip(
                                selected = formula == presetFormula,
                                onClick = { formula = presetFormula },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Range Bounds Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = xMinStr,
                        onValueChange = { xMinStr = it },
                        label = { Text("X min") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = xMaxStr,
                        onValueChange = { xMaxStr = it },
                        label = { Text("X max") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yMinStr,
                        onValueChange = { yMinStr = it },
                        label = { Text("Y min") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yMaxStr,
                        onValueChange = { yMaxStr = it },
                        label = { Text("Y max") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Curve Color & Axes Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Color swatches
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        STEM_PLOT_COLORS.forEach { colorVal ->
                            val isSelected = selectedColor == colorVal
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable { selectedColor = colorVal }
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Draw axes toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = drawAxes,
                            onCheckedChange = { drawAxes = it }
                        )
                        Text("Ejes", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val xMin = xMinStr.toDoubleOrNull() ?: -5.0
                    val xMax = xMaxStr.toDoubleOrNull() ?: 5.0
                    val yMin = yMinStr.toDoubleOrNull() ?: -5.0
                    val yMax = yMaxStr.toDoubleOrNull() ?: 5.0

                    val elements = FunctionPlotter.generatePlotElements(
                        formula = formula,
                        originX = originX,
                        originY = originY,
                        plotWidthPt = 280.0,
                        plotHeightPt = 200.0,
                        xMin = xMin,
                        xMax = xMax,
                        yMin = yMin,
                        yMax = yMax,
                        strokeColor = selectedColor,
                        strokeWidthPt = 2.0f,
                        drawAxes = drawAxes
                    )
                    onInsertPlot(elements)
                    onDismiss()
                },
                enabled = formula.isNotBlank()
            ) {
                Text("Insertar en Página")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
