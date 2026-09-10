// Ruta: app/src/main/java/com/nexopp/ui/ToolMiniContextMenu.kt
package com.nexopp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

val DEFAULT_PEN_COLORS: List<Int> = listOf(
    0xFF000000.toInt(), // Negro
    0xFF1E88E5.toInt(), // Azul
    0xFFE53935.toInt(), // Rojo
    0xFF43A047.toInt(), // Verde
    0xFFFB8C00.toInt(), // Naranja
    0xFF8E24AA.toInt(), // Púrpura
    0xFF00ACC1.toInt(), // Cian
    0xFFFDD835.toInt(), // Amarillo
)

val DEFAULT_HIGHLIGHTER_COLORS: List<Int> = listOf(
    0xFFFFF176.toInt(), // Amarillo pastel
    0xFFAED581.toInt(), // Verde lima pastel
    0xFFF06292.toInt(), // Rosa pastel
    0xFF4FC3F7.toInt(), // Celeste pastel
    0xFFFFB74D.toInt(), // Naranja pastel
    0xFFBA68C8.toInt(), // Violeta pastel
    0xFFFF8A80.toInt(), // Coral pastel
    0xFFB0BEC5.toInt(), // Gris resaltador
)

/**
 * Mini menú contextual para Pluma, Subrayador y Borrador.
 * Permite configurar independientemente color (con paleta por defecto y gama personalizada)
 * y grosor (con slider continuo y chips de presets rápidos).
 */
@Composable
fun ToolMiniContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    tool: EditorTool,
    currentColor: Int,
    currentWidth: Float,
    palette: ColorPaletteState,
    onColorChange: (Int) -> Unit,
    onWidthChange: (Float) -> Unit,
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .widthIn(min = 280.dp, max = 320.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: Título e indicador
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (tool) {
                        EditorTool.PEN -> "Configuración de Pluma"
                        EditorTool.HIGHLIGHTER -> "Configuración de Subrayador"
                        else -> "Configuración de Borrador"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (tool == EditorTool.PEN || tool == EditorTool.HIGHLIGHTER) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(currentColor))
                            .border(
                                1.dp,
                                if (Color(currentColor).luminance() < 0.25f) Color.Gray.copy(alpha = 0.6f) else Color.Transparent,
                                CircleShape
                            )
                    )
                }
            }

            // Sección de Color (Solo para Pluma y Subrayador)
            if (tool == EditorTool.PEN || tool == EditorTool.HIGHLIGHTER) {
                val defaultColors = if (tool == EditorTool.PEN) DEFAULT_PEN_COLORS else DEFAULT_HIGHLIGHTER_COLORS

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { showCustomPicker = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.ColorLens,
                            contentDescription = "Gama de colores completa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Cuadrícula 2x4 de colores predeterminados
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    defaultColors.take(4).forEach { col ->
                        CleanColorSwatch(
                            color = col,
                            selected = currentColor == col,
                            onClick = { onColorChange(col) },
                            size = 32.dp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    defaultColors.drop(4).take(4).forEach { col ->
                        CleanColorSwatch(
                            color = col,
                            selected = currentColor == col,
                            onClick = { onColorChange(col) },
                            size = 32.dp
                        )
                    }
                }

                // Indicador de color personalizado activo si no coincide con los predeterminados
                if (currentColor !in defaultColors) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CleanColorSwatch(
                            color = currentColor,
                            selected = true,
                            onClick = {},
                            size = 26.dp
                        )
                        Text(
                            "Color personalizado activo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            }

            // Sección de Grosor (Para Pluma, Subrayador y Borrador)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Grosor",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        String.format(Locale.US, "%.1f pt", currentWidth),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            val (minW, maxW) = when (tool) {
                EditorTool.PEN -> 0.5f to 15.0f
                EditorTool.HIGHLIGHTER -> 4.0f to 40.0f
                else -> 2.0f to 30.0f // Borrador
            }

            Slider(
                value = currentWidth.coerceIn(minW, maxW),
                onValueChange = { onWidthChange(it) },
                valueRange = minW..maxW,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )

            // Chips de grosores rápidos según la herramienta
            val presets = when (tool) {
                EditorTool.PEN -> listOf(
                    "1.0" to 1.0f,
                    "1.5" to 1.5f,
                    "3.0" to 3.0f,
                    "5.0" to 5.0f,
                    "7.0" to 7.0f
                )
                EditorTool.HIGHLIGHTER -> listOf(
                    "8" to 8.0f,
                    "12" to 12.0f,
                    "16" to 16.0f,
                    "20" to 20.0f,
                    "28" to 28.0f
                )
                else -> listOf(
                    "3" to 3.0f,
                    "5" to 5.0f,
                    "10" to 10.0f,
                    "15" to 15.0f,
                    "25" to 25.0f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets.forEach { (label, w) ->
                    val isSel = abs(currentWidth - w) < 0.2f
                    FilterChip(
                        selected = isSel,
                        onClick = { onWidthChange(w) },
                        label = {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initial = currentColor,
            onConfirm = { customColor ->
                onColorChange(customColor)
                palette.redefineCustom(customColor)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}
