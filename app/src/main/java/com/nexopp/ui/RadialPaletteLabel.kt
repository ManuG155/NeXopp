// Ruta: app/src/main/java/com/nexopp/ui/RadialPaletteLabel.kt
package com.nexopp.ui

import kotlin.math.roundToInt

data class PaletteFace(val glyph: String, val swatchArgb: Int? = null)

fun PaletteAction.face(): PaletteFace = when (this) {
    is PaletteAction.SelectTool -> PaletteFace(tool.glyph())
    is PaletteAction.ToggleTool -> PaletteFace(tool.glyph())
    is PaletteAction.SetColor -> PaletteFace("", swatchArgb = argb)
    is PaletteAction.SetWidth -> PaletteFace(widthPt.roundToInt().toString())
    PaletteAction.Undo -> PaletteFace("↶")
    PaletteAction.Redo -> PaletteFace("↷")
    PaletteAction.ToggleFullPage -> PaletteFace("⛶")
    is PaletteAction.Page -> PaletteFace(op.glyph())
    is PaletteAction.ApplyPreset -> PaletteFace("★")
    is PaletteAction.ApplyPresetSlot -> PaletteFace("★${index + 1}")
    is PaletteAction.SwitchPalette -> PaletteFace("◎")
}

private fun EditorTool.glyph(): String = when (this) {
    EditorTool.PEN -> "✎"
    EditorTool.HIGHLIGHTER -> "▨"
    EditorTool.ERASER, EditorTool.ERASER_WHOLE -> "⌫"
    EditorTool.HAND -> "✋"
    EditorTool.SELECT -> "⬭"
    EditorTool.LASSO_SELECT -> "◌"
    EditorTool.TEXT_SELECT -> "T◌"
    EditorTool.BG_SELECT -> "▤"
    EditorTool.TEXT -> "T"
    EditorTool.MATH_INK -> "∫"
    EditorTool.IMAGE -> "🖼"
    EditorTool.TEXIMAGE -> "∑"
    EditorTool.LINE -> "／"
    EditorTool.ARROW -> "→"
    EditorTool.DOUBLE_ARROW -> "↔"
    EditorTool.COORDINATE_AXIS -> "📈"
    EditorTool.RECTANGLE -> "□"
    EditorTool.ELLIPSE -> "○"
    EditorTool.SPLINE -> "∽"
    EditorTool.VERTICAL_SPACE -> "↕"
    EditorTool.PLAY_OBJECT -> "▶"
}

private fun PalettePageOp.glyph(): String = when (this) {
    PalettePageOp.NEW_AFTER -> "+▼"
    PalettePageOp.NEW_BEFORE -> "+▲"
    PalettePageOp.DUPLICATE -> "⧉"
    PalettePageOp.DELETE -> "✕"
    PalettePageOp.NEXT -> "▼"
    PalettePageOp.PREVIOUS -> "▲"
}