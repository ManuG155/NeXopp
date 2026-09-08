// Ruta: app/src/main/java/com/nexopp/ui/RadialPalette.kt
package com.nexopp.ui

enum class RadialRing(val slotCount: Int) {
    INNER(8),
    OUTER(16),
}

data class RadialSlot(val ring: RadialRing, val index: Int) {
    init {
        require(index in 0 until ring.slotCount) { "slot $index out of range for $ring" }
    }
}

enum class PalettePageOp { NEW_AFTER, NEW_BEFORE, DUPLICATE, DELETE, NEXT, PREVIOUS }

sealed interface PaletteAction {
    data class SelectTool(val tool: EditorTool) : PaletteAction
    data class ToggleTool(val tool: EditorTool) : PaletteAction
    data class SetColor(val argb: Int) : PaletteAction
    data class SetWidth(val widthPt: Float) : PaletteAction {
        init {
            require(widthPt in PEN_WIDTH_MIN..PEN_WIDTH_MAX) { "width $widthPt out of range" }
        }
    }
    data object Undo : PaletteAction
    data object Redo : PaletteAction
    data object ToggleFullPage : PaletteAction
    data class Page(val op: PalettePageOp) : PaletteAction
    data class ApplyPreset(val presetId: String) : PaletteAction
    data class ApplyPresetSlot(val index: Int) : PaletteAction
    data class SwitchPalette(val paletteName: String) : PaletteAction
}

data class RadialPalette(
    val name: String = DEFAULT_NAME,
    val inner: List<PaletteAction?> = List(RadialRing.INNER.slotCount) { null },
    val outer: List<PaletteAction?> = List(RadialRing.OUTER.slotCount) { null },
) {
    init {
        require(inner.size == RadialRing.INNER.slotCount) { "inner ring must have ${RadialRing.INNER.slotCount} slots" }
        require(outer.size == RadialRing.OUTER.slotCount) { "outer ring must have ${RadialRing.OUTER.slotCount} slots" }
    }

    operator fun get(slot: RadialSlot): PaletteAction? = ring(slot.ring)[slot.index]

    fun ring(ring: RadialRing): List<PaletteAction?> = when (ring) {
        RadialRing.INNER -> inner
        RadialRing.OUTER -> outer
    }

    fun with(slot: RadialSlot, action: PaletteAction?): RadialPalette {
        val updated = ring(slot.ring).toMutableList().also { it[slot.index] = action }
        return when (slot.ring) {
            RadialRing.INNER -> copy(inner = updated)
            RadialRing.OUTER -> copy(outer = updated)
        }
    }

    fun without(slot: RadialSlot): RadialPalette = with(slot, null)

    fun cleared(): RadialPalette = RadialPalette(name = name)

    val filledCount: Int get() = inner.count { it != null } + outer.count { it != null }

    val isEmpty: Boolean get() = filledCount == 0

    companion object {
        const val DEFAULT_NAME = "Paleta"

        fun default(): RadialPalette = RadialPalette(
            inner = listOf(
                PaletteAction.SelectTool(EditorTool.PEN),
                PaletteAction.ToggleTool(EditorTool.ERASER),
                PaletteAction.SelectTool(EditorTool.HIGHLIGHTER),
                PaletteAction.ToggleTool(EditorTool.SELECT),
                PaletteAction.Undo,
                PaletteAction.Redo,
                PaletteAction.SelectTool(EditorTool.HAND),
                PaletteAction.ToggleFullPage,
            ),
            outer = List(RadialRing.OUTER.slotCount) { i ->
                PEN_COLORS.getOrNull(i)?.let { PaletteAction.SetColor(it) }
            },
        )
    }
}

fun RadialPalette.slots(): List<Pair<RadialSlot, PaletteAction?>> =
    RadialRing.entries.flatMap { ring ->
        ring(ring).mapIndexed { i, action -> RadialSlot(ring, i) to action }
    }