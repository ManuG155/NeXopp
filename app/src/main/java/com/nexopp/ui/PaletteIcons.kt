// --- PaletteIcons.kt ---
package com.nexopp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.ui.graphics.vector.ImageVector

fun PaletteAction.icon(): ImageVector? = when (this) {
    is PaletteAction.SelectTool -> tool.icon
    is PaletteAction.ToggleTool -> tool.icon
    is PaletteAction.SetColor -> null
    is PaletteAction.SetWidth -> null
    PaletteAction.Undo -> Icons.Filled.Undo
    PaletteAction.Redo -> Icons.Filled.Redo
    PaletteAction.ToggleFullPage -> Icons.Filled.Fullscreen
    is PaletteAction.Page -> op.icon()
    is PaletteAction.ApplyPreset -> Icons.Filled.Bookmark
    is PaletteAction.ApplyPresetSlot -> null
    is PaletteAction.SwitchPalette -> Icons.Filled.Adjust
}

fun PaletteAction.iconTintArgb(presetColors: Map<String, Int>): Int? = when (this) {
    is PaletteAction.ApplyPreset -> presetColors[presetId]?.let { legibleOnSlot(it) }
    else -> null
}

fun legibleOnSlot(argb: Int): Int {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
    if (luminance >= MIN_LUMINANCE) return 0xFF000000.toInt() or (argb and 0x00FFFFFF)
    val t = ((MIN_LUMINANCE - luminance) / (1.0 - luminance)).coerceIn(0.0, 1.0)
    fun mix(c: Int) = (c + (255 - c) * t).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (mix(r) shl 16) or (mix(g) shl 8) or mix(b)
}

private const val MIN_LUMINANCE = 0.45

private fun PalettePageOp.icon(): ImageVector = when (this) {
    PalettePageOp.NEW_AFTER -> Icons.Filled.PostAdd
    PalettePageOp.NEW_BEFORE -> Icons.Filled.NoteAdd
    PalettePageOp.DUPLICATE -> Icons.Filled.ContentCopy
    PalettePageOp.DELETE -> Icons.Filled.DeleteForever
    PalettePageOp.NEXT -> Icons.Filled.KeyboardArrowDown
    PalettePageOp.PREVIOUS -> Icons.Filled.KeyboardArrowUp
}