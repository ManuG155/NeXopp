package com.nexopp.stem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificCalculatorDialog(
    onDismiss: () -> Unit,
    onInsertText: (String) -> Unit
) {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var isRadMode by remember { mutableStateOf(false) } // False = DEG, True = RAD
    var history by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }

    fun calculate() {
        if (expression.isBlank()) return
        try {
            val evaluated = evaluateExpression(expression, isRadMode)
            resultText = if (evaluated.isNaN() || evaluated.isInfinite()) {
                "Error"
            } else {
                if (abs(evaluated - evaluated.toLong()) < 1e-10) {
                    evaluated.toLong().toString()
                } else {
                    String.format(Locale.US, "%.8f", evaluated).trimEnd('0').trimEnd('.')
                }
            }
            if (resultText != "Error") {
                history = listOf(Pair(expression, resultText)) + history.take(15)
            }
        } catch (e: Exception) {
            resultText = "Error"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Calculadora Científica STEM", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHistory = !showHistory }) {
                            Icon(Icons.Filled.History, contentDescription = "Historial", tint = if (showHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        Button(
                            onClick = {
                                val textToInsert = if (resultText != "Error" && resultText != "0") {
                                    "$expression = $resultText"
                                } else {
                                    expression
                                }
                                onInsertText(textToInsert)
                                onDismiss()
                            },
                            enabled = expression.isNotBlank()
                        ) {
                            Icon(Icons.Filled.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Insertar en Nota")
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                )

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Left Column: Calculator Screen & Keypad
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Display Screen
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rad/Deg Mode Indicator Chip
                                    AssistChip(
                                        onClick = { isRadMode = !isRadMode },
                                        label = { Text(if (isRadMode) "RAD" else "DEG", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )

                                    // Expression input display
                                    Text(
                                        text = expression.ifBlank { "0" },
                                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        textAlign = TextAlign.End
                                    )
                                }

                                // Main Result Display
                                Text(
                                    text = resultText,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Scientific Keypad
                        val keypadButtons = listOf(
                            listOf("sin", "cos", "tan", "ln", "log", "C", "⌫"),
                            listOf("asin", "acos", "atan", "e", "π", "(", ")"),
                            listOf("x²", "xʸ", "√", "7", "8", "9", "÷"),
                            listOf("1/x", "abs", "%", "4", "5", "6", "×"),
                            listOf("n!", "exp", "±", "1", "2", "3", "-"),
                            listOf("ANS", "0", ".", "=" , "+")
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            keypadButtons.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { key ->
                                        val isEquals = key == "="
                                        val isOp = key in listOf("+", "-", "×", "÷", "=")
                                        val isClear = key in listOf("C", "⌫")
                                        val isFunc = key.length > 1 && key !in listOf("ANS")

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = when {
                                                isEquals -> MaterialTheme.colorScheme.primary
                                                isOp -> MaterialTheme.colorScheme.primaryContainer
                                                isClear -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                                isFunc -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> MaterialTheme.colorScheme.surface
                                            },
                                            contentColor = when {
                                                isEquals -> MaterialTheme.colorScheme.onPrimary
                                                isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isClear -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            tonalElevation = 2.dp,
                                            modifier = Modifier
                                                .weight(if (isEquals || key == "0") 1.2f else 1f)
                                                .fillMaxHeight()
                                                .clickable {
                                                    when (key) {
                                                        "C" -> {
                                                            expression = ""
                                                            resultText = "0"
                                                        }
                                                        "⌫" -> {
                                                            if (expression.isNotEmpty()) expression = expression.dropLast(1)
                                                        }
                                                        "=" -> calculate()
                                                        "×" -> expression += "*"
                                                        "÷" -> expression += "/"
                                                        "x²" -> expression += "^2"
                                                        "xʸ" -> expression += "^"
                                                        "√" -> expression += "sqrt("
                                                        "1/x" -> expression += "^(-1)"
                                                        "sin" -> expression += "sin("
                                                        "cos" -> expression += "cos("
                                                        "tan" -> expression += "tan("
                                                        "asin" -> expression += "asin("
                                                        "acos" -> expression += "acos("
                                                        "atan" -> expression += "atan("
                                                        "ln" -> expression += "ln("
                                                        "log" -> expression += "log10("
                                                        "abs" -> expression += "abs("
                                                        "exp" -> expression += "exp("
                                                        "π" -> expression += "pi"
                                                        "e" -> expression += "e"
                                                        "ANS" -> if (resultText != "Error") expression += resultText
                                                        "±" -> expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression"
                                                        else -> expression += key
                                                    }
                                                }
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = key,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Optional Calculation History
                    if (showHistory) {
                        Spacer(Modifier.width(16.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .width(240.dp)
                                .fillMaxHeight()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Historial", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    if (history.isNotEmpty()) {
                                        TextButton(onClick = { history = emptyList() }) {
                                            Text("Borrar", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                                if (history.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Sin cálculos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(history) { (expr, res) ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expression = expr
                                                        resultText = res
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(expr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("= $res", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pure Kotlin mathematical evaluator supporting trigonometric, exponential, power, and log operations.
 */
internal fun evaluateExpression(raw: String, isRad: Boolean): Double {
    val sanitized = raw.lowercase().replace(" ", "")
    return FunctionPlotter.evaluate(sanitized, 0.0, isRad)
}

