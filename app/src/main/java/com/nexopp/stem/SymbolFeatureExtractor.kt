package com.nexopp.stem

import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import kotlin.math.*

/**
 * Extracts normalised geometric features from raw handwriting strokes.
 *
 * Every metric is designed to be **scale-, position- and rotation-tolerant** so
 * that the same symbol drawn large/small, tilted, or in a different position on
 * the page produces similar feature vectors.
 *
 * The features fall into five groups:
 *   1. Global geometry (aspect, closure, linearity, path-to-diagonal ratio)
 *   2. Directional chain-code histogram (8 sectors)
 *   3. Curvature & inflection counts
 *   4. Zonal occupation (3×3 grid)
 *   5. Start/end positioning
 */
object SymbolFeatureExtractor {

    /** How many direction sectors the chain-code histogram uses. */
    const val DIRECTION_SECTORS = 8

    /**
     * Complete feature set extracted from one or more strokes that form a
     * single glyph candidate.
     */
    data class StrokeFeatures(
        // --- Global geometry ---
        /** width/height of the bounding box. Capped to [0.05, 20]. */
        val aspectRatio: Double,
        /** distance(start,end) / pathLength. 0→complex loop, 1→straight line. */
        val linearity: Double,
        /** distance(start,end) / diagonal(bbox). 0→closed, 1→maximally open. */
        val closureRatio: Double,
        /** pathLength / diagonal(bbox). Values >π indicate lots of self-overlapping. */
        val pathToDiagonalRatio: Double,
        /** Maximum perpendicular distance from the start→end chord, normalised by diagonal. */
        val maxDeviationNorm: Double,

        // --- Directional chain-code histogram ---
        /** 8-bin histogram (sum=1) of segment directions (0°=right, counter-clockwise). */
        val directionHistogram: DoubleArray,

        // --- Curvature & inflections ---
        val xInflections: Int,
        val yInflections: Int,
        /** Total absolute curvature (sum of angle changes) in radians, normalised by 2π. */
        val totalCurvatureNorm: Double,
        /** Number of self-crossings of the stroke path (approximated). */
        val selfCrossings: Int,

        // --- Zonal occupation ---
        /** 3×3 grid: fraction of points landing in each cell. Row-major (top-left first). */
        val zoneOccupation: DoubleArray, // size 9

        // --- Start / End positioning ---
        /** Normalised start (x,y) in [0,1]² relative to bounding box. */
        val startX: Double, val startY: Double,
        /** Normalised end (x,y) in [0,1]² relative to bounding box. */
        val endX: Double, val endY: Double,

        // --- Multi-stroke ---
        val strokeCount: Int,
        /** Bounding box height in raw page points (useful for absolute-size checks like dots). */
        val rawDiagonal: Double,
        /** Path length in raw page points. */
        val rawPathLength: Double,
        /** Bounding box height in raw page points */
        val rawHeight: Double,
        /** Bounding box width in raw page points */
        val rawWidth: Double,

        // --- Vertical profile ---
        /** Fraction of path length that goes downward (dy>0). */
        val descendingFraction: Double,
        /** Fraction of path where y is above the vertical midpoint of the bbox. */
        val upperHalfFraction: Double,
    )

    /**
     * Extract features from a list of strokes belonging to a single glyph cluster.
     * The strokes are concatenated for single-path analysis but multi-stroke
     * information (count, relative bounds) is preserved.
     */
    fun extract(strokes: List<Stroke>): StrokeFeatures {
        val allPoints = strokes.flatMap { it.points }
        if (allPoints.size < 2) return emptyFeatures(strokes.size)

        // --- Bounding box ---
        var minX = Double.MAX_VALUE; var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
        for (p in allPoints) {
            if (p.x < minX) minX = p.x; if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x; if (p.y > maxY) maxY = p.y
        }
        val bw = max(1.0, maxX - minX)
        val bh = max(1.0, maxY - minY)
        val diag = hypot(bw, bh)

        // --- Flatten into a single point list preserving per-stroke boundaries ---
        val pts = allPoints

        val pStart = pts.first()
        val pEnd = pts.last()
        val startEndDist = hypot(pEnd.x - pStart.x, pEnd.y - pStart.y)

        // --- Path length ---
        var pathLen = 0.0
        for (i in 1 until pts.size) {
            pathLen += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
        }
        pathLen = max(1.0, pathLen)

        val linearity = startEndDist / pathLen
        val closureRatio = startEndDist / max(1.0, diag)
        val pathToDiag = pathLen / max(1.0, diag)

        // --- Max deviation from start→end chord ---
        val chordDx = pEnd.x - pStart.x
        val chordDy = pEnd.y - pStart.y
        val chordLen2 = chordDx * chordDx + chordDy * chordDy
        val maxDev = if (chordLen2 < 1e-6) {
            pts.maxOf { hypot(it.x - pStart.x, it.y - pStart.y) }
        } else {
            pts.maxOf { p ->
                abs((p.x - pStart.x) * chordDy - (p.y - pStart.y) * chordDx) / sqrt(chordLen2)
            }
        }

        // --- Direction histogram (8 sectors) ---
        val dirHist = DoubleArray(DIRECTION_SECTORS)
        var totalDirLen = 0.0
        for (i in 1 until pts.size) {
            val dx = pts[i].x - pts[i - 1].x
            val dy = pts[i].y - pts[i - 1].y
            val segLen = hypot(dx, dy)
            if (segLen < 0.5) continue
            var angle = atan2(dy, dx) // [-π, π]
            if (angle < 0) angle += 2.0 * PI
            val sector = ((angle / (2.0 * PI)) * DIRECTION_SECTORS).toInt().coerceIn(0, DIRECTION_SECTORS - 1)
            dirHist[sector] += segLen
            totalDirLen += segLen
        }
        if (totalDirLen > 0) for (i in dirHist.indices) dirHist[i] /= totalDirLen

        // --- Inflections ---
        var xInfl = 0; var yInfl = 0
        for (i in 2 until pts.size) {
            val dx1 = pts[i - 1].x - pts[i - 2].x
            val dx2 = pts[i].x - pts[i - 1].x
            if (dx1 * dx2 < -1.0) xInfl++
            val dy1 = pts[i - 1].y - pts[i - 2].y
            val dy2 = pts[i].y - pts[i - 1].y
            if (dy1 * dy2 < -1.0) yInfl++
        }

        // --- Total curvature ---
        var totalCurv = 0.0
        for (i in 2 until pts.size) {
            val dx1 = pts[i - 1].x - pts[i - 2].x
            val dy1 = pts[i - 1].y - pts[i - 2].y
            val dx2 = pts[i].x - pts[i - 1].x
            val dy2 = pts[i].y - pts[i - 1].y
            val a1 = atan2(dy1, dx1)
            val a2 = atan2(dy2, dx2)
            var da = a2 - a1
            while (da > PI) da -= 2 * PI
            while (da < -PI) da += 2 * PI
            totalCurv += abs(da)
        }

        // --- Self-crossings (approximate via segment intersection test on sampled segments) ---
        val crossings = countSelfCrossings(pts)

        // --- Zone occupation (3×3) ---
        val zones = DoubleArray(9)
        for (p in pts) {
            val col = ((p.x - minX) / bw * 3.0).toInt().coerceIn(0, 2)
            val row = ((p.y - minY) / bh * 3.0).toInt().coerceIn(0, 2)
            zones[row * 3 + col] += 1.0
        }
        val totalZ = zones.sum()
        if (totalZ > 0) for (i in zones.indices) zones[i] /= totalZ

        // --- Normalised start/end ---
        val sx = ((pStart.x - minX) / bw).coerceIn(0.0, 1.0)
        val sy = ((pStart.y - minY) / bh).coerceIn(0.0, 1.0)
        val ex = ((pEnd.x - minX) / bw).coerceIn(0.0, 1.0)
        val ey = ((pEnd.y - minY) / bh).coerceIn(0.0, 1.0)

        // --- Descending fraction ---
        var descLen = 0.0
        for (i in 1 until pts.size) {
            if (pts[i].y > pts[i - 1].y + 0.5) {
                descLen += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
            }
        }

        // --- Upper-half fraction ---
        val midY = minY + bh / 2.0
        val upperCount = pts.count { it.y < midY }

        return StrokeFeatures(
            aspectRatio = (bw / bh).coerceIn(0.05, 20.0),
            linearity = linearity,
            closureRatio = closureRatio,
            pathToDiagonalRatio = pathToDiag,
            maxDeviationNorm = maxDev / max(1.0, diag),
            directionHistogram = dirHist,
            xInflections = xInfl,
            yInflections = yInfl,
            totalCurvatureNorm = totalCurv / (2.0 * PI),
            selfCrossings = crossings,
            zoneOccupation = zones,
            startX = sx, startY = sy,
            endX = ex, endY = ey,
            strokeCount = strokes.size,
            rawDiagonal = diag,
            rawPathLength = pathLen,
            rawHeight = bh,
            rawWidth = bw,
            descendingFraction = descLen / pathLen,
            upperHalfFraction = upperCount.toDouble() / pts.size,
        )
    }

    /**
     * Count approximate self-crossings by testing sampled segments.
     * We sample every Nth segment to keep this O(n) rather than O(n²).
     */
    private fun countSelfCrossings(pts: List<StrokePoint>): Int {
        if (pts.size < 6) return 0
        val step = max(1, pts.size / 50) // Sample ~50 segments
        val segments = mutableListOf<IntArray>()
        var i = 0
        while (i + step < pts.size) {
            segments.add(intArrayOf(i, i + step))
            i += step
        }
        if (segments.isEmpty()) return 0
        var crossings = 0
        for (a in 0 until segments.size - 2) {
            for (b in a + 2 until segments.size) {
                val (a1, a2) = segments[a]
                val (b1, b2) = segments[b]
                if (segmentsIntersect(
                        pts[a1].x, pts[a1].y, pts[a2].x, pts[a2].y,
                        pts[b1].x, pts[b1].y, pts[b2].x, pts[b2].y
                    )) crossings++
            }
        }
        return crossings
    }

    private fun segmentsIntersect(
        ax1: Double, ay1: Double, ax2: Double, ay2: Double,
        bx1: Double, by1: Double, bx2: Double, by2: Double
    ): Boolean {
        val d1 = cross(bx1, by1, bx2, by2, ax1, ay1)
        val d2 = cross(bx1, by1, bx2, by2, ax2, ay2)
        val d3 = cross(ax1, ay1, ax2, ay2, bx1, by1)
        val d4 = cross(ax1, ay1, ax2, ay2, bx2, by2)
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true
        return false
    }

    private fun cross(
        ax: Double, ay: Double, bx: Double, by: Double,
        cx: Double, cy: Double
    ): Double = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)

    private fun emptyFeatures(strokeCount: Int) = StrokeFeatures(
        aspectRatio = 1.0, linearity = 1.0, closureRatio = 1.0,
        pathToDiagonalRatio = 1.0, maxDeviationNorm = 0.0,
        directionHistogram = DoubleArray(DIRECTION_SECTORS),
        xInflections = 0, yInflections = 0,
        totalCurvatureNorm = 0.0, selfCrossings = 0,
        zoneOccupation = DoubleArray(9),
        startX = 0.5, startY = 0.5, endX = 0.5, endY = 0.5,
        strokeCount = strokeCount,
        rawDiagonal = 0.0, rawPathLength = 0.0, rawHeight = 0.0, rawWidth = 0.0,
        descendingFraction = 0.0, upperHalfFraction = 0.5,
    )
}
