// Ruta: app/src/main/java/com/nexopp/ui/ToolbarStylePopup.kt
package com.nexopp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nexopp.format.model.LineStyle

private val LINE_STYLE_LABELS: List<Pair<LineStyle, String>> = listOf(
    LineStyle.PLAIN to "Sólida",
    LineStyle.DASHED to "A trazos",
    LineStyle.DASH_DOT to "Trazo y punto",
    LineStyle.DOTTED to "Punteada",
)

const val DEFAULT_FILL_ALPHA: Int = 128

@Composable
internal fun StylePopupButton(
    lineStyle: LineStyle,
    onLineStyle: (LineStyle) -> Unit,
    fill: Int?,
    onFill: (Int?) -> Unit,
) {
    ToolbarPopupButton(
        icon = Icons.Filled.Timeline,
        contentDescription = "Estilo de línea y relleno",
    ) { dismiss ->
        MenuHeading("Estilo de línea")
        for ((style, label) in LINE_STYLE_LABELS) {
            DropdownMenuItem(
                text = { Text(label) },
                leadingIcon = { if (style == lineStyle) Icon(Icons.Filled.Check, contentDescription = "seleccionado") },
                onClick = { onLineStyle(style); dismiss() },
            )
        }
        MenuHeading("Relleno")
        FillControls(fill, onFill)
    }
}

@Composable
internal fun ShapeRecognitionButton(enabled: Boolean, onEnabled: (Boolean) -> Unit) {
    val tint =
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                if (enabled) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else Modifier,
            )
            .clickable { onEnabled(!enabled) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.ChangeHistory,
            contentDescription = if (enabled) "Reconocimiento de formas activado" else "Reconocimiento de formas desactivado",
            tint = tint,
        )
    }
}

@Composable
private fun FillControls(fill: Int?, onFill: (Int?) -> Unit) {
    var lastAlpha by remember { mutableStateOf(fill ?: DEFAULT_FILL_ALPHA) }
    val alpha = fill ?: lastAlpha
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(if (fill == null) "Desactivado" else "${alphaPercent(alpha)}%")
        Switch(
            checked = fill != null,
            onCheckedChange = { on -> onFill(if (on) lastAlpha else null) },
        )
    }
    Slider(
        value = alpha.toFloat(),
        onValueChange = { v ->
            lastAlpha = v.toInt().coerceIn(1, 255)
            onFill(lastAlpha)
        },
        valueRange = 1f..255f,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

internal fun alphaPercent(alpha: Int): Int = Math.round(alpha * 100f / 255f)