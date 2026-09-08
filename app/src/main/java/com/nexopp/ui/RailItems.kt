// Ruta: app/src/main/java/com/nexopp/ui/RailItems.kt
package com.nexopp.ui

data class RailItem(
    val id: String,
    val label: String,
)

val PANEL_RAIL_ITEMS: List<RailItem> = listOf(
    RailItem("color", "Color y grosor"),
    RailItem("style", "Estilo"),
    RailItem("presets", "Preajustes"),
    RailItem("shapes", "Reconocimiento de formas"),
    RailItem("guides", "Guías"),
    RailItem("layers", "Capas"),
    RailItem("zoom", "Zoom"),
    RailItem("background", "Fondo"),
    RailItem("pages", "Páginas"),
    RailItem("audio", "Audio"),
)

val RAIL_ITEMS: List<RailItem> =
    TOOL_GROUPS.map { RailItem(it.id, it.label) } + PANEL_RAIL_ITEMS

fun toolGroupForRailItem(id: String): ToolGroup? = TOOL_GROUPS.firstOrNull { it.id == id }

fun orderedRailItems(order: List<String>): List<RailItem> {
    val byId = RAIL_ITEMS.associateBy { it.id }
    val listed = order.distinct().mapNotNull { byId[it] }
    return listed + RAIL_ITEMS.filterNot { it in listed }
}

fun visibleRailItems(order: List<String>, hidden: Set<String>): List<RailItem> =
    orderedRailItems(order).filterNot { it.id in hidden }

fun moveRailItem(order: List<String>, index: Int, delta: Int): List<String> {
    val ids = orderedRailItems(order).map { it.id }.toMutableList()
    val to = index + delta
    if (index !in ids.indices || to !in ids.indices) return ids
    ids.add(to, ids.removeAt(index))
    return ids
}

fun encodeRailIds(ids: Collection<String>): String = ids.joinToString(",")

fun decodeRailIds(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    val known = RAIL_ITEMS.map { it.id }.toSet()
    return raw.split(',').map { it.trim() }.filter { it in known }.distinct()
}