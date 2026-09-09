package com.nexopp.stem

import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import kotlin.math.*

/**
 * 2D Bounding Box for strokes and recognized mathematical glyphs.
 */
data class MathBoundingBox(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double
) {
    val width: Double get() = max(0.0, right - left)
    val height: Double get() = max(0.0, bottom - top)
    val centerX: Double get() = (left + right) / 2.0
    val centerY: Double get() = (top + bottom) / 2.0

    fun overlaps(other: MathBoundingBox, padX: Double = 2.0, padY: Double = 2.0): Boolean {
        return (left - padX) <= other.right &&
               (right + padX) >= other.left &&
               (top - padY) <= other.bottom &&
               (bottom + padY) >= other.top
    }

    fun contains(x: Double, y: Double): Boolean =
        x in left..right && y in top..bottom
}

/**
 * An individual recognized mathematical symbol with spatial positioning and confidence.
 */
data class MathSymbol(
    val latex: String,
    val unicode: String,
    val confidence: Double,
    val bounds: MathBoundingBox,
    val isOperator: Boolean = false,
    val isFractionBar: Boolean = false,
    val isSlash: Boolean = false,
    val isRadical: Boolean = false,
    val isIntegral: Boolean = false,
    val isSummation: Boolean = false,
    val isDelimiter: Boolean = false,
    val isUnknown: Boolean = false,
    val candidates: List<String> = emptyList(),
    val originalStrokes: List<Stroke> = emptyList()
)

/**
 * Result of handwriting math recognition.
 */
data class MathRecognitionResult(
    val latex: String,
    val unicodeText: String,
    val confidence: Double,
    val bounds: MathBoundingBox,
    val symbolCount: Int,
    val originalStrokes: List<Stroke>,
    val hasUnknownSymbols: Boolean = false,
    val symbols: List<MathSymbol> = emptyList()
)

/**
 * 100% Offline, robust Handwriting Math Recognition Engine for STEM tablets.
 * Transforms natural stylus handwriting into clean LaTeX and mathematical representations.
 */
object MathHandwritingEngine {

    fun recognize(strokes: List<Stroke>): MathRecognitionResult {
        if (strokes.isEmpty()) {
            return MathRecognitionResult(
                latex = "",
                unicodeText = "",
                confidence = 1.0,
                bounds = MathBoundingBox(0.0, 0.0, 0.0, 0.0),
                symbolCount = 0,
                originalStrokes = emptyList(),
                hasUnknownSymbols = false,
                symbols = emptyList()
            )
        }

        val totalBounds = computeTotalBounds(strokes)
        val clusters = clusterStrokes(strokes)
        val symbols = clusters.map { cluster -> classifyStrokeCluster(cluster) }

        // Spatial 2D Structural Math Parsing
        val mathTree = parseSpatialMath(symbols)
        val latex = mathTree.toLatex().trim()
        val unicode = mathTree.toUnicode().trim()

        val avgConfidence = if (symbols.isNotEmpty()) {
            symbols.map { it.confidence }.average()
        } else 1.0

        val hasUnknown = symbols.any { it.isUnknown }

        return MathRecognitionResult(
            latex = latex,
            unicodeText = unicode,
            confidence = avgConfidence,
            bounds = totalBounds,
            symbolCount = symbols.size,
            originalStrokes = strokes,
            hasUnknownSymbols = hasUnknown,
            symbols = symbols
        )
    }

    // --- Stroke Clustering & Spatial Segmentation ---------------------------------------------

    private data class StrokeCluster(
        val strokes: List<Stroke>,
        val bounds: MathBoundingBox
    )

    private fun strokeBounds(stroke: Stroke): MathBoundingBox {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        for (p in stroke.points) {
            val r = max(0.5, p.width / 2.0)
            minX = min(minX, p.x - r)
            minY = min(minY, p.y - r)
            maxX = max(maxX, p.x + r)
            maxY = max(maxY, p.y + r)
        }

        if (minX == Double.MAX_VALUE) return MathBoundingBox(0.0, 0.0, 1.0, 1.0)
        return MathBoundingBox(minX, minY, maxX, maxY)
    }

    private fun computeTotalBounds(strokes: List<Stroke>): MathBoundingBox {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        for (s in strokes) {
            for (p in s.points) {
                val r = max(0.5, p.width / 2.0)
                minX = min(minX, p.x - r)
                minY = min(minY, p.y - r)
                maxX = max(maxX, p.x + r)
                maxY = max(maxY, p.y + r)
            }
        }

        if (minX == Double.MAX_VALUE) return MathBoundingBox(0.0, 0.0, 0.0, 0.0)
        return MathBoundingBox(minX, minY, maxX, maxY)
    }

    private fun isRadicalStroke(stroke: Stroke, bounds: MathBoundingBox): Boolean {
        val pts = stroke.points
        if (pts.size < 4) return false
        var hasDrop = false
        var hasRise = false
        for (i in 1 until pts.size) {
            val dy = pts[i].y - pts[i - 1].y
            if (dy > 2.5) hasDrop = true
            if (dy < -2.5 && hasDrop) hasRise = true
        }
        val lastPt = pts.last()
        val hasOverbar = lastPt.x > bounds.left + bounds.width * 0.50 && lastPt.y <= bounds.top + bounds.height * 0.45
        return hasDrop && hasRise && (bounds.width > 10.0) && hasOverbar
    }

    private fun clusterStrokes(strokes: List<Stroke>): List<StrokeCluster> {
        if (strokes.isEmpty()) return emptyList()
        val items = strokes.map { it to strokeBounds(it) }.sortedBy { it.second.left }

        val clusters = mutableListOf<MutableList<Pair<Stroke, MathBoundingBox>>>()

        for (item in items) {
            val itemStroke = item.first
            val itemBounds = item.second
            val isItemRadical = isRadicalStroke(itemStroke, itemBounds)

            var merged = false

            for (c in clusters) {
                val cBounds = computeClusterBounds(c)
                val isCRadical = c.size == 1 && isRadicalStroke(c.first().first, cBounds)

                // Parallel horizontal strokes for '=' (equals sign)
                val isItemH = itemBounds.width / max(1.0, itemBounds.height) > 1.2 && itemBounds.height < 15.0
                val isCH = cBounds.width / max(1.0, cBounds.height) > 1.2 && cBounds.height < 15.0
                if (isItemH && isCH && c.size == 1) {
                    val horizOverlap = min(cBounds.right, itemBounds.right) - max(cBounds.left, itemBounds.left)
                    val maxW = max(cBounds.width, itemBounds.width)
                    val vertGap = abs(itemBounds.centerY - cBounds.centerY)
                    if (horizOverlap > maxW * 0.4 && vertGap < maxW * 1.5 && vertGap > 2.0) {
                        c.add(item)
                        merged = true
                        break
                    }
                }

                // Fraction bar protection
                val isFrac1 = itemBounds.width > 14.0 && itemBounds.height < 10.0 &&
                              (cBounds.bottom < itemBounds.centerY || cBounds.top > itemBounds.centerY)
                val isFrac2 = cBounds.width > 14.0 && cBounds.height < 10.0 &&
                              (itemBounds.bottom < cBounds.centerY || itemBounds.top > cBounds.centerY)

                if (isFrac1 || isFrac2) continue

                // Radical protection: do not merge child strokes underneath radical with the radical stroke
                if (isCRadical || isItemRadical) continue

                // Overlap check for multi-stroke characters (+, x, t, i, =, <=, >=)
                if (cBounds.overlaps(itemBounds, padX = 2.0, padY = 2.0)) {
                    c.add(item)
                    merged = true
                    break
                }
            }

            if (!merged) {
                clusters.add(mutableListOf(item))
            }
        }

        return clusters.map { c ->
            StrokeCluster(strokes = c.map { it.first }, bounds = computeClusterBounds(c))
        }.sortedBy { it.bounds.left }
    }

    private fun computeClusterBounds(items: List<Pair<Stroke, MathBoundingBox>>): MathBoundingBox {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for ((_, b) in items) {
            minX = min(minX, b.left)
            minY = min(minY, b.top)
            maxX = max(maxX, b.right)
            maxY = max(maxY, b.bottom)
        }
        return MathBoundingBox(minX, minY, maxX, maxY)
    }

    // --- Mathematical Symbol Classification ---------------------------------------------------

    private fun classifyStrokeCluster(cluster: StrokeCluster): MathSymbol {
        val strokes = cluster.strokes
        val bounds = cluster.bounds
        val aspect = bounds.width / max(1.0, bounds.height)

        // SINGLE STROKE CLASSIFICATION
        if (strokes.size == 1) {
            val stroke = strokes.first()
            val pts = stroke.points
            if (pts.isEmpty()) {
                return MathSymbol("\\Box", "▢", 0.2, bounds, isUnknown = true, candidates = listOf("?", "x", "1"), originalStrokes = strokes)
            }

            val pStart = pts.first()
            val pEnd = pts.last()
            val startEndDist = hypot(pEnd.x - pStart.x, pEnd.y - pStart.y)
            val pathLen = (1 until pts.size).sumOf { hypot(pts[it].x - pts[it - 1].x, pts[it].y - pts[it - 1].y) }.coerceAtLeast(1.0)
            val linearity = startEndDist / pathLen
            val diag = hypot(bounds.width, bounds.height)

            val maxDevFromChord = pts.maxOfOrNull { pt ->
                val lineDx = pEnd.x - pStart.x
                val lineDy = pEnd.y - pStart.y
                val l2 = lineDx * lineDx + lineDy * lineDy
                if (l2 == 0.0) hypot(pt.x - pStart.x, pt.y - pStart.y)
                else abs((pt.x - pStart.x) * lineDy - (pt.y - pStart.y) * lineDx) / sqrt(l2)
            } ?: 0.0

            // Compute inflections and directional peaks
            var xInflections = 0
            var yInflections = 0
            for (i in 2 until pts.size) {
                val dx1 = pts[i - 1].x - pts[i - 2].x
                val dx2 = pts[i].x - pts[i - 1].x
                if (dx1 * dx2 < -1.0) xInflections++
                val dy1 = pts[i - 1].y - pts[i - 2].y
                val dy2 = pts[i].y - pts[i - 1].y
                if (dy1 * dy2 < -1.0) yInflections++
            }

            // 0. High curvature chaotic scribble detection
            val pathRatio = pathLen / max(1.0, diag)
            if (pathRatio > 2.5 && (xInflections + yInflections) >= 3) {
                return MathSymbol("\\Box", "▢", 0.25, bounds, isUnknown = true, candidates = listOf("?", "x", "y", "\\alpha"), originalStrokes = strokes)
            }

            // 1. Small Dot / Multiplication Dot
            if (diag < 10.0 || (pathLen < 12.0 && diag < 12.0)) {
                return MathSymbol("\\cdot", "·", 0.95, bounds, isOperator = true, candidates = listOf("·", ".", "1"), originalStrokes = strokes)
            }

            // 2. Radical sign \sqrt
            if (isRadicalStroke(stroke, bounds)) {
                return MathSymbol("\\sqrt", "√", 0.94, bounds, isOperator = true, isRadical = true, candidates = listOf("\\sqrt", "v"), originalStrokes = strokes)
            }

            // 3. Fraction Bar (horizontal line)
            if (aspect > 2.0 && bounds.height < 14.0 && linearity > 0.82) {
                return MathSymbol("-", "-", 0.96, bounds, isOperator = true, isFractionBar = true, candidates = listOf("-", "_", "\\frac{}{}"), originalStrokes = strokes)
            }

            // 4. Horizontal Minus sign
            if (aspect > 1.6 && bounds.height < 16.0 && linearity > 0.80) {
                return MathSymbol("-", "-", 0.95, bounds, isOperator = true, candidates = listOf("-", "_"), originalStrokes = strokes)
            }

            // 5. Integral sign \int (tall S-shaped curve with curvature)
            if (aspect < 0.50 && bounds.height > 18.0 && maxDevFromChord > 2.0) {
                var yDescending = 0
                for (i in 1 until pts.size) {
                    if (pts[i].y > pts[i - 1].y) yDescending++
                }
                if (yDescending > pts.size * 0.50) {
                    return MathSymbol("\\int", "∫", 0.94, bounds, isOperator = true, isIntegral = true, candidates = listOf("\\int", "S", "1"), originalStrokes = strokes)
                }
            }

            // 6. Vertical line: 1, |, l
            if (aspect < 0.35 && bounds.height > 10.0 && linearity > 0.82 && maxDevFromChord <= 2.5) {
                return MathSymbol("1", "1", 0.94, bounds, candidates = listOf("1", "|", "l", "/"), originalStrokes = strokes)
            }

            // 7. Diagonal Slash / division sign /
            if (aspect in 0.35..1.6 && linearity > 0.85) {
                val isSlashDirection = (pStart.x < pEnd.x && pStart.y > pEnd.y) || (pStart.x > pEnd.x && pStart.y < pEnd.y)
                if (isSlashDirection) {
                    return MathSymbol("/", "/", 0.95, bounds, isOperator = true, isSlash = true, candidates = listOf("/", "1", "\\div"), originalStrokes = strokes)
                }
            }

            // 8. Closed Loop: 0, O, \theta, \infty, 8, \sigma
            if (diag > 8.0 && (startEndDist / max(1.0, diag)) < 0.35) {
                if (aspect > 1.4) {
                    return MathSymbol("\\infty", "∞", 0.92, bounds, candidates = listOf("∞", "\\alpha", "0"), originalStrokes = strokes)
                }
                if (aspect in 0.6..1.3) {
                    return MathSymbol("0", "0", 0.94, bounds, candidates = listOf("0", "O", "\\theta", "\\sigma"), originalStrokes = strokes)
                }
            }

            // 9. Greek \alpha / letter a: loop starting top-right, looping down-left, crossing and finishing bottom-right
            if (aspect in 0.5..1.6 && pts.size > 5) {
                val pStartRight = pStart.x > bounds.left + bounds.width * 0.40
                val pEndRight = pEnd.x > bounds.left + bounds.width * 0.40
                val reachesLeft = pts.any { it.x < bounds.left + bounds.width * 0.35 }
                val startDescends = pts.size >= 2 && pts[1].y > pStart.y + 2.0
                if (pStartRight && pEndRight && reachesLeft && startDescends && startEndDist > 4.0) {
                    return MathSymbol("\\alpha", "α", 0.92, bounds, candidates = listOf("\\alpha", "a", "\\sigma", "x"), originalStrokes = strokes)
                }
            }

            // 10. Greek \Sigma / \sum: zig-zag with 2+ inflections, starting top-right and ending bot-right
            if (xInflections >= 2 && aspect in 0.4..1.4 && pStart.x > bounds.left + bounds.width * 0.35 && pEnd.x > bounds.left + bounds.width * 0.35) {
                return MathSymbol("\\sum", "∑", 0.92, bounds, isOperator = true, candidates = listOf("\\sum", "\\Sigma", "E", "3"), originalStrokes = strokes)
            }

            // 11. Letter 'd' (calculus derivative d/dx, dx, dy): loop on bottom-left, stem rises on right
            val minYPt = pts.minByOrNull { it.y } ?: pStart
            val maxYPt = pts.maxByOrNull { it.y } ?: pEnd
            if (aspect in 0.4..1.2 && bounds.height > 10.0) {
                val topPeakX = minYPt.x
                val isRightAscender = topPeakX > bounds.left + bounds.width * 0.45
                val loopOnLeft = pts.any { it.x < bounds.left + bounds.width * 0.35 && it.y > bounds.centerY }
                if (isRightAscender && loopOnLeft && pEnd.y > bounds.centerY && pStart.y > bounds.top + bounds.height * 0.25) {
                    return MathSymbol("d", "d", 0.92, bounds, candidates = listOf("d", "a", "\\partial", "\\alpha", "6"), originalStrokes = strokes)
                }
            }

            // 12. Letter 'b' or Greek \beta: starts top, goes down, then right lobe(s)
            if (pStart.y < bounds.top + bounds.height * 0.35 && pStart.x < bounds.left + bounds.width * 0.45 && aspect in 0.4..1.1) {
                if (bounds.height > 14.0) {
                    return MathSymbol("\\beta", "β", 0.90, bounds, candidates = listOf("\\beta", "b", "B", "13"), originalStrokes = strokes)
                }
            }

            // 12. Greek \alpha / letter a: loop with crossing tail on right
            if (aspect in 0.6..1.5 && pts.size > 5) {
                val lastQuarterX = pts.takeLast(max(1, pts.size / 4)).map { it.x }.average()
                if (lastQuarterX > bounds.centerX && startEndDist > 4.0) {
                    return MathSymbol("\\alpha", "α", 0.90, bounds, candidates = listOf("\\alpha", "a", "\\sigma", "x"), originalStrokes = strokes)
                }
            }

            // 13. Greater Than '>' & Less Than '<'
            if (linearity < 0.75 && xInflections <= 1 && yInflections <= 1) {
                val maxPt = pts.maxByOrNull { it.x } ?: pStart
                val minPt = pts.minByOrNull { it.x } ?: pStart
                if (maxPt.x > pStart.x + 4.0 && maxPt.x > pEnd.x + 4.0 && aspect in 0.4..1.6) {
                    return MathSymbol(">", ">", 0.92, bounds, isOperator = true, candidates = listOf(">", "7", ")"), originalStrokes = strokes)
                }
                if (minPt.x < pStart.x - 4.0 && minPt.x < pEnd.x - 4.0 && aspect in 0.4..1.6) {
                    return MathSymbol("<", "<", 0.92, bounds, isOperator = true, candidates = listOf("<", "c", "("), originalStrokes = strokes)
                }
            }

            // 14. Number '2' / 'z': top curve, diagonal down-left, horizontal base
            if (pStart.x < pEnd.x && pStart.y < pEnd.y && aspect in 0.5..1.2) {
                val isBaseHorizontal = abs(pEnd.y - maxYPt.y) < 4.0 && pEnd.x > bounds.left + bounds.width * 0.60
                if (isBaseHorizontal) {
                    return MathSymbol("2", "2", 0.90, bounds, candidates = listOf("2", "z", "Z"), originalStrokes = strokes)
                }
            }

            // 15. Number '3': two rightward lobes
            if (xInflections >= 2 && pStart.y < pEnd.y && aspect in 0.4..1.1) {
                return MathSymbol("3", "3", 0.90, bounds, candidates = listOf("3", "\\beta", "8", "E"), originalStrokes = strokes)
            }

            // 16. Number '7': top horizontal + diagonal down
            if (pStart.y < bounds.top + bounds.height * 0.30 && pEnd.y > bounds.bottom - bounds.height * 0.25 && aspect in 0.5..1.1) {
                return MathSymbol("7", "7", 0.88, bounds, candidates = listOf("7", ">", "1", "z"), originalStrokes = strokes)
            }

            // 17. Parentheses: (, )
            if (aspect in 0.18..0.60 && bounds.height > 12.0) {
                val midPt = pts[pts.size / 2]
                if (midPt.x < pStart.x - 2.0 && midPt.x < pEnd.x - 2.0) {
                    return MathSymbol("(", "(", 0.92, bounds, isDelimiter = true, candidates = listOf("(", "[", "c", "1"), originalStrokes = strokes)
                } else if (midPt.x > pStart.x + 2.0 && midPt.x > pEnd.x + 2.0) {
                    return MathSymbol(")", ")", 0.92, bounds, isDelimiter = true, candidates = listOf(")", "]", "1"), originalStrokes = strokes)
                }
            }

            // 18. Letter 'C' / 'c': open right curve
            if (pStart.x > bounds.left + bounds.width * 0.40 && pEnd.x > bounds.left + bounds.width * 0.40 && aspect in 0.5..1.3 && xInflections <= 1 && yInflections <= 1) {
                val isCapital = bounds.height > 16.0
                val sym = if (isCapital) "C" else "c"
                return MathSymbol(sym, sym, 0.90, bounds, candidates = listOf(sym, "(", "\\subset"), originalStrokes = strokes)
            }

            // 19. Letter 's' / 'S'
            if (xInflections >= 1 && yInflections <= 2 && aspect in 0.4..1.1 && pStart.y < pEnd.y) {
                return MathSymbol("s", "s", 0.88, bounds, candidates = listOf("s", "5", "8", "\\int"), originalStrokes = strokes)
            }

            // 20. Letter 'v', 'u', 'w' (smooth valley with no chaotic self-crossing)
            val midY = pts[pts.size / 2].y
            if (midY > pStart.y && midY > pEnd.y && aspect in 0.5..1.6 && xInflections <= 1 && yInflections <= 1) {
                return MathSymbol("v", "v", 0.88, bounds, candidates = listOf("v", "u", "w", "\\nu"), originalStrokes = strokes)
            }

            // 21. Letter 'y': starts top, ends below baseline on left or right
            if (pEnd.y > bounds.bottom - 2.0 && aspect in 0.5..1.2 && xInflections <= 2) {
                return MathSymbol("y", "y", 0.88, bounds, candidates = listOf("y", "g", "v", "u"), originalStrokes = strokes)
            }

            // 22. Letter 'x' (single cursive stroke)
            if (aspect in 0.6..1.4 && xInflections in 1..2 && yInflections in 1..2) {
                return MathSymbol("x", "x", 0.85, bounds, candidates = listOf("x", "\\alpha", "t", "z"), originalStrokes = strokes)
            }

            // Fallback for single unknown stroke: preserve original strokes, mark low confidence
            return MathSymbol(
                latex = "\\Box",
                unicode = "▢",
                confidence = 0.35,
                bounds = bounds,
                isUnknown = true,
                candidates = listOf("x", "y", "d", "\\alpha", "1", "z", "+", "t", "C"),
                originalStrokes = strokes
            )
        }

        // TWO STROKES CLASSIFICATION (+, =, x, t, i, \le, \ge, \pm, \neq, \sqrt)
        if (strokes.size == 2) {
            val b1 = strokeBounds(strokes[0])
            val b2 = strokeBounds(strokes[1])

            val isH1 = b1.width / max(1.0, b1.height) > 1.2
            val isV1 = b1.height / max(1.0, b1.width) > 1.2
            val isH2 = b2.width / max(1.0, b2.height) > 1.2
            val isV2 = b2.height / max(1.0, b2.width) > 1.2

            // Plus '+': intersecting horizontal & vertical
            if ((isH1 && isV2) || (isV1 && isH2)) {
                return MathSymbol("+", "+", 0.96, bounds, isOperator = true, candidates = listOf("+", "t", "\\dagger"), originalStrokes = strokes)
            }

            // Equals '=': two parallel horizontal strokes
            if (isH1 && isH2) {
                return MathSymbol("=", "=", 0.96, bounds, isOperator = true, candidates = listOf("=", "\\approx", "\\equiv"), originalStrokes = strokes)
            }

            // Greater or Equal '>=' / Less or Equal '<=': angle + horizontal line underneath
            if ((isH1 && !isH2) || (isH2 && !isH1)) {
                val angleB = if (isH1) b2 else b1
                val lineB = if (isH1) b1 else b2
                if (lineB.top >= angleB.centerY) {
                    val isGreater = angleB.right > angleB.left + angleB.width * 0.70
                    val sym = if (isGreater) "\\ge" else "\\le"
                    val uni = if (isGreater) "≥" else "≤"
                    return MathSymbol(sym, uni, 0.92, bounds, isOperator = true, candidates = listOf(sym, "=", ">", "<"), originalStrokes = strokes)
                }
            }

            // Plus-Minus '\pm': plus above minus
            if ((isH1 && isV1) || (isH2 && isV2)) {
                return MathSymbol("\\pm", "±", 0.92, bounds, isOperator = true, candidates = listOf("\\pm", "+", "-"), originalStrokes = strokes)
            }

            // Cross 'x' or multiplication \times: two intersecting diagonal strokes
            if (b1.overlaps(b2, 2.0, 2.0)) {
                return MathSymbol("x", "x", 0.92, bounds, candidates = listOf("x", "\\times", "X", "+"), originalStrokes = strokes)
            }

            // Radical with separate overbar
            if (b1.left < b2.left && isH2) {
                return MathSymbol("\\sqrt", "√", 0.94, bounds, isOperator = true, isRadical = true, candidates = listOf("\\sqrt"), originalStrokes = strokes)
            }

            // Letter 't'
            if ((isV1 && isH2) || (isV2 && isH1)) {
                return MathSymbol("t", "t", 0.90, bounds, candidates = listOf("t", "+", "T"), originalStrokes = strokes)
            }

            // Letter 'i' or 'j' with dot
            val topS = if (b1.top < b2.top) b1 else b2
            val botS = if (b1.top < b2.top) b2 else b1
            if (topS.height < 12.0 && botS.height > 10.0) {
                return MathSymbol("i", "i", 0.92, bounds, candidates = listOf("i", "j", ";", "!"), originalStrokes = strokes)
            }

            // Fallback for 2-stroke cluster
            return MathSymbol(
                latex = "\\Box",
                unicode = "▢",
                confidence = 0.35,
                bounds = bounds,
                isUnknown = true,
                candidates = listOf("x", "+", "=", "t", "y", "\\le", "\\ge"),
                originalStrokes = strokes
            )
        }

        // THREE STROKES CLASSIFICATION (\pi, \sum, \neq, \Delta, A, H)
        if (strokes.size == 3) {
            val bList = strokes.map { strokeBounds(it) }
            val hCount = bList.count { it.width / max(1.0, it.height) > 1.2 }
            val vCount = bList.count { it.height / max(1.0, it.width) > 1.2 }

            // Greek \pi: horizontal top bar + 2 vertical legs
            if (hCount >= 1 && vCount >= 2) {
                return MathSymbol("\\pi", "π", 0.94, bounds, candidates = listOf("\\pi", "\\Pi", "n", "H"), originalStrokes = strokes)
            }

            // Not equal \neq: equals + diagonal slash
            if (hCount >= 2) {
                return MathSymbol("\\neq", "≠", 0.94, bounds, isOperator = true, candidates = listOf("\\neq", "="), originalStrokes = strokes)
            }

            // Delta \Delta: triangle shape
            if (bList.all { it.width > 4.0 }) {
                return MathSymbol("\\Delta", "Δ", 0.90, bounds, candidates = listOf("\\Delta", "A", "\\sum"), originalStrokes = strokes)
            }

            // Summation \sum (Sigma)
            return MathSymbol("\\sum", "∑", 0.92, bounds, isOperator = true, isSummation = true, candidates = listOf("\\sum", "E", "Z"), originalStrokes = strokes)
        }

        return MathSymbol(
            latex = "\\Box",
            unicode = "▢",
            confidence = 0.30,
            bounds = bounds,
            isUnknown = true,
            candidates = listOf("\\sum", "\\pi", "\\Delta", "\\Omega", "M"),
            originalStrokes = strokes
        )
    }

    // --- Spatial 2D Layout AST Builder ---------------------------------------------------------

    private sealed interface SpatialMathNode {
        fun toLatex(): String
        fun toUnicode(): String
    }

    private data class LeafMathNode(val symbol: MathSymbol) : SpatialMathNode {
        override fun toLatex(): String = symbol.latex
        override fun toUnicode(): String = symbol.unicode
    }

    private data class FractionMathNode(
        val numerator: SpatialMathNode,
        val denominator: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String = "\\frac{${numerator.toLatex()}}{${denominator.toLatex()}}"
        override fun toUnicode(): String = "(${numerator.toUnicode()} / ${denominator.toUnicode()})"
    }

    private data class PowerMathNode(
        val base: SpatialMathNode,
        val exponent: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String = "${base.toLatex()}^{${exponent.toLatex()}}"
        override fun toUnicode(): String = "${base.toUnicode()}^(${exponent.toUnicode()})"
    }

    private data class SubscriptMathNode(
        val base: SpatialMathNode,
        val subscript: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String = "${base.toLatex()}_{${subscript.toLatex()}}"
        override fun toUnicode(): String = "${base.toUnicode()}_(${subscript.toUnicode()})"
    }

    private data class RadicalMathNode(val content: SpatialMathNode) : SpatialMathNode {
        override fun toLatex(): String = "\\sqrt{${content.toLatex()}}"
        override fun toUnicode(): String = "√(${content.toUnicode()})"
    }

    private data class IntegralMathNode(
        val lowerBound: SpatialMathNode?,
        val upperBound: SpatialMathNode?,
        val integrand: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String {
            val boundsStr = when {
                lowerBound != null && upperBound != null -> "_{${lowerBound.toLatex()}}^{${upperBound.toLatex()}}"
                lowerBound != null -> "_{${lowerBound.toLatex()}}"
                upperBound != null -> "^{${upperBound.toLatex()}}"
                else -> ""
            }
            return "\\int$boundsStr ${integrand.toLatex()}"
        }

        override fun toUnicode(): String {
            val boundsStr = when {
                lowerBound != null && upperBound != null -> "[${lowerBound.toUnicode()} to ${upperBound.toUnicode()}]"
                else -> ""
            }
            return "∫ $boundsStr ${integrand.toUnicode()}"
        }
    }

    private data class SequenceMathNode(val items: List<SpatialMathNode>) : SpatialMathNode {
        override fun toLatex(): String = items.joinToString(" ") { it.toLatex() }
        override fun toUnicode(): String = items.joinToString(" ") { it.toUnicode() }
    }

    private fun parseSpatialMath(symbols: List<MathSymbol>): SpatialMathNode {
        if (symbols.isEmpty()) return LeafMathNode(MathSymbol("", "", 1.0, MathBoundingBox(0.0, 0.0, 0.0, 0.0)))
        if (symbols.size == 1) return LeafMathNode(symbols.first())

        // 1. Detect Fraction Bars (horizontal line separating symbols above and below)
        val fractionBar = symbols.firstOrNull { it.isFractionBar }
        if (fractionBar != null) {
            val barB = fractionBar.bounds
            val above = symbols.filter { it !== fractionBar && it.bounds.bottom <= barB.centerY + 6.0 && it.bounds.right >= barB.left - 8.0 && it.bounds.left <= barB.right + 8.0 }
            val below = symbols.filter { it !== fractionBar && it.bounds.top >= barB.centerY - 6.0 && it.bounds.right >= barB.left - 8.0 && it.bounds.left <= barB.right + 8.0 }

            if (above.isNotEmpty() && below.isNotEmpty()) {
                val leftSymbols = symbols.filter { it !== fractionBar && !above.contains(it) && !below.contains(it) && it.bounds.right < barB.left }
                val rightSymbols = symbols.filter { it !== fractionBar && !above.contains(it) && !below.contains(it) && it.bounds.left > barB.right }

                val fracNode = FractionMathNode(
                    numerator = parseSpatialMath(above),
                    denominator = parseSpatialMath(below)
                )

                val resultList = mutableListOf<SpatialMathNode>()
                if (leftSymbols.isNotEmpty()) resultList.add(parseSpatialMath(leftSymbols))
                resultList.add(fracNode)
                if (rightSymbols.isNotEmpty()) resultList.add(parseSpatialMath(rightSymbols))
                return if (resultList.size == 1) resultList.first() else SequenceMathNode(resultList)
            }
        }

        // 2. Detect Radicals / Square Roots
        val radical = symbols.firstOrNull { it.isRadical }
        if (radical != null) {
            val radB = radical.bounds
            val inside = symbols.filter { it !== radical && it.bounds.left >= radB.left + 4.0 && it.bounds.right <= radB.right + 30.0 && it.bounds.top >= radB.top - 6.0 }
            if (inside.isNotEmpty()) {
                val radNode = RadicalMathNode(parseSpatialMath(inside))
                val before = symbols.filter { it !== radical && it.bounds.right < radB.left }
                val after = symbols.filter { it !== radical && !inside.contains(it) && it.bounds.left > radB.right }

                val resultList = mutableListOf<SpatialMathNode>()
                if (before.isNotEmpty()) resultList.add(parseSpatialMath(before))
                resultList.add(radNode)
                if (after.isNotEmpty()) resultList.add(parseSpatialMath(after))
                return if (resultList.size == 1) resultList.first() else SequenceMathNode(resultList)
            }
        }

        // 3. Detect Integrals with Bounds
        val integral = symbols.firstOrNull { it.isIntegral }
        if (integral != null) {
            val intB = integral.bounds
            val lower = symbols.filter { it !== integral && it.bounds.top >= intB.centerY && it.bounds.left in (intB.left - 6.0)..(intB.right + 18.0) }
            val upper = symbols.filter { it !== integral && it.bounds.bottom <= intB.centerY && it.bounds.left in (intB.left - 6.0)..(intB.right + 18.0) }
            val integrand = symbols.filter { it !== integral && !lower.contains(it) && !upper.contains(it) && it.bounds.left > intB.right }

            if (integrand.isNotEmpty()) {
                val lowerNode = if (lower.isNotEmpty()) parseSpatialMath(lower) else null
                val upperNode = if (upper.isNotEmpty()) parseSpatialMath(upper) else null
                val intNode = IntegralMathNode(lowerNode, upperNode, parseSpatialMath(integrand))

                val before = symbols.filter { it !== integral && it.bounds.right < intB.left }
                val resultList = mutableListOf<SpatialMathNode>()
                if (before.isNotEmpty()) resultList.add(parseSpatialMath(before))
                resultList.add(intNode)
                return if (resultList.size == 1) resultList.first() else SequenceMathNode(resultList)
            }
        }

        // 4. Sequential Stream with Powers / Superscripts and Subscripts
        val nodes = mutableListOf<SpatialMathNode>()
        var i = 0
        while (i < symbols.size) {
            val current = symbols[i]
            var currentNode: SpatialMathNode = LeafMathNode(current)

            // Lookahead for Superscript (elevated smaller character to top-right)
            if (i + 1 < symbols.size) {
                val next = symbols[i + 1]
                val isSuperscript = next.bounds.top < current.bounds.centerY &&
                                    next.bounds.bottom < current.bounds.bottom &&
                                    next.bounds.height <= current.bounds.height * 0.95 &&
                                    next.bounds.left >= current.bounds.right - 10.0

                val isSubscript = next.bounds.top > current.bounds.centerY &&
                                  next.bounds.bottom > current.bounds.bottom &&
                                  next.bounds.height <= current.bounds.height * 0.95 &&
                                  next.bounds.left >= current.bounds.right - 10.0

                if (isSuperscript) {
                    val expNode = LeafMathNode(next)
                    currentNode = PowerMathNode(currentNode, expNode)
                    i += 2
                    nodes.add(currentNode)
                    continue
                } else if (isSubscript) {
                    val subNode = LeafMathNode(next)
                    currentNode = SubscriptMathNode(currentNode, subNode)
                    i += 2
                    nodes.add(currentNode)
                    continue
                }
            }

            nodes.add(currentNode)
            i++
        }

        return if (nodes.size == 1) nodes.first() else SequenceMathNode(nodes)
    }
}
