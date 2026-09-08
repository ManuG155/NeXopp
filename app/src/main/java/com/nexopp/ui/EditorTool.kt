// --- EditorTool.kt ---
package com.nexopp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.vector.ImageVector
import com.nexopp.format.model.Tool

enum class EditorTool {
    PEN, HIGHLIGHTER, ERASER, ERASER_WHOLE, HAND, SELECT, LASSO_SELECT, TEXT_SELECT, BG_SELECT,
    TEXT, IMAGE, TEXIMAGE,
    LINE, ARROW, DOUBLE_ARROW, COORDINATE_AXIS, RECTANGLE, ELLIPSE, SPLINE, VERTICAL_SPACE,
    PLAY_OBJECT,
}

val SHAPE_TOOLS: List<EditorTool> = listOf(
    EditorTool.LINE, EditorTool.ARROW, EditorTool.DOUBLE_ARROW, EditorTool.COORDINATE_AXIS,
    EditorTool.RECTANGLE, EditorTool.ELLIPSE, EditorTool.SPLINE,
)

private data class ToolInfo(val tool: EditorTool, val label: String, val icon: ImageVector)

private val TOOLS: List<ToolInfo> = listOf(
    ToolInfo(EditorTool.PEN, "Pluma", Icons.Filled.Create),
    ToolInfo(EditorTool.HIGHLIGHTER, "Subrayador", Icons.Filled.Brush),
    ToolInfo(EditorTool.ERASER, "Borrador (parcial)", Icons.Filled.Delete),
    ToolInfo(EditorTool.ERASER_WHOLE, "Borrador (trazo completo)", Icons.Filled.DeleteSweep),
    ToolInfo(EditorTool.LINE, "Línea", Icons.Filled.HorizontalRule),
    ToolInfo(EditorTool.ARROW, "Flecha", Icons.Filled.ArrowRightAlt),
    ToolInfo(EditorTool.DOUBLE_ARROW, "Flecha doble", Icons.Filled.SwapHoriz),
    ToolInfo(EditorTool.COORDINATE_AXIS, "Ejes de coordenadas", Icons.Filled.ShowChart),
    ToolInfo(EditorTool.RECTANGLE, "Rectángulo", Icons.Filled.Rectangle),
    ToolInfo(EditorTool.ELLIPSE, "Elipse", Icons.Filled.RadioButtonUnchecked),
    ToolInfo(EditorTool.SPLINE, "Curva (Spline)", Icons.Filled.Gesture),
    ToolInfo(EditorTool.HAND, "Mano (desplazar)", Icons.Filled.PanTool),
    ToolInfo(EditorTool.SELECT, "Selección rectangular", Icons.Filled.HighlightAlt),
    ToolInfo(EditorTool.LASSO_SELECT, "Selección de lazo", Icons.Filled.Polyline),
    ToolInfo(EditorTool.TEXT_SELECT, "Seleccionar texto (PDF)", Icons.Filled.SelectAll),
    ToolInfo(EditorTool.BG_SELECT, "Seleccionar fondo (acoplar)", Icons.Filled.Crop),
    ToolInfo(EditorTool.TEXT, "Texto", Icons.Filled.TextFields),
    ToolInfo(EditorTool.IMAGE, "Imagen", Icons.Filled.Image),
    ToolInfo(EditorTool.TEXIMAGE, "LaTeX", Icons.Filled.Functions),
    ToolInfo(EditorTool.VERTICAL_SPACE, "Espacio vertical", Icons.Filled.SwapVert),
    ToolInfo(EditorTool.PLAY_OBJECT, "Reproducir objeto", Icons.Filled.PlayCircleOutline),
)

val EditorTool.label: String get() = TOOLS.first { it.tool == this }.label

val EditorTool.icon: ImageVector get() = TOOLS.first { it.tool == this }.icon

val DEFAULT_TOOL_CHOICES: List<EditorTool> =
    listOf(EditorTool.PEN, EditorTool.HIGHLIGHTER, EditorTool.ERASER, EditorTool.HAND)