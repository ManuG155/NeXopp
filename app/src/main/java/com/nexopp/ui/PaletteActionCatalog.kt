// --- PaletteActionCatalog.kt ---
package com.nexopp.ui

data class PaletteActionChoice(val label: String, val action: PaletteAction)

data class PaletteActionGroup(val title: String, val choices: List<PaletteActionChoice>)

fun paletteActionGroups(
    presets: List<ToolPreset> = emptyList(),
    palettes: List<RadialPalette> = emptyList(),
): List<PaletteActionGroup> = listOfNotNull(
    presets.takeIf { it.isNotEmpty() }?.let { saved ->
        PaletteActionGroup(
            "Preajuste",
            saved.mapIndexed { i, preset ->
                PaletteActionChoice("${i + 1}. ${preset.name}", PaletteAction.ApplyPreset(preset.id))
            },
        )
    },
    presets.takeIf { it.isNotEmpty() }?.let { saved ->
        PaletteActionGroup(
            "Espacio de preajuste",
            saved.mapIndexed { i, preset ->
                PaletteActionChoice("Preajuste ${i + 1}: ${preset.name}", PaletteAction.ApplyPresetSlot(i))
            },
        )
    },
    palettes.takeIf { it.size > 1 }?.let { saved ->
        PaletteActionGroup(
            "Cambiar paleta",
            saved.map { PaletteActionChoice(it.name, PaletteAction.SwitchPalette(it.name)) },
        )
    },
    PaletteActionGroup(
        "Seleccionar herramienta",
        EditorTool.entries.map { PaletteActionChoice(it.label, PaletteAction.SelectTool(it)) },
    ),
    PaletteActionGroup(
        "Alternar herramienta",
        EditorTool.entries.map { PaletteActionChoice(it.label, PaletteAction.ToggleTool(it)) },
    ),
    PaletteActionGroup(
        "Edición",
        listOf(
            PaletteActionChoice("Deshacer", PaletteAction.Undo),
            PaletteActionChoice("Rehacer", PaletteAction.Redo),
            PaletteActionChoice("Alternar pantalla completa", PaletteAction.ToggleFullPage),
        ),
    ),
    PaletteActionGroup(
        "Página",
        PalettePageOp.entries.map { PaletteActionChoice(it.label, PaletteAction.Page(it)) },
    ),
)

val PalettePageOp.label: String
    get() = when (this) {
        PalettePageOp.NEW_AFTER -> "Nueva página después"
        PalettePageOp.NEW_BEFORE -> "Nueva página antes"
        PalettePageOp.DUPLICATE -> "Duplicar página"
        PalettePageOp.DELETE -> "Eliminar página"
        PalettePageOp.NEXT -> "Página siguiente"
        PalettePageOp.PREVIOUS -> "Página anterior"
    }

fun PaletteAction.describeAction(presets: List<ToolPreset> = emptyList()): String = when (this) {
    is PaletteAction.SelectTool -> "Seleccionar ${tool.label.lowercase()}"
    is PaletteAction.ToggleTool -> "Alternar ${tool.label.lowercase()}"
    is PaletteAction.SetColor -> "Color #%06X".format(argb and 0xFFFFFF)
    is PaletteAction.SetWidth -> "Grosor ${ptLabel(widthPt)} pt"
    PaletteAction.Undo -> "Deshacer"
    PaletteAction.Redo -> "Rehacer"
    PaletteAction.ToggleFullPage -> "Alternar pantalla completa"
    is PaletteAction.Page -> op.label
    is PaletteAction.ApplyPreset ->
        presets.firstOrNull { it.id == presetId }?.let { "Preajuste ${it.name}" } ?: "Preajuste (eliminado)"
    is PaletteAction.ApplyPresetSlot ->
        presets.getOrNull(index)?.let { "Preajuste ${index + 1} (${it.name})" } ?: "Preajuste ${index + 1} (vacío)"
    is PaletteAction.SwitchPalette -> "Cambiar a $paletteName"
}