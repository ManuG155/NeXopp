// --- EditorSection.kt ---
package com.nexopp.ui

import androidx.compose.runtime.Composable

@Composable
fun EditorSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    SwitchRow(
        title = "Reconocer formas automáticamente",
        subtitle = "Convierte trazos manuscritos aproximados en líneas rectas, círculos y rectángulos geométricos precisos.",
        checked = settings.recognizeShapes,
        onCheckedChange = { onChange(settings.copy(recognizeShapes = it)) },
    )
    SwitchRow(
        title = "Ajustar a la cuadrícula",
        subtitle = "Los extremos de las formas se ajustan a las líneas del fondo de página.",
        checked = settings.snapToGrid,
        onCheckedChange = { onChange(settings.copy(snapToGrid = it)) },
    )
    SwitchRow(
        title = "Ajustar rotación",
        subtitle = "Rotar una selección lo hace en incrementos de 15°.",
        checked = settings.snapRotation,
        onCheckedChange = { onChange(settings.copy(snapRotation = it)) },
    )
    OptionGroup(
        title = "Herramienta por defecto",
        subtitle = "Qué herramienta está activa al abrir un documento.",
        options = DEFAULT_TOOL_CHOICES,
        selected = settings.defaultTool,
        label = { it.label },
        onSelect = { onChange(settings.copy(defaultTool = it)) },
    )
}