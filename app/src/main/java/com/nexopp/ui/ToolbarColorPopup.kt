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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            IconButton(onClick = open, modifier = Modifier.size(46.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(34.dp)
                ) {
                    val dotSize = (callbacks.width * 2.5f).coerceIn(8f, 24f).dp
                    // Clean solid circle of current ink color
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(Color(callbacks.color))
                            .border(
                                width = 1.dp,
                                color = if (Color(callbacks.color).luminance() < 0.2f) Color.Gray.copy(alpha = 0.6f) else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }
        },
    ) { dismiss ->
        Column(
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Color Header & Painter Palette
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Color de trazo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = { editing = true; dismiss() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.ColorLens,
                        contentDescription = "Paleta de pintor completa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Quick Color Palette Grid: Solid filled circles with clean selection ring
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PEN_COLORS.take(4).forEach { col ->
                    CleanColorSwatch(
                        color = col,
                        selected = callbacks.color == col,
                        onClick = { callbacks.onColor(col); dismiss() },
                        size = 40.dp
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PEN_COLORS.drop(4).take(4).forEach { col ->
                    CleanColorSwatch(
                        color = col,
                        selected = callbacks.color == col,
                        onClick = { callbacks.onColor(col); dismiss() },
                        size = 40.dp
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
                Text(
                    "Grosor de trazo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        String.format(java.util.Locale.US, "%.1f pt", callbacks.width),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Slider(
                value = callbacks.width,
                onValueChange = { callbacks.onWidth(it) },
                valueRange = 0.2f..20f,
                modifier = Modifier.fillMaxWidth()
            )

            // Preset Width Buttons (Large tablet targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Fino (0.8 pt)" to 0.85f, "Medio (1.5 pt)" to 1.5f, "Grueso (3.0 pt)" to 3.0f, "Extra (6.0 pt)" to 6.0f).forEach { (label, w) ->
                    val isSel = (callbacks.width - w).let { it in -0.15f..0.15f }
                    FilterChip(
                        selected = isSel,
                        onClick = { callbacks.onWidth(w) },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
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

/**
 * Clean, uniform solid-color swatch with no strange white halos or multi-ring artifacts.
 */
@Composable
fun CleanColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick)
    ) {
        // Main solid filled circle
        Box(
            modifier = Modifier
                .size(if (selected) size - 10.dp else size - 4.dp)
                .clip(CircleShape)
                .background(Color(color))
                .border(
                    width = 1.dp,
                    color = if (Color(color).luminance() < 0.25f) Color.Gray.copy(alpha = 0.5f) else Color.Transparent,
                    shape = CircleShape
                )
        )
        // Clean single high-contrast selection ring around the circle
        if (selected) {
            Box(
                modifier = Modifier
                    .size(size)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}