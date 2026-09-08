// Ruta: app/src/main/java/com/nexopp/ui/PaletteSection.kt
package com.nexopp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaletteSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    var selected by remember { mutableStateOf<RadialSlot?>(null) }
    var picking by remember { mutableStateOf<RadialSlot?>(null) }
    var editing by remember { mutableStateOf(settings.activePaletteIndex) }
    val colors = rememberColorPaletteState(settings, onChange)
    val set = settings.paletteSet
    val index = editing.coerceIn(0, set.palettes.lastIndex)
    val palette = set.palettes[index]
    val onPalette: (RadialPalette) -> Unit = { onChange(settings.withPalettes(set.withPaletteAt(index, it))) }
    
    Text("Paletas radiales", style = MaterialTheme.typography.titleMedium)
    Text(
        "El menú que se abre al hacer doble clic con el botón del stylus en la pantalla. Elige una paleta y luego toca una ranura para asignarla.",
        style = MaterialTheme.typography.bodySmall,
    )
    PaletteManagerRow(
        set = set,
        editing = index,
        onEdit = { editing = it; selected = null },
        onSet = { onChange(settings.withPalettes(it)) },
    )
    PaletteDiagram(
        palette = palette,
        selected = selected,
        onSelect = { selected = it; picking = it },
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    )
    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    Column {
        Text("Ranura seleccionada", style = MaterialTheme.typography.bodyLarge)
        Text(selected.describe(palette, settings.presets), style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    PaletteResetControls(palette) { updated ->
        selected = null
        onPalette(updated)
    }
    picking?.let { slot ->
        PaletteActionPickerSheet(
            slot = slot,
            current = palette[slot],
            palette = colors,
            presets = settings.presets,
            palettes = set.palettes,
            onPick = { action ->
                onPalette(palette.with(slot, action))
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

internal fun RadialSlot?.describe(palette: RadialPalette, presets: List<ToolPreset> = emptyList()): String {
    if (this == null) return "Ninguna — toca una ranura en el diagrama superior."
    val ringName = if (ring == RadialRing.INNER) "Anillo interior" else "Anillo exterior"
    val holds = palette[this]?.describeAction(presets) ?: "vacía"
    return "$ringName, ranura ${index + 1} de ${ring.slotCount} — $holds."
}

@Composable
private fun PaletteDiagram(
    palette: RadialPalette,
    selected: RadialSlot?,
    onSelect: (RadialSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val current by rememberUpdatedState(palette)
    val select by rememberUpdatedState(onSelect)
    val outline = MaterialTheme.colorScheme.outline
    val filled = MaterialTheme.colorScheme.secondaryContainer
    val glyphColor = MaterialTheme.colorScheme.onSecondaryContainer
    val highlight = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier.aspectRatio(1f).pointerInput(Unit) {
            detectTapGestures { offset ->
                val edge = minOf(size.width, size.height).toFloat()
                current.editorSlotAt(offset.x, offset.y, edge)?.let(select)
            }
        },
    ) {
        val edge = minOf(size.width, size.height)
        drawRings(edge, outline)
        for (mark in palette.editorSlots(edge)) {
            drawSlotMark(mark, mark.slot == selected, measurer, outline, filled, glyphColor, highlight)
        }
    }
}

private const val PALETTE_ICON_SCALE = 1.4f

private fun DrawScope.drawRings(edge: Float, outline: Color) {
    val scale = paletteEditorScale(edge)
    val center = Offset(edge / 2f, edge / 2f)
    val geometry = RadialPaletteGeometry()
    for (r in listOf(geometry.deadZoneRadius, geometry.innerRingRadius, geometry.outerRingRadius)) {
        drawCircle(outline.copy(alpha = 0.35f), radius = r * scale, center = center, style = Stroke(1.dp.toPx()))
    }
}

private fun DrawScope.drawSlotMark(
    mark: PaletteEditorSlot,
    isSelected: Boolean,
    measurer: TextMeasurer,
    outline: Color,
    filled: Color,
    glyphColor: Color,
    highlight: Color,
) {
    val center = Offset(mark.center.x, mark.center.y)
    val face = mark.action?.face()
    if (face == null) {
        drawCircle(
            color = outline,
            radius = mark.radius,
            center = center,
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))),
        )
    } else {
        val swatch = face.swatchArgb
        drawCircle(swatch?.let { Color(it) } ?: filled, radius = mark.radius, center = center)
        val icon = mark.action?.icon()
        if (swatch == null && icon != null) {
            val size = mark.radius * PALETTE_ICON_SCALE
            withTransform({
                translate(center.x, center.y)
                scale(size, size, pivot = Offset.Zero)
            }) {
                drawPath(icon.unitOutline(), glyphColor)
            }
        } else if (swatch == null && face.glyph.isNotEmpty()) {
            val layout = measurer.measure(face.glyph, TextStyle(color = glyphColor, fontSize = 12.sp))
            drawText(
                layout,
                topLeft = Offset(
                    center.x - layout.size.width / 2f,
                    center.y - layout.size.height / 2f,
                ),
            )
        }
    }
    if (isSelected) {
        drawCircle(highlight, radius = mark.radius + 3.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
    }
}