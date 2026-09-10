// --- EditorUiState.kt ---
package com.nexopp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nexopp.format.FontDescription
import com.nexopp.format.model.LineStyle
import com.nexopp.render.Placement

const val TEXT_SIZE_PT = 12.0
const val TEXT_SIZE_MIN = 6f
const val TEXT_SIZE_MAX = 96f

class TextDefaults {
    var family by mutableStateOf(FontDescription.DEFAULT_FAMILY)
    var bold by mutableStateOf(false)
    var italic by mutableStateOf(false)
    var size by mutableStateOf(TEXT_SIZE_PT)
    var color by mutableStateOf(PEN_COLORS.first())
}

class EditorUiState(tool: EditorTool, color: Int, width: Float) {
    var tool by mutableStateOf(tool)
    var color by mutableStateOf(color)
    var width by mutableStateOf(width)
    var lineStyle by mutableStateOf(LineStyle.PLAIN)

    var showSettings by mutableStateOf(false)
    var showSaveAs by mutableStateOf(false)
    var showImportPdf by mutableStateOf(false)

    var fullPage by mutableStateOf(false)

    var splitFraction by mutableStateOf(0.5f)

    var textPlacement by mutableStateOf<Placement?>(null)
    var texPlacement by mutableStateOf<Placement?>(null)
    var activeLinkCard by mutableStateOf<Triple<String, String, String>?>(null)
    var activeLinkCardElement by mutableStateOf<Pair<Int, com.nexopp.format.model.ImageElement>?>(null)
    var showPeriodicTableDialog by mutableStateOf(false)
    var showTableEditDialog by mutableStateOf(false)
    var showFunctionPlotterDialog by mutableStateOf(false)

    val textDefaults = TextDefaults()

    val panes = List(PANE_COUNT) { PaneState() }

    fun toggleTool(target: EditorTool) {
        if (tool == target) {
            tool = toolBeforeToggle ?: EditorTool.PEN
            toolBeforeToggle = null
        } else {
            toolBeforeToggle = tool
            tool = target
        }
    }

    private var toolBeforeToggle: EditorTool? = null

    fun pane(active: Int): PaneState = panes[active.coerceIn(panes.indices)]
}

@Composable
fun rememberEditorUiState(settings: AppSettings): EditorUiState = remember {
    EditorUiState(
        tool = startingTool(settings.defaultTool, settings.toolGroupSelections),
        color = settings.lastColor,
        width = settings.lastWidth,
    )
}