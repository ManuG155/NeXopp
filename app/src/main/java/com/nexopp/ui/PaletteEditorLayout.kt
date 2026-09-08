// --- PaletteEditorLayout.kt ---
package com.nexopp.ui

import kotlin.math.hypot

data class PaletteEditorSlot(
    val slot: RadialSlot,
    val action: PaletteAction?,
    val center: RadialPoint,
    val radius: Float,
)

fun slotMarkRadius(ring: RadialRing): Float = when (ring) {
    RadialRing.INNER -> INNER_SLOT_MARK_RADIUS
    RadialRing.OUTER -> OUTER_SLOT_MARK_RADIUS
}

fun paletteEditorScale(sizePx: Float, geometry: RadialPaletteGeometry = RadialPaletteGeometry()): Float {
    val needed = geometry.outerRingRadius + INNER_SLOT_MARK_RADIUS
    return if (needed <= 0f || sizePx <= 0f) 0f else (sizePx / 2f) / needed
}

fun RadialPalette.editorSlots(
    sizePx: Float,
    geometry: RadialPaletteGeometry = RadialPaletteGeometry(),
): List<PaletteEditorSlot> {
    val scale = paletteEditorScale(sizePx, geometry)
    val scaled = geometry.scaledBy(scale)
    val center = sizePx / 2f
    return slots().map { (slot, action) ->
        PaletteEditorSlot(
            slot = slot,
            action = action,
            center = slot.drawCenter(center, center, scaled),
            radius = slotMarkRadius(slot.ring) * scale,
        )
    }
}

fun RadialPalette.editorSlotAt(
    x: Float,
    y: Float,
    sizePx: Float,
    geometry: RadialPaletteGeometry = RadialPaletteGeometry(),
): RadialSlot? {
    val scale = paletteEditorScale(sizePx, geometry)
    if (scale <= 0f) return null
    val scaled = geometry.scaledBy(scale)
    val center = sizePx / 2f
    val radius = hypot(x - center, y - center)
    if (radius <= scaled.deadZoneRadius) return null
    if (radius > scaled.outerRingRadius + OUTER_SLOT_MARK_RADIUS * scale) return null
    return (hitTest(center, center, x, y, scaled) as? RadialHit.Slot)?.slot
}

private fun RadialPaletteGeometry.scaledBy(scale: Float) = RadialPaletteGeometry(
    deadZoneRadius = deadZoneRadius * scale,
    innerRingRadius = innerRingRadius * scale,
    outerRingRadius = outerRingRadius * scale,
    dismissRadius = dismissRadius * scale,
)

fun slotMarkRadius(ring: RadialRing, geometry: RadialPaletteGeometry): Float =
    slotMarkRadius(ring) * (geometry.outerRingRadius / RadialPaletteGeometry.DEFAULT_OUTER_RING_RADIUS)

const val INNER_SLOT_MARK_RADIUS = 20f
const val OUTER_SLOT_MARK_RADIUS = 15f