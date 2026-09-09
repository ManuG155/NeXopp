package com.nexopp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StemToolsPopupButton(
    onPlotFunctions: () -> Unit,
    onScientificCalculator: () -> Unit,
    onUnitConverter: () -> Unit,
    onTechnicalSymbols: () -> Unit,
    onPeriodicTable: () -> Unit,
    onInsertLatex: () -> Unit,
    modifier: Modifier = Modifier
) {
    ToolbarPopupButton(
        icon = Icons.Filled.Functions,
        contentDescription = "Herramientas de Matemáticas y Ciencias",
    ) { dismiss ->
        MenuHeading("Matemáticas y Ciencias")
        DropdownMenuItem(
            text = { Text("Graficador de Funciones") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { dismiss(); onPlotFunctions() }
        )

        DropdownMenuItem(
            text = { Text("Calculadora Científica") },
            leadingIcon = { Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { dismiss(); onScientificCalculator() }
        )
        DropdownMenuItem(
            text = { Text("Conversor de Unidades") },
            leadingIcon = { Icon(Icons.Filled.Transform, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { dismiss(); onUnitConverter() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Símbolos Técnicos y LaTeX") },
            leadingIcon = { Icon(Icons.Filled.Spellcheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { dismiss(); onTechnicalSymbols() }
        )
        DropdownMenuItem(
            text = { Text("Insertar Fórmula LaTeX") },
            leadingIcon = { Icon(Icons.Filled.DataObject, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { dismiss(); onInsertLatex() }
        )
        DropdownMenuItem(
            text = { Text("Tabla Periódica") },
            leadingIcon = { Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { dismiss(); onPeriodicTable() }
        )
    }
}
