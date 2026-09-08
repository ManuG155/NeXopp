// Ruta: app/src/main/java/com/nexopp/ui/RadialPaletteCodec.kt
package com.nexopp.ui

fun encodeRadialPalette(palette: RadialPalette): String = listOf(
    palette.name.replace(FIELD_SEP, ' ').replace(SLOT_SEP, ' ').replace(PALETTE_SEP, ' '),
    encodeRing(palette.inner),
    encodeRing(palette.outer),
).joinToString(FIELD_SEP.toString())

fun decodeRadialPalette(raw: String?): RadialPalette? {
    if (raw.isNullOrBlank()) return null
    val fields = raw.split(FIELD_SEP)
    val name = fields.getOrNull(0)?.takeIf { it.isNotBlank() } ?: RadialPalette.DEFAULT_NAME
    return RadialPalette(
        name = name,
        inner = decodeRing(fields.getOrNull(1), RadialRing.INNER),
        outer = decodeRing(fields.getOrNull(2), RadialRing.OUTER),
    )
}

fun encodeRadialPalettes(palettes: List<RadialPalette>): String =
    palettes.joinToString(PALETTE_SEP.toString(), transform = ::encodeRadialPalette)

fun decodeRadialPalettes(raw: String?): List<RadialPalette> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(PALETTE_SEP).mapNotNull(::decodeRadialPalette)
}

private const val FIELD_SEP: Char = ';'
private const val PALETTE_SEP: Char = '|'
private const val SLOT_SEP: Char = ','

private fun encodeRing(slots: List<PaletteAction?>): String =
    slots.joinToString(SLOT_SEP.toString()) { it?.let(::encodeAction).orEmpty() }

private fun decodeRing(raw: String?, ring: RadialRing): List<PaletteAction?> {
    val parsed = raw.orEmpty().split(SLOT_SEP).map { decodeAction(it.trim()) }
    return List(ring.slotCount) { parsed.getOrNull(it) }
}

private fun encodeAction(action: PaletteAction): String = when (action) {
    is PaletteAction.SelectTool -> "tool:${action.tool.name}"
    is PaletteAction.ToggleTool -> "toggle:${action.tool.name}"
    is PaletteAction.SetColor -> "color:${action.argb}"
    is PaletteAction.SetWidth -> "width:${action.widthPt}"
    PaletteAction.Undo -> "undo"
    PaletteAction.Redo -> "redo"
    PaletteAction.ToggleFullPage -> "fullpage"
    is PaletteAction.Page -> "page:${action.op.name}"
    is PaletteAction.ApplyPreset -> "preset:${action.presetId}"
    is PaletteAction.ApplyPresetSlot -> "presetslot:${action.index}"
    is PaletteAction.SwitchPalette -> "palette:${action.paletteName}"
}

private fun decodeAction(token: String): PaletteAction? {
    if (token.isEmpty()) return null
    val kind = token.substringBefore(':')
    val arg = token.substringAfter(':', missingDelimiterValue = "")
    return when (kind) {
        "tool" -> enumOrNull<EditorTool>(arg)?.let(PaletteAction::SelectTool)
        "toggle" -> enumOrNull<EditorTool>(arg)?.let(PaletteAction::ToggleTool)
        "color" -> arg.toIntOrNull()?.let(PaletteAction::SetColor)
        "width" -> arg.toFloatOrNull()?.let { PaletteAction.SetWidth(it.coerceIn(PEN_WIDTH_MIN, PEN_WIDTH_MAX)) }
        "undo" -> PaletteAction.Undo
        "redo" -> PaletteAction.Redo
        "fullpage" -> PaletteAction.ToggleFullPage
        "page" -> enumOrNull<PalettePageOp>(arg)?.let(PaletteAction::Page)
        "preset" -> arg.takeIf { it.isNotEmpty() }?.let(PaletteAction::ApplyPreset)
        "presetslot" -> arg.toIntOrNull()?.takeIf { it >= 0 }?.let(PaletteAction::ApplyPresetSlot)
        "palette" -> arg.takeIf { it.isNotEmpty() }?.let(PaletteAction::SwitchPalette)
        else -> null
    }
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
    runCatching { enumValueOf<E>(name) }.getOrNull()