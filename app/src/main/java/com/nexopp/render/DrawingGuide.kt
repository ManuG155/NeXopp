package com.nexopp.render

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.PI

/**
 * STEM drawing guides and physical instruments (Ruler, Setsquares, Protractor, Compass)
 * as pure page-space geometry.
 *
 * A guide is an input aid that pulls drawn vertices within [GRAB_PT] of its edges so strokes
 * are ruled perfectly straight, curved along precise circular arcs, or measured to exact degrees.
 * Strokes land in the `.xopp` file as standard vector strokes, preserving format compatibility.
 */
sealed interface DrawingGuide {

    val x: Double
    val y: Double

    fun moved(dx: Double, dy: Double): DrawingGuide

    fun project(px: Double, py: Double): Pair<Double, Double>

    /**
     * Digital metric ruler with millimeter/centimeter graduation.
     * [x]/[y] is the top-left start corner, [angle] the direction of its top edge in radians.
     */
    data class Ruler(
        override val x: Double,
        override val y: Double,
        val length: Double = DEFAULT_RULER_LENGTH_PT,
        val height: Double = DEFAULT_RULER_HEIGHT_PT,
        val angle: Double = 0.0,
    ) : DrawingGuide {

        fun corners(): List<Pair<Double, Double>> {
            val ux = cos(angle); val uy = sin(angle)
            val vx = -uy; val vy = ux
            val c0 = x to y
            val c1 = (x + ux * length) to (y + uy * length)
            val c2 = (c1.first + vx * height) to (c1.second + vy * height)
            val c3 = (x + vx * height) to (y + vy * height)
            return listOf(c0, c1, c2, c3)
        }

        override fun moved(dx: Double, dy: Double): Ruler = copy(x = x + dx, y = y + dy)

        fun contains(px: Double, py: Double): Boolean {
            val (c0, c1, c2, c3) = corners()
            val d1 = cross(c0, c1, px, py)
            val d2 = cross(c1, c2, px, py)
            val d3 = cross(c2, c3, px, py)
            val d4 = cross(c3, c0, px, py)
            val anyNeg = d1 < 0 || d2 < 0 || d3 < 0 || d4 < 0
            val anyPos = d1 > 0 || d2 > 0 || d3 > 0 || d4 > 0
            return !(anyNeg && anyPos)
        }

        fun aimedAt(tx: Double, ty: Double, snapAngle: Boolean): Ruler {
            val raw = atan2(ty - y, tx - x)
            val len = hypot(tx - x, ty - y).coerceAtLeast(MIN_SIZE_PT)
            return copy(angle = if (snapAngle) Snapping.snapAngle(raw) else raw, length = len)
        }

        override fun project(px: Double, py: Double): Pair<Double, Double> {
            val (c0, c1, c2, c3) = corners()
            val qTop = closestOnSegment(px, py, c0.first, c0.second, c1.first, c1.second)
            val qBot = closestOnSegment(px, py, c3.first, c3.second, c2.first, c2.second)
            val dTop = hypot(px - qTop.first, py - qTop.second)
            val dBot = hypot(px - qBot.first, py - qBot.second)
            val (best, dist) = if (dTop <= dBot) (qTop to dTop) else (qBot to dBot)
            return if (dist <= GRAB_PT) best else (px to py)
        }

        companion object {
            const val DEFAULT_RULER_LENGTH_PT = 320.0
            const val DEFAULT_RULER_HEIGHT_PT = 52.0
            /** 1 point is 1/72 inch = 25.4/72 mm ≈ 0.352778 mm -> 1 mm ≈ 2.83465 pt */
            const val PT_PER_MM = 72.0 / 25.4
            const val PT_PER_CM = PT_PER_MM * 10.0
        }
    }

    /**
     * 30/60/90 triangle (Cartabón). [x]/[y] is the right-angle corner and [angle] is the long leg orientation.
     */
    data class Setsquare(
        override val x: Double,
        override val y: Double,
        val size: Double = DEFAULT_SIZE_PT,
        val angle: Double = 0.0,
    ) : DrawingGuide {

        fun corners(): List<Pair<Double, Double>> {
            val ux = cos(angle); val uy = sin(angle)
            return listOf(
                x to y,
                (x + ux * size) to (y + uy * size),
                (x - uy * size * SHORT_LEG_RATIO) to (y + ux * size * SHORT_LEG_RATIO),
            )
        }

        override fun moved(dx: Double, dy: Double): Setsquare = copy(x = x + dx, y = y + dy)

        fun contains(px: Double, py: Double): Boolean {
            val (a, b, c) = corners()
            val d1 = cross(a, b, px, py)
            val d2 = cross(b, c, px, py)
            val d3 = cross(c, a, px, py)
            val anyNeg = d1 < 0 || d2 < 0 || d3 < 0
            val anyPos = d1 > 0 || d2 > 0 || d3 > 0
            return !(anyNeg && anyPos)
        }

        fun aimedAt(tx: Double, ty: Double, snapAngle: Boolean): Setsquare {
            val raw = atan2(ty - y, tx - x)
            val len = hypot(tx - x, ty - y).coerceAtLeast(MIN_SIZE_PT)
            return copy(angle = if (snapAngle) Snapping.snapAngle(raw) else raw, size = len)
        }

        override fun project(px: Double, py: Double): Pair<Double, Double> {
            val c = corners()
            val edges = listOf(c[0] to c[1], c[0] to c[2], c[1] to c[2])
            var best: Pair<Double, Double>? = null
            var bestDist = GRAB_PT
            for ((a, b) in edges) {
                val q = closestOnSegment(px, py, a.first, a.second, b.first, b.second)
                val d = hypot(px - q.first, py - q.second)
                if (d < bestDist) { bestDist = d; best = q }
            }
            return best ?: (px to py)
        }

        companion object {
            const val SHORT_LEG_RATIO: Double = 0.5773502691896257
        }
    }

    /**
     * 45/45/90 triangle (Escuadra). Both catheti have equal length [size].
     */
    data class Setsquare45(
        override val x: Double,
        override val y: Double,
        val size: Double = DEFAULT_SIZE_PT,
        val angle: Double = 0.0,
    ) : DrawingGuide {

        fun corners(): List<Pair<Double, Double>> {
            val ux = cos(angle); val uy = sin(angle)
            return listOf(
                x to y,
                (x + ux * size) to (y + uy * size),
                (x - uy * size) to (y + ux * size),
            )
        }

        override fun moved(dx: Double, dy: Double): Setsquare45 = copy(x = x + dx, y = y + dy)

        fun contains(px: Double, py: Double): Boolean {
            val (a, b, c) = corners()
            val d1 = cross(a, b, px, py)
            val d2 = cross(b, c, px, py)
            val d3 = cross(c, a, px, py)
            val anyNeg = d1 < 0 || d2 < 0 || d3 < 0
            val anyPos = d1 > 0 || d2 > 0 || d3 > 0
            return !(anyNeg && anyPos)
        }

        fun aimedAt(tx: Double, ty: Double, snapAngle: Boolean): Setsquare45 {
            val raw = atan2(ty - y, tx - x)
            val len = hypot(tx - x, ty - y).coerceAtLeast(MIN_SIZE_PT)
            return copy(angle = if (snapAngle) Snapping.snapAngle(raw) else raw, size = len)
        }

        override fun project(px: Double, py: Double): Pair<Double, Double> {
            val c = corners()
            val edges = listOf(c[0] to c[1], c[0] to c[2], c[1] to c[2])
            var best: Pair<Double, Double>? = null
            var bestDist = GRAB_PT
            for ((a, b) in edges) {
                val q = closestOnSegment(px, py, a.first, a.second, b.first, b.second)
                val d = hypot(px - q.first, py - q.second)
                if (d < bestDist) { bestDist = d; best = q }
            }
            return best ?: (px to py)
        }
    }

    /**
     * Protractor (Transportador de ángulos 180°). [x]/[y] is the origin center.
     */
    data class Protractor(
        override val x: Double,
        override val y: Double,
        val radius: Double = DEFAULT_PROTRACTOR_RADIUS_PT,
        val angle: Double = 0.0,
    ) : DrawingGuide {

        override fun moved(dx: Double, dy: Double): Protractor = copy(x = x + dx, y = y + dy)

        fun aimedAt(tx: Double, ty: Double, snapAngle: Boolean): Protractor {
            val raw = atan2(ty - y, tx - x)
            val len = hypot(tx - x, ty - y).coerceAtLeast(MIN_SIZE_PT)
            return copy(angle = if (snapAngle) Snapping.snapAngle(raw) else raw, radius = len)
        }

        fun baselineEndpoints(): Pair<Pair<Double, Double>, Pair<Double, Double>> {
            val ux = cos(angle); val uy = sin(angle)
            val p1 = (x - ux * radius) to (y - uy * radius)
            val p2 = (x + ux * radius) to (y + uy * radius)
            return p1 to p2
        }

        fun contains(px: Double, py: Double): Boolean {
            val dx = px - x; val dy = py - y
            val d = hypot(dx, dy)
            if (d > radius) return false
            // Check if point is on the upper semicircle along baseline normal (pointing towards -Y in screen space)
            val normalX = sin(angle); val normalY = -cos(angle)
            val dot = dx * normalX + dy * normalY
            return dot >= -5.0
        }

        override fun project(px: Double, py: Double): Pair<Double, Double> {
            val dx = px - x; val dy = py - y
            val d = hypot(dx, dy)
            if (d < 1e-9) return px to py

            // 1. Distance to circular arc (if in the upper semicircle)
            val normalX = sin(angle); val normalY = -cos(angle)
            val dot = dx * normalX + dy * normalY
            var bestArc: Pair<Double, Double>? = null
            var bestArcDist = Double.MAX_VALUE
            if (dot >= -GRAB_PT) {
                val projArc = (x + dx / d * radius) to (y + dy / d * radius)
                val dist = hypot(px - projArc.first, py - projArc.second)
                if (dist <= GRAB_PT) {
                    bestArc = projArc
                    bestArcDist = dist
                }
            }

            // 2. Distance to straight baseline
            val (b1, b2) = baselineEndpoints()
            val qBase = closestOnSegment(px, py, b1.first, b1.second, b2.first, b2.second)
            val dBase = hypot(px - qBase.first, py - qBase.second)

            return when {
                bestArc != null && bestArcDist <= dBase -> bestArc
                dBase <= GRAB_PT -> qBase
                else -> px to py
            }
        }

        companion object {
            const val DEFAULT_PROTRACTOR_RADIUS_PT = 130.0
        }
    }

    /**
     * Compass: circle of [radius] pt about center ([x], [y]).
     */
    data class Compass(
        override val x: Double,
        override val y: Double,
        val radius: Double = DEFAULT_RADIUS_PT,
    ) : DrawingGuide {

        override fun moved(dx: Double, dy: Double): Compass = copy(x = x + dx, y = y + dy)

        fun openedTo(tx: Double, ty: Double): Compass =
            copy(radius = hypot(tx - x, ty - y).coerceAtLeast(MIN_SIZE_PT))

        override fun project(px: Double, py: Double): Pair<Double, Double> {
            val dx = px - x; val dy = py - y
            val d = hypot(dx, dy)
            if (d < 1e-9) return px to py
            if (kotlin.math.abs(d - radius) > GRAB_PT) return px to py
            return (x + dx / d * radius) to (y + dy / d * radius)
        }
    }

    companion object {
        const val GRAB_PT: Double = 18.0
        const val DEFAULT_SIZE_PT: Double = 180.0
        const val DEFAULT_RADIUS_PT: Double = 100.0
        const val MIN_SIZE_PT: Double = 20.0

        fun closestOnSegment(
            px: Double, py: Double,
            ax: Double, ay: Double, bx: Double, by: Double,
        ): Pair<Double, Double> {
            val vx = bx - ax
            val vy = by - ay
            val len2 = vx * vx + vy * vy
            if (len2 < 1e-12) return ax to ay
            val t = (((px - ax) * vx + (py - ay) * vy) / len2).coerceIn(0.0, 1.0)
            return (ax + t * vx) to (ay + t * vy)
        }

        fun cross(
            a: Pair<Double, Double>,
            b: Pair<Double, Double>,
            px: Double,
            py: Double,
        ): Double = (b.first - a.first) * (py - a.second) - (b.second - a.second) * (px - a.first)
    }
}

enum class GuideKind(val label: String) {
    NONE("Desactivado"),
    RULER("Regla métrica"),
    SETSQUARE("Cartabón (30°/60°)"),
    SETSQUARE_45("Escuadra (45°)"),
    PROTRACTOR("Transportador (180°)"),
    COMPASS("Compás"),
    ;

    fun place(cx: Double, cy: Double): DrawingGuide? = when (this) {
        NONE -> null
        RULER -> DrawingGuide.Ruler(
            x = cx - DrawingGuide.Ruler.DEFAULT_RULER_LENGTH_PT / 2,
            y = cy - DrawingGuide.Ruler.DEFAULT_RULER_HEIGHT_PT / 2,
        )
        SETSQUARE -> DrawingGuide.Setsquare(
            x = cx - DrawingGuide.DEFAULT_SIZE_PT / 2,
            y = cy + DrawingGuide.DEFAULT_SIZE_PT / 4,
        )
        SETSQUARE_45 -> DrawingGuide.Setsquare45(
            x = cx - DrawingGuide.DEFAULT_SIZE_PT / 2,
            y = cy + DrawingGuide.DEFAULT_SIZE_PT / 4,
        )
        PROTRACTOR -> DrawingGuide.Protractor(
            x = cx,
            y = cy,
        )
        COMPASS -> DrawingGuide.Compass(cx, cy)
    }
}
