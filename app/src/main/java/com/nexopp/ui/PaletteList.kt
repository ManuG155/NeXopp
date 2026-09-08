// --- PaletteList.kt ---
package com.nexopp.ui

data class PaletteSet(
    val palettes: List<RadialPalette> = listOf(RadialPalette.default()),
    val activeIndex: Int = 0,
) {
    val active: RadialPalette
        get() = palettes.getOrNull(activeIndex) ?: palettes.firstOrNull() ?: RadialPalette.default()

    fun normalized(): PaletteSet {
        val list = palettes.ifEmpty { listOf(RadialPalette.default()) }
        return PaletteSet(list, activeIndex.coerceIn(0, list.lastIndex))
    }

    fun withPaletteAt(index: Int, palette: RadialPalette): PaletteSet {
        if (index !in palettes.indices) return this
        return copy(palettes = palettes.toMutableList().also { it[index] = palette })
    }
}

fun addPalette(set: PaletteSet, name: String = ""): PaletteSet {
    val chosen = uniquePaletteName(set.palettes, name.trim().ifEmpty { "Paleta ${set.palettes.size + 1}" })
    val list = set.palettes + RadialPalette(name = chosen)
    return PaletteSet(list, list.lastIndex)
}

fun removePalette(set: PaletteSet, index: Int): PaletteSet {
    if (set.palettes.size <= 1 || index !in set.palettes.indices) return set
    val list = set.palettes.toMutableList().also { it.removeAt(index) }
    val active = when {
        set.activeIndex > index -> set.activeIndex - 1
        else -> set.activeIndex
    }
    return PaletteSet(list, active.coerceIn(0, list.lastIndex))
}

fun movePalette(set: PaletteSet, index: Int, delta: Int): PaletteSet {
    val to = index + delta
    if (index !in set.palettes.indices || to !in set.palettes.indices) return set
    val list = set.palettes.toMutableList().also { it.add(to, it.removeAt(index)) }
    val active = when (set.activeIndex) {
        index -> to
        in minOf(index, to)..maxOf(index, to) -> set.activeIndex + if (to > index) -1 else 1
        else -> set.activeIndex
    }
    return PaletteSet(list, active)
}

fun renamePalette(set: PaletteSet, index: Int, name: String): PaletteSet {
    if (index !in set.palettes.indices) return set
    val wanted = name.trim().ifEmpty { RadialPalette.DEFAULT_NAME }
    val others = set.palettes.filterIndexed { i, _ -> i != index }
    return set.withPaletteAt(index, set.palettes[index].copy(name = uniquePaletteName(others, wanted)))
}

fun activatePalette(set: PaletteSet, index: Int): PaletteSet =
    if (index in set.palettes.indices) set.copy(activeIndex = index) else set

fun migratedPaletteSet(listRaw: String?, legacyRaw: String?, activeIndex: Int): PaletteSet {
    val saved = decodeRadialPalettes(listRaw)
    if (saved.isNotEmpty()) return PaletteSet(saved, activeIndex).normalized()
    return PaletteSet(listOf(decodeRadialPalette(legacyRaw) ?: RadialPalette.default()), 0)
}

private fun uniquePaletteName(existing: List<RadialPalette>, wanted: String): String {
    val taken = existing.map { it.name }.toSet()
    if (wanted !in taken) return wanted
    var n = 2
    while ("$wanted $n" in taken) n++
    return "$wanted $n"
}