// Ruta: app/src/main/java/com/nexopp/ui/RadialPaletteHitTest.kt
package com.nexopp.ui

import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot

data class RadialPaletteGeometry(
    val deadZoneRadius: Float = DEFAULT_DEAD_ZONE_RADIUS,
    val innerRingRadius: Float = DEFAULT_INNER_RING_RADIUS,
    val outerRingRadius: Float = DEFAULT_OUTER_RING_RADIUS,
    val dismissRadius: Float = DEFAULT_DISMISS_RADIUS,
) {
    init {
        require(deadZoneRadius >= 0f) { "dead zone radius must not be negative" }
        require(innerRingRadius > deadZoneRadius) { "inner ring must lie outside the dead zone" }
        require(outerRingRadius > innerRingRadius) { "outer ring must lie outside the inner ring" }
        require(dismissRadius >= outerRingRadius) { "dismiss ring must not lie inside the outer ring" }
    }

    companion object {
        const val DEFAULT_DEAD_ZONE_RADIUS = 32f
        const val DEFAULT_INNER_RING_RADIUS = 100f
        const val DEFAULT_OUTER_RING_RADIUS = 160f
        const val DEFAULT_DISMISS_RADIUS = DEFAULT_OUTER_RING_RADIUS
    }
}

sealed interface RadialHit {
    data object Inert : RadialHit
    data object Outside : RadialHit
    data class Slot(val slot: RadialSlot, val action: PaletteAction?) : RadialHit
}

fun RadialPalette.hitTest(
    anchorX: Float,
    anchorY: Float,
    x: Float,
    y: Float,
    geometry: RadialPaletteGeometry = RadialPaletteGeometry(),
): RadialHit {
    val dx = x - anchorX
    val dy = y - anchorY
    val radius = hypot(dx, dy)
    if (radius <= geometry.deadZoneRadius) return RadialHit.Inert
    if (radius > geometry.dismissRadius) return RadialHit.Outside

    val ring = if (radius <= geometry.innerRingRadius) RadialRing.INNER else RadialRing.OUTER
    val index = slotIndexAt(clockwiseDegrees(dx, dy), ring)
    val slot = RadialSlot(ring, index)
    if (!slot.covers(anchorX, anchorY, x, y, geometry)) return RadialHit.Outside
    return RadialHit.Slot(slot, this[slot])
}

internal fun RadialSlot.covers(
    anchorX: Float,
    anchorY: Float,
    x: Float,
    y: Float,
    geometry: RadialPaletteGeometry,
): Boolean {
    val c = drawCenter(anchorX, anchorY, geometry)
    return hypot(x - c.x, y - c.y) <= slotMarkRadius(ring, geometry)
}

internal fun clockwiseDegrees(dx: Float, dy: Float): Float {
    val degrees = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
    return (degrees + 360f) % 360f
}

internal fun slotIndexAt(degrees: Float, ring: RadialRing): Int {
    val wedge = 360f / ring.slotCount
    val shifted = (degrees + wedge / 2f + 360f) % 360f
    return floor(shifted / wedge).toInt().coerceIn(0, ring.slotCount - 1)
}

fun RadialSlot.centerDegrees(): Float = index * (360f / ring.slotCount)