// Ruta: app/src/main/java/com/nexopp/stem/FloatingCalculatorWidget.kt
package com.nexopp.stem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun FloatingCalculatorWidget(
    onDismiss: () -> Unit,
    onInsertResult: ((String) -> Unit)? = null
) {
    var offsetX by remember { mutableStateOf(120f) }
    var offsetY by remember { mutableStateOf(160f) }
    var widthDp by remember { mutableStateOf(280.dp) }
    var heightDp by remember { mutableStateOf(360.dp) }

    var display by remember { mutableStateOf("0") }
    var memory by remember { mutableStateOf(0.0) }
    var prevValue by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var clearOnNextDigit by remember { mutableStateOf(false) }

    val isExpanded = widthDp > 320.dp || heightDp > 420.dp

    fun inputDigit(d: String) {
        if (display == "0" || clearOnNextDigit) {
            display = d
            clearOnNextDigit = false
        } else {
            display += d
        }
    }

    fun inputDot() {
        if (clearOnNextDigit) {
            display = "0."
            clearOnNextDigit = false
        } else if (!display.contains(".")) {
            display += "."
        }
    }

    fun clear() {
        display = "0"
        prevValue = null
        pendingOp = null
        clearOnNextDigit = false
    }

    fun backspace() {
        if (display.length > 1) {
            display = display.dropLast(1)
        } else {
            display = "0"
        }
    }

    fun compute() {
        val current = display.toDoubleOrNull() ?: return
        val prev = prevValue
        val op = pendingOp
        if (prev != null && op != null) {
            val res = when (op) {
                "+" -> prev + current
                "-" -> prev - current
                "×" -> prev * current
                "÷" -> if (current != 0.0) prev / current else Double.NaN
                "^" -> prev.pow(current)
                else -> current
            }
            display = if (res.isNaN()) "Error" else if (res % 1.0 == 0.0 && abs(res) < 1e12) res.toLong().toString() else String.format(java.util.Locale.US, "%.6g", res)
            prevValue = null
            pendingOp = null
            clearOnNextDigit = true
        }
    }

    fun applyOp(op: String) {
        val current = display.toDoubleOrNull() ?: return
        if (prevValue != null && pendingOp != null) {
            compute()
        }
        prevValue = display.toDoubleOrNull()
        pendingOp = op
        clearOnNextDigit = true
    }

    fun applyScientific(fn: String) {
        val x = display.toDoubleOrNull() ?: return
        val res = when (fn) {
            "sin" -> sin(Math.toRadians(x))
            "cos" -> cos(Math.toRadians(x))
            "tan" -> tan(Math.toRadians(x))
            "ln" -> if (x > 0) ln(x) else Double.NaN
            "log" -> if (x > 0) log10(x) else Double.NaN
            "sqrt" -> if (x >= 0) sqrt(x) else Double.NaN
            "sqr" -> x * x
            "inv" -> if (x != 0.0) 1.0 / x else Double.NaN
            "pi" -> Math.PI
            "e" -> Math.E
            else -> x
        }
        display = if (res.isNaN()) "Error" else if (res % 1.0 == 0.0 && abs(res) < 1e12) res.toLong().toString() else String.format(java.util.Locale.US, "%.6g", res)
        clearOnNextDigit = true
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(widthDp, heightDp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Drag Handle Bar (Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Calculadora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onInsertResult != null) {
                        TextButton(
                            onClick = { onInsertResult(display); onDismiss() },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Pegar", fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Display Screen
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (pendingOp != null && prevValue != null) {
                        Text(
                            "${prevValue} $pendingOp",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        display,
                        fontSize = if (display.length > 10) 18.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Keypad Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (isExpanded) {
                    // Scientific Function Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CalcButton("sin", Modifier.weight(1f)) { applyScientific("sin") }
                        CalcButton("cos", Modifier.weight(1f)) { applyScientific("cos") }
                        CalcButton("tan", Modifier.weight(1f)) { applyScientific("tan") }
                        CalcButton("√", Modifier.weight(1f)) { applyScientific("sqrt") }
                        CalcButton("^", Modifier.weight(1f)) { applyOp("^") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CalcButton("ln", Modifier.weight(1f)) { applyScientific("ln") }
                        CalcButton("log", Modifier.weight(1f)) { applyScientific("log") }
                        CalcButton("x²", Modifier.weight(1f)) { applyScientific("sqr") }
                        CalcButton("π", Modifier.weight(1f)) { applyScientific("pi") }
                        CalcButton("e", Modifier.weight(1f)) { applyScientific("e") }
                    }
                }

                // Row 1: C, ⌫, %, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalcButton("C", Modifier.weight(1f), isSpecial = true) { clear() }
                    CalcButton("⌫", Modifier.weight(1f)) { backspace() }
                    CalcButton("1/x", Modifier.weight(1f)) { applyScientific("inv") }
                    CalcButton("÷", Modifier.weight(1f), isOp = true) { applyOp("÷") }
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalcButton("7", Modifier.weight(1f)) { inputDigit("7") }
                    CalcButton("8", Modifier.weight(1f)) { inputDigit("8") }
                    CalcButton("9", Modifier.weight(1f)) { inputDigit("9") }
                    CalcButton("×", Modifier.weight(1f), isOp = true) { applyOp("×") }
                }

                // Row 3: 4, 5, 6, -
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalcButton("4", Modifier.weight(1f)) { inputDigit("4") }
                    CalcButton("5", Modifier.weight(1f)) { inputDigit("5") }
                    CalcButton("6", Modifier.weight(1f)) { inputDigit("6") }
                    CalcButton("-", Modifier.weight(1f), isOp = true) { applyOp("-") }
                }

                // Row 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalcButton("1", Modifier.weight(1f)) { inputDigit("1") }
                    CalcButton("2", Modifier.weight(1f)) { inputDigit("2") }
                    CalcButton("3", Modifier.weight(1f)) { inputDigit("3") }
                    CalcButton("+", Modifier.weight(1f), isOp = true) { applyOp("+") }
                }

                // Row 5: ±, 0, ., =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalcButton("±", Modifier.weight(1f)) {
                        val v = display.toDoubleOrNull()
                        if (v != null) display = (-v).let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
                    }
                    CalcButton("0", Modifier.weight(1f)) { inputDigit("0") }
                    CalcButton(".", Modifier.weight(1f)) { inputDot() }
                    CalcButton("=", Modifier.weight(1f), isPrimary = true) { compute() }
                }
            }
        }

        // Bottom-Right Corner Resize Handle
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        widthDp = (widthDp + dragAmount.x.dp).coerceIn(240.dp, 480.dp)
                        heightDp = (heightDp + dragAmount.y.dp).coerceIn(320.dp, 600.dp)
                    }
                },
            contentAlignment = Alignment.BottomEnd
        ) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Redimensionar",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp).padding(2.dp)
            )
        }
    }
}

@Composable
private fun CalcButton(
    label: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isOp: Boolean = false,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isPrimary -> MaterialTheme.colorScheme.primary
                isOp -> MaterialTheme.colorScheme.secondaryContainer
                isSpecial -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = when {
                isPrimary -> MaterialTheme.colorScheme.onPrimary
                isOp -> MaterialTheme.colorScheme.onSecondaryContainer
                isSpecial -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        modifier = modifier.height(36.dp)
    ) {
        Text(
            label,
            fontSize = if (label.length > 2) 11.sp else 14.sp,
            fontWeight = if (isPrimary || isOp) FontWeight.Bold else FontWeight.Medium
        )
    }
}
