// Ruta: app/src/main/java/com/nexopp/ui/RadialPaletteLayout.kt
package com.nexopp.ui

import kotlin.math.cos
import kotlin.math.sin

data class RadialPoint(val x: Float, val y: Float)

fun clampAnchor(
    x: Float,
    y: Float,
    viewWidth: Float,
    viewHeight: Float,
    geometry: RadialPaletteGeometry = RadialPaletteGeometry(),
    margin: Float = DEFAULT_ANCHOR_MARGIN,
): RadialPoint {
    val inset = geometry.outerRingRadius + margin
    return RadialPoint(clampAxis(x, viewWidth, inset), clampAxis(y, viewHeight, inset))
}

private fun clampAxis(value: Float, extent: Float, inset: Float): Float =
    if (extent < inset * 2f) extent / 2f else value.coerceIn(inset, extent - inset)

fun slotDrawRadius(ring: RadialRing, geometry: RadialPaletteGeometry = RadialPaletteGeometry()): Float =
    when (ring) {
        RadialRing.INNER -> (geometry.deadZoneRadius + geometry.innerRingRadius) / 2f
        RadialRing.OUTER -> (geometry.innerRingRadius + geometry.outerRingRadius) / 2f
    }

fun RadialSlot.drawCenter(
    anchorX: Float,
    anchorY: Float,
    geometry: RadialPaletteGeometry = RadialPaletteGeometry(),
): RadialPoint {
    val radians = Math.toRadians(centerDegrees().toDouble())
    val r = slotDrawRadius(ring, geometry)
    return RadialPoint(
        anchorX + (sin(radians) * r).toFloat(),
        anchorY - (cos(radians) * r).toFloat(),
    )
}

const val DEFAULT_ANCHOR_MARGIN = 16f