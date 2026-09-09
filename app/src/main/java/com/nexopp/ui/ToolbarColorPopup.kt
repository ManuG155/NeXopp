// Ruta: app/src/main/java/com/nexopp/ui/ToolbarColorPopup.kt
package com.nexopp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

val PEN_COLORS: List<Int> = listOf(
    0xFF000000.toInt(), // Black
    0xFF1E88E5.toInt(), // Blue
    0xFFE53935.toInt(), // Red
    0xFF43A047.toInt(), // Green
    0xFFFB8C00.toInt(), // Orange
    0xFF8E24AA.toInt(), // Purple
    0xFF00ACC1.toInt(), // Cyan
    0xFFFDD835.toInt(), // Yellow
)

@Composable
fun ToolbarColorPopup(styleCallbacks: ToolbarStyleCallbacks) {
    ColorSizePopupButton(callbacks = styleCallbacks)
}

@Composable
internal fun ColorSizePopupButton(callbacks: ToolbarStyleCallbacks) {
    var editing by remember { mutableStateOf(false) }

    ToolbarPopupButton(
        face = { open ->
            IconButton(onClick = open, modifier = Modifier.size(40.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    val dotSize = (callbacks.width * 2.5f).coerceIn(6f, 22f)
                    // High contrast outer ring so black ink is visible in dark theme
                    Box(
                        modifier = Modifier
                            .size((dotSize + 2f).dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize.dp)
                            .clip(CircleShape)
                            .background(Color(callbacks.color))
                    )
                }
            }
        },
    ) { dismiss ->
        Column(
            modifier = Modifier
                .width(260.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section 1: Color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Color", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                IconButton(
                    onClick = { editing = true; dismiss() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.ColorLens, contentDescription = "Personalizado", modifier = Modifier.size(18.dp))
                }
            }

            // Quick Color Palette Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PEN_COLORS.take(4).forEach { col ->
                    ColorSwatch(
                        color = col,
                        selected = callbacks.color == col,
                        onClick = { callbacks.onColor(col); dismiss() }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PEN_COLORS.drop(4).take(4).forEach { col ->
                    ColorSwatch(
                        color = col,
                        selected = callbacks.color == col,
                        onClick = { callbacks.onColor(col); dismiss() }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Section 2: Continuous Width Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Grosor", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    String.format(java.util.Locale.US, "%.1f pt", callbacks.width),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = callbacks.width,
                onValueChange = { callbacks.onWidth(it) },
                valueRange = 0.2f..20f,
                modifier = Modifier.fillMaxWidth()
            )

            // Preset Width Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Fino" to 0.85f, "Medio" to 1.5f, "Grueso" to 3.0f).forEach { (label, w) ->
                    val isSel = (callbacks.width - w).let { it in -0.1f..0.1f }
                    FilterChip(
                        selected = isSel,
                        onClick = { callbacks.onWidth(w) },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    CustomColorEditor(
        visible = editing,
        palette = callbacks.palette,
        onDismiss = { editing = false },
        onRedefine = callbacks.onRedefineCustom,
    )
}

@Composable
private fun ColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        // Outer contrast ring for black/dark colors
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
        )
        // Main swatch
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(color))
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}