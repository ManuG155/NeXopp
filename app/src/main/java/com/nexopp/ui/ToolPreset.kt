// Ruta: app/src/main/java/com/nexopp/ui/ToolPreset.kt
package com.nexopp.ui

import com.nexopp.format.model.LineStyle
import com.nexopp.render.DrawingSurfaceView

data class ToolPreset(
    val id: String,
    val name: String,
    val tool: EditorTool,
    val colorArgb: Int,
    val widthPt: Float,
    val lineStyle: LineStyle = LineStyle.PLAIN,
    val fillEnabled: Boolean = false,
    val fillAlpha: Int = DEFAULT_FILL_ALPHA,
) {
    val currentFill: Int? get() = if (fillEnabled) fillAlpha else null

    companion object {
        fun capture(
            ui: EditorUiState,
            settings: AppSettings,
            name: String,
            id: String = slugId(name),
        ): ToolPreset = ToolPreset(
            id = id,
            name = name,
            tool = ui.tool,
            colorArgb = ui.color,
            widthPt = ui.width,
            lineStyle = ui.lineStyle,
            fillEnabled = settings.fillEnabled,
            fillAlpha = settings.fillAlpha,
        )

        fun slugId(name: String): String =
            name.lowercase().map { if (it.isLetterOrDigit()) it else '-' }
                .joinToString("").trim('-').replace(Regex("-+"), "-")
                .ifEmpty { "preajuste" }
    }
}

fun ToolPreset.applyToState(ui: EditorUiState) {
    ui.tool = tool
    ui.color = colorArgb
    ui.width = widthPt
    ui.lineStyle = lineStyle
}

fun ToolPreset.applyToSettings(settings: AppSettings): AppSettings =
    settings.withColorUsed(colorArgb)
        .copy(lastWidth = widthPt, fillEnabled = fillEnabled, fillAlpha = fillAlpha)

fun applyToolPreset(
    preset: ToolPreset,
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    preset.applyToState(ui)
    surface?.let {
        it.applyTool(preset.tool)
        it.colorArgb = preset.colorArgb
        it.baseWidthPt = preset.widthPt
        it.currentLineStyle = preset.lineStyle
        it.currentFill = preset.currentFill
    }
    onSettingsChange(preset.applyToSettings(settings))
}