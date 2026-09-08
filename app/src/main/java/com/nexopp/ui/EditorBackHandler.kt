// --- EditorBackHandler.kt ---
package com.nexopp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.nexopp.render.cancelSpline
import com.nexopp.render.clearBackgroundRegion
import com.nexopp.render.clearSelection
import com.nexopp.render.closePalette
import com.nexopp.render.splineInProgress

@Composable
fun EditorBackHandler(
    ui: EditorUiState,
    pane: PaneState,
    busy: Boolean,
    onExit: () -> Unit,
) {
    BackHandler(enabled = true) {
        if (busy) return@BackHandler
        val surface = pane.surface

        when {
            surface != null && surface.paletteOpen -> surface.closePalette()
            surface != null && surface.splineInProgress() -> surface.cancelSpline()
            pane.hasTextSelection -> surface?.cancelTextEdit()
            pane.hasSelection -> surface?.clearSelection()
            pane.hasBackgroundRegion -> surface?.clearBackgroundRegion()
            pane.selectedPages > 0 -> surface?.clearPageSelection()
            pane.pagesEditMode -> surface?.setPagesEditMode(false)
            ui.fullPage -> ui.fullPage = false
            else -> onExit()
        }
    }
}