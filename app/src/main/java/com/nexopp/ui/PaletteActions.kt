// --- PaletteActions.kt ---
package com.nexopp.ui

import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.reopenPalette

fun applyPaletteAction(
    action: PaletteAction,
    ui: EditorUiState,
    surface: DrawingSurfaceView,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    when (action) {
        is PaletteAction.SelectTool -> { ui.tool = action.tool; surface.applyTool(ui.tool) }
        is PaletteAction.ToggleTool -> { ui.toggleTool(action.tool); surface.applyTool(ui.tool) }
        is PaletteAction.SetColor -> {
            ui.color = action.argb
            surface.colorArgb = action.argb
            onSettingsChange(settings.withColorUsed(action.argb))
        }
        is PaletteAction.SetWidth -> {
            ui.width = action.widthPt
            surface.baseWidthPt = action.widthPt
            onSettingsChange(settings.copy(lastWidth = action.widthPt))
        }
        PaletteAction.Undo -> surface.undo()
        PaletteAction.Redo -> surface.redo()
        PaletteAction.ToggleFullPage -> ui.fullPage = !ui.fullPage
        is PaletteAction.Page -> applyPageOp(action.op, surface)
        is PaletteAction.ApplyPreset -> settings.presets.firstOrNull { it.id == action.presetId }
            ?.let { applyToolPreset(it, ui, surface, settings, onSettingsChange) }
        is PaletteAction.ApplyPresetSlot -> settings.presets.getOrNull(action.index)
            ?.let { applyToolPreset(it, ui, surface, settings, onSettingsChange) }
        is PaletteAction.SwitchPalette -> switchPalette(action.paletteName, surface, settings, onSettingsChange)
    }
}

private fun switchPalette(
    name: String,
    surface: DrawingSurfaceView,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val index = settings.palettes.indexOfFirst { it.name == name }
    if (index < 0) return
    val updated = settings.withPalettes(activatePalette(settings.paletteSet, index))
    onSettingsChange(updated)
    surface.palette = updated.radialPalette
    surface.reopenPalette(updated.radialPalette)
}

private fun applyPageOp(op: PalettePageOp, surface: DrawingSurfaceView) {
    when (op) {
        PalettePageOp.NEW_AFTER -> surface.addPage()
        PalettePageOp.NEW_BEFORE -> surface.addPageBefore()
        PalettePageOp.DUPLICATE -> surface.duplicatePage()
        PalettePageOp.DELETE -> surface.removePage()
        PalettePageOp.NEXT -> surface.goToNextPage()
        PalettePageOp.PREVIOUS -> surface.goToPreviousPage()
    }
}