// Ruta: app/src/main/java/com/nexopp/ui/ToolGroups.kt
package com.nexopp.ui

data class ToolGroup(
    val id: String,
    val label: String,
    val tools: List<EditorTool>,
)

val TOOL_GROUPS: List<ToolGroup> = listOf(
    ToolGroup("nav", "Navegación", listOf(EditorTool.HAND)),
    ToolGroup("draw", "Dibujar", listOf(EditorTool.PEN, EditorTool.HIGHLIGHTER)),
    ToolGroup("eraser", "Borrador", listOf(EditorTool.ERASER, EditorTool.ERASER_WHOLE)),
    ToolGroup("select", "Seleccionar", listOf(EditorTool.SELECT, EditorTool.LASSO_SELECT, EditorTool.TEXT_SELECT, EditorTool.BG_SELECT)),
    ToolGroup("shapes", "Formas", listOf(EditorTool.LINE, EditorTool.ARROW, EditorTool.DOUBLE_ARROW, EditorTool.RECTANGLE, EditorTool.ELLIPSE, EditorTool.SPLINE, EditorTool.COORDINATE_AXIS)),
    ToolGroup("insert", "Insertar", listOf(EditorTool.TEXT, EditorTool.TEXIMAGE, EditorTool.IMAGE)),
    ToolGroup("vspace", "Espacio", listOf(EditorTool.VERTICAL_SPACE)),
    ToolGroup("play", "Audio", listOf(EditorTool.PLAY_OBJECT))
)

fun groupOf(tool: EditorTool): ToolGroup? = TOOL_GROUPS.firstOrNull { tool in it.tools }

fun startingTool(defaultTool: EditorTool, selections: Map<String, EditorTool>): EditorTool =
    groupOf(defaultTool)?.selected(selections) ?: defaultTool

fun ToolGroup.selected(selections: Map<String, EditorTool>): EditorTool =
    selections[id]?.takeIf { it in tools } ?: tools.first()

fun ToolGroup.withSelection(
    selections: Map<String, EditorTool>,
    tool: EditorTool,
): Map<String, EditorTool> =
    if (tool in tools) selections + (id to tool) else selections

fun encodeToolGroupSelections(selections: Map<String, EditorTool>): String =
    selections.entries.joinToString(",") { "${it.key}:${it.value.name}" }

fun decodeToolGroupSelections(raw: String?): Map<String, EditorTool> {
    if (raw.isNullOrBlank()) return emptyMap()
    return raw.split(',').mapNotNull { entry ->
        val (id, name) = entry.split(':', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        val group = TOOL_GROUPS.firstOrNull { it.id == id.trim() } ?: return@mapNotNull null
        val tool = runCatching { enumValueOf<EditorTool>(name.trim()) }.getOrNull() ?: return@mapNotNull null
        if (tool in group.tools) group.id to tool else null
    }.toMap()
}