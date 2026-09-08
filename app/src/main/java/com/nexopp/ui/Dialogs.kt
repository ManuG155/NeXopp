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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp

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
    
    val mathSymbols = listOf(
        "\\alpha", "\\beta", "\\pi", "\\Omega", "\\mu", "\\infty", "\\approx", "\\neq",
        "\\int", "\\sum", "\\frac{NUM}{DEN}", "\\sqrt{}", "^{2}", "_{}", "\\vec{}", "\\cdot"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Símbolos rápidos:", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    modifier = Modifier.heightIn(max = 140.dp)
                ) {
                    items(mathSymbols) { sym ->
                        Surface(
                            modifier = Modifier.padding(2.dp).clickable { value += sym + " " },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                Text(
                                    sym.replace("\\", "").replace("{NUM}{DEN}","{/}"), 
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = false,
                    label = { Text("Código LaTeX") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}