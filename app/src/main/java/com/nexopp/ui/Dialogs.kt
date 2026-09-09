// Ruta: app/src/main/java/com/nexopp/ui/Dialogs.kt
package com.nexopp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = false,
                label = { Text(title) },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun RenameDialog(
    title: String,
    label: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(label) },
                modifier = Modifier,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Renombrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun LatexInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    
    data class MathSymbolGlyph(val glyph: String, val latex: String)
    val mathSymbols = listOf(
        MathSymbolGlyph("α", "\\alpha "),
        MathSymbolGlyph("β", "\\beta "),
        MathSymbolGlyph("γ", "\\gamma "),
        MathSymbolGlyph("δ", "\\delta "),
        MathSymbolGlyph("θ", "\\theta "),
        MathSymbolGlyph("π", "\\pi "),
        MathSymbolGlyph("σ", "\\sigma "),
        MathSymbolGlyph("Ω", "\\Omega "),
        MathSymbolGlyph("μ", "\\mu "),
        MathSymbolGlyph("∞", "\\infty "),
        MathSymbolGlyph("≈", "\\approx "),
        MathSymbolGlyph("≠", "\\neq "),
        MathSymbolGlyph("≤", "\\le "),
        MathSymbolGlyph("≥", "\\ge "),
        MathSymbolGlyph("±", "\\pm "),
        MathSymbolGlyph("·", "\\cdot "),
        MathSymbolGlyph("×", "\\times "),
        MathSymbolGlyph("½", "\\frac{1}{2} "),
        MathSymbolGlyph("a/b", "\\frac{a}{b} "),
        MathSymbolGlyph("√x", "\\sqrt{x} "),
        MathSymbolGlyph("x²", "^{2} "),
        MathSymbolGlyph("xᵢ", "_{i} "),
        MathSymbolGlyph("v⃗", "\\vec{v} "),
        MathSymbolGlyph("∫", "\\int "),
        MathSymbolGlyph("∫ₐᵇ", "\\int_{a}^{b} "),
        MathSymbolGlyph("∑", "\\sum "),
        MathSymbolGlyph("lim", "\\lim_{x \\to 0} "),
        MathSymbolGlyph("d/dx", "\\frac{d}{dx} "),
        MathSymbolGlyph("∂", "\\partial "),
        MathSymbolGlyph("∇", "\\nabla ")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Paleta de símbolos matemáticos:", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 52.dp),
                    modifier = Modifier.heightIn(max = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(mathSymbols) { item ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { value += item.latex },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                                Text(
                                    item.glyph,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = false,
                    label = { Text("Expresión LaTeX") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(value) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}