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

    private fun isAngleStroke(stroke: Stroke): Boolean {
        val pts = stroke.points
        if (pts.size < 3) return false
        val pStart = pts.first()
        val pEnd = pts.last()
        val b = strokeBounds(stroke)
        val diag = hypot(b.width, b.height)
        val startEndDist = hypot(pEnd.x - pStart.x, pEnd.y - pStart.y)
        if (startEndDist / max(1.0, diag) < 0.40) return false // Closed loop or circle is NOT an angle
        val minX = pts.minOf { it.x }
        val maxX = pts.maxOf { it.x }
        return (minX < pStart.x - 3.0 && minX < pEnd.x - 3.0) ||
               (maxX > pStart.x + 3.0 && maxX > pEnd.x + 3.0)
    }

    private fun clusterStrokes(strokes: List<Stroke>): List<StrokeCluster> {
        if (strokes.isEmpty()) return emptyList()
        val items = strokes.map { it to strokeBounds(it) }.sortedBy { it.second.left }

        val clusters = mutableListOf<MutableList<Pair<Stroke, MathBoundingBox>>>()

        for (item in items) {
            val itemStroke = item.first
            val itemBounds = item.second
            val isItemRadical = isRadicalStroke(itemStroke, itemBounds)
            val isItemAngle = isAngleStroke(itemStroke)
            val isItemH = itemBounds.width / max(1.0, itemBounds.height) > 1.2 && itemBounds.height < 15.0

            var merged = false

            for (c in clusters) {
                val cBounds = computeClusterBounds(c)
                val isCRadical = c.size == 1 && isRadicalStroke(c.first().first, cBounds)
                val isCAngle = c.size == 1 && isAngleStroke(c.first().first)
                val isCH = cBounds.width / max(1.0, cBounds.height) > 1.2 && cBounds.height < 15.0

                // Inequality '<=' or '>=': angle + horizontal line underneath
                if ((isCAngle && isItemH) || (isItemAngle && isCH)) {
                    val angleB = if (isCAngle) cBounds else itemBounds
                    val lineB = if (isCAngle) itemBounds else cBounds
                    if (lineB.top >= angleB.centerY - 2.0) {
                        c.add(item)
                        merged = true
                        break
                    }
                }

                // Parallel horizontal strokes for '=' (equals sign)
                if (isItemH && isCH && c.size == 1 && !isCAngle && !isItemAngle) {
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
                val isFrac1 = !isCAngle && !isItemAngle && itemBounds.width > 14.0 && itemBounds.height < 10.0 &&
                              (cBounds.bottom < itemBounds.centerY || cBounds.top > itemBounds.centerY)
                val isFrac2 = !isCAngle && !isItemAngle && cBounds.width > 14.0 && cBounds.height < 10.0 &&
                              (itemBounds.bottom < cBounds.centerY || itemBounds.top > cBounds.centerY)

                if (isFrac1 || isFrac2) continue

                // Radical protection: do not merge child strokes underneath radical with the radical stroke
                if (isCRadical || isItemRadical) continue

                // Overlap check for multi-stroke characters (+, x, t, i, =, <=, >=)
                val isTinyDot = min(cBounds.height, itemBounds.height) < 7.0
                val actualYOverlap = min(cBounds.bottom, itemBounds.bottom) >= max(cBounds.top, itemBounds.top) - 2.0
                if (cBounds.overlaps(itemBounds, padX = 3.0, padY = if (isTinyDot) 6.0 else 2.0)) {
                    // Two substantial strokes stacked vertically without Y overlap (like num and den) should not merge
                    if (!isTinyDot && !actualYOverlap) continue
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

        if (strokes.isEmpty()) {
            return MathSymbol("\\Box", "▢", 0.2, bounds, isUnknown = true, candidates = listOf("?", "1", "x"), originalStrokes = strokes)
        }

        val allPts = strokes.flatMap { it.points }
        if (allPts.isEmpty()) {
            return MathSymbol("\\Box", "▢", 0.2, bounds, isUnknown = true, candidates = listOf("?", "1", "x"), originalStrokes = strokes)
        }

        val pStart = allPts.first()
        val pEnd = allPts.last()
        val startEndDist = hypot(pEnd.x - pStart.x, pEnd.y - pStart.y)
        val diag = hypot(bounds.width, bounds.height)

        val features = SymbolFeatureExtractor.extract(strokes)
        val pathLen = features.rawPathLength

        // 0. High curvature chaotic scribble detection
        val pathRatio = pathLen / max(1.0, diag)
        if (pathRatio > 2.5 && (features.xInflections + features.yInflections) >= 3) {
            return MathSymbol("\\Box", "▢", 0.25, bounds, isUnknown = true, candidates = listOf("?", "x", "y", "\\alpha"), originalStrokes = strokes)
        }

        // 1. Small Dot / Multiplication Dot (fast structural anchor)
        if (diag < 10.0 || (pathLen < 12.0 && diag < 12.0)) {
            return MathSymbol("\\cdot", "·", 0.95, bounds, isOperator = true, candidates = listOf("·", ".", "1"), originalStrokes = strokes)
        }

        // 2. Radical sign \sqrt (special stroke structure anchor)
        if (strokes.size in 1..2 && (isRadicalStroke(strokes.first(), bounds) || (strokes.size == 2 && isRadicalStroke(strokes[0], strokeBounds(strokes[0]))))) {
            return MathSymbol("\\sqrt", "√", 0.94, bounds, isOperator = true, isRadical = true, candidates = listOf("\\sqrt", "v"), originalStrokes = strokes)
        }

        // 3. Fraction Bar / Minus sign anchor for very flat strokes
        if (strokes.size == 1 && aspect > 1.6 && bounds.height < 16.0 && features.linearity > 0.78) {
            val isFrac = aspect > 2.0 && bounds.height < 14.0
            return MathSymbol(
                latex = "-",
                unicode = "-",
                confidence = 0.96,
                bounds = bounds,
                isOperator = true,
                isFractionBar = isFrac,
                candidates = if (isFrac) listOf("-", "_", "\\frac{}{}") else listOf("-", "_"),
                originalStrokes = strokes
            )
        }

        // 4. Parentheses (, ) and Integral \int by Chord Deviation analysis
        if (strokes.size == 1 && aspect in 0.05..0.60 && bounds.height > 10.0 && allPts.size >= 3) {
            val chordDx = pEnd.x - pStart.x
            val chordDy = pEnd.y - pStart.y
            val chordLen = hypot(chordDx, chordDy)
            if (chordLen > 1.0) {
                // Signed deviation (positive = right, negative = left)
                val devs = allPts.map { p ->
                    ((p.x - pStart.x) * chordDy - (p.y - pStart.y) * chordDx) / chordLen
                }
                val minDev = devs.minOf { it }
                val maxDev = devs.maxOf { it }

                // S-curve deviating to both left and right: \int
                if (minDev < -1.5 && maxDev > 1.5 && bounds.height > 18.0) {
                    return MathSymbol("\\int", "∫", 0.94, bounds, isOperator = true, isIntegral = true, candidates = listOf("\\int", "S", "1"), originalStrokes = strokes)
                }
                // Arc purely to the left: open paren '('
                if (maxDev < 1.0 && minDev < -1.5) {
                    return MathSymbol("(", "(", 0.94, bounds, isDelimiter = true, candidates = listOf("(", "[", "c", "1"), originalStrokes = strokes)
                }
                // Arc purely to the right: close paren ')'
                if (minDev > -1.0 && maxDev > 1.5) {
                    return MathSymbol(")", ")", 0.94, bounds, isDelimiter = true, candidates = listOf(")", "]", "1"), originalStrokes = strokes)
                }
            }
        }

        // 5. Vertical line: 1, |, l
        if (strokes.size == 1 && aspect < 0.30 && bounds.height > 10.0 && features.linearity > 0.82 && features.maxDeviationNorm < 0.06) {
            return MathSymbol("1", "1", 0.94, bounds, candidates = listOf("1", "|", "l", "/"), originalStrokes = strokes)
        }

        // 6. Closed Loop: 0, O, \theta, \infty, 8, \sigma
        if (strokes.size == 1 && diag > 8.0 && (startEndDist / max(1.0, diag)) < 0.35) {
            if (aspect > 1.35) {
                return MathSymbol("\\infty", "∞", 0.92, bounds, candidates = listOf("∞", "\\alpha", "0"), originalStrokes = strokes)
            }
            if (features.selfCrossings >= 1 && aspect in 0.5..1.0) {
                return MathSymbol("8", "8", 0.92, bounds, candidates = listOf("8", "0", "\\theta"), originalStrokes = strokes)
            }
            if (aspect in 0.55..1.35) {
                return MathSymbol("0", "0", 0.94, bounds, candidates = listOf("0", "O", "\\theta", "\\sigma"), originalStrokes = strokes)
            }
        }

        // 7. Greek \alpha / letter a: loop with crossing tail on right
        if (strokes.size == 1 && aspect in 0.5..1.6 && allPts.size > 5) {
            val pStartRight = pStart.x > bounds.left + bounds.width * 0.40
            val pEndRight = pEnd.x > bounds.left + bounds.width * 0.40
            val reachesLeft = allPts.any { it.x < bounds.left + bounds.width * 0.35 }
            val startDescends = allPts.size >= 2 && allPts[1].y > pStart.y + 2.0
            if (pStartRight && pEndRight && reachesLeft && startDescends && startEndDist > 4.0) {
                return MathSymbol("\\alpha", "α", 0.92, bounds, candidates = listOf("\\alpha", "a", "\\sigma", "x"), originalStrokes = strokes)
            }
        }

        // 8. Two-stroke structural symbols: =, +, <=, >=, \neq, d, x (checked before general templates)
        if (strokes.size == 2) {
            val b1 = strokeBounds(strokes[0])
            val b2 = strokeBounds(strokes[1])
            val isH1 = b1.width / max(1.0, b1.height) > 1.2
            val isH2 = b2.width / max(1.0, b2.height) > 1.2
            val isV1 = b1.height / max(1.0, b1.width) > 1.2
            val isV2 = b2.height / max(1.0, b2.width) > 1.2

            // Equals '=': two parallel horizontal strokes
            if (isH1 && isH2) {
                return MathSymbol("=", "=", 0.96, bounds, isOperator = true, candidates = listOf("=", "\\approx", "\\equiv"), originalStrokes = strokes)
            }
            // Plus '+': intersecting horizontal & vertical
            if ((isH1 && isV2) || (isV1 && isH2)) {
                return MathSymbol("+", "+", 0.96, bounds, isOperator = true, candidates = listOf("+", "t", "\\dagger"), originalStrokes = strokes)
            }
            // Letter 'd': left loop/circle + right vertical ascender
            val hasLeftLoop = (b1.left < b2.left && b1.centerY > b2.top) || (b2.left < b1.left && b2.centerY > b1.top)
            val hasRightAscender = (b1.right > b2.centerX && isV1 && b1.top < b2.centerY) || (b2.right > b1.centerX && isV2 && b2.top < b1.centerY)
            if (hasLeftLoop && hasRightAscender) {
                return MathSymbol("d", "d", 0.94, bounds, candidates = listOf("d", "a", "\\partial"), originalStrokes = strokes)
            }
            // Greater/Less Equal >= / <=
            if ((isH1 && !isH2) || (isH2 && !isH1)) {
                val angleB = if (isH1) b2 else b1
                val lineB = if (isH1) b1 else b2
                if (lineB.top >= angleB.centerY - 2.0) {
                    val angleStroke = if (isH1) strokes[1] else strokes[0]
                    val pts = angleStroke.points
                    val minAngleX = pts.minOf { it.x }
                    val maxAngleX = pts.maxOf { it.x }
                    val isGreater = maxAngleX > pts.first().x + 3.0 && maxAngleX > pts.last().x + 3.0
                    val sym = if (isGreater) "\\ge" else "\\le"
                    val uni = if (isGreater) "≥" else "≤"
                    return MathSymbol(sym, uni, 0.94, bounds, isOperator = true, candidates = listOf(sym, "=", ">", "<"), originalStrokes = strokes)
                }
            }
            // Plus-Minus '\pm'
            if ((isH1 && isV1) || (isH2 && isV2)) {
                return MathSymbol("\\pm", "±", 0.92, bounds, isOperator = true, candidates = listOf("\\pm", "+", "-"), originalStrokes = strokes)
            }
            // Cross 'x' or multiplication \times: two overlapping diagonal strokes
            if (b1.overlaps(b2, 3.0, 3.0)) {
                return MathSymbol("x", "x", 0.94, bounds, candidates = listOf("x", "\\times", "X", "+"), originalStrokes = strokes)
            }
        }

        // 9. Greek \Sigma / \sum: zig-zag with 2+ inflections
        if (features.xInflections >= 2 && aspect in 0.4..1.4 && pStart.x > bounds.left + bounds.width * 0.35 && pEnd.x > bounds.left + bounds.width * 0.35) {
            return MathSymbol("\\sum", "∑", 0.92, bounds, isOperator = true, isSummation = true, candidates = listOf("\\sum", "\\Sigma", "E", "3"), originalStrokes = strokes)
        }

        // 9. Three-stroke structural symbols (\pi, \sum, \neq, \Delta)
        if (strokes.size == 3) {
            val bList = strokes.map { strokeBounds(it) }
            val hCount = bList.count { it.width / max(1.0, it.height) > 1.2 }
            val vCount = bList.count { it.height / max(1.0, it.width) > 1.2 }

            if (hCount >= 1 && vCount >= 2) {
                return MathSymbol("\\pi", "π", 0.94, bounds, candidates = listOf("\\pi", "\\Pi", "n", "H"), originalStrokes = strokes)
            }
            if (hCount >= 2) {
                return MathSymbol("\\neq", "≠", 0.94, bounds, isOperator = true, candidates = listOf("\\neq", "="), originalStrokes = strokes)
            }
            if (bList.all { it.width > 4.0 } && aspect in 0.6..1.4) {
                return MathSymbol("\\Delta", "Δ", 0.90, bounds, candidates = listOf("\\Delta", "A", "\\sum"), originalStrokes = strokes)
            }
        }

        // 10. Multi-candidate classification using SymbolTemplateLibrary
        val scoredCandidates = SymbolTemplateLibrary.classify(features, topN = 6, minScore = 0.18)

        if (scoredCandidates.isNotEmpty()) {
            val best = scoredCandidates.first()
            val candidateStrings = scoredCandidates.map { it.template.latex }

            val t = best.template
            val conf = best.score.coerceIn(0.20, 0.96)
            return MathSymbol(
                latex = t.latex,
                unicode = t.unicode,
                confidence = conf,
                bounds = bounds,
                isOperator = t.isOperator,
                isFractionBar = t.isFractionBar,
                isSlash = t.latex == "/",
                isRadical = t.isRadical,
                isIntegral = t.isIntegral,
                isSummation = t.isSummation,
                isDelimiter = t.isDelimiter,
                isUnknown = conf < 0.30,
                candidates = candidateStrings,
                originalStrokes = strokes
            )
        }

        // Fallback: unknown glyph
        return MathSymbol(
            latex = "\\Box",
            unicode = "▢",
            confidence = 0.25,
            bounds = bounds,
            isUnknown = true,
            candidates = listOf("x", "y", "1", "0", "+", "-"),
            originalStrokes = strokes
        )
    }

    // --- Spatial 2D Layout AST Builder ---------------------------------------------------------

    sealed interface SpatialMathNode {
        fun toLatex(): String
        fun toUnicode(): String
    }

    data class LeafMathNode(val symbol: MathSymbol) : SpatialMathNode {
        override fun toLatex(): String = symbol.latex
        override fun toUnicode(): String = symbol.unicode
    }

    data class FractionMathNode(
        val numerator: SpatialMathNode,
        val denominator: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String = "\\frac{${numerator.toLatex()}}{${denominator.toLatex()}}"
        override fun toUnicode(): String = "(${numerator.toUnicode()} / ${denominator.toUnicode()})"
    }

    data class PowerMathNode(
        val base: SpatialMathNode,
        val exponent: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String = "${base.toLatex()}^{${exponent.toLatex()}}"
        override fun toUnicode(): String = "${base.toUnicode()}^(${exponent.toUnicode()})"
    }

    data class SubscriptMathNode(
        val base: SpatialMathNode,
        val subscript: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String = "${base.toLatex()}_{${subscript.toLatex()}}"
        override fun toUnicode(): String = "${base.toUnicode()}_(${subscript.toUnicode()})"
    }

    data class RadicalMathNode(val content: SpatialMathNode) : SpatialMathNode {
        override fun toLatex(): String = "\\sqrt{${content.toLatex()}}"
        override fun toUnicode(): String = "√(${content.toUnicode()})"
    }

    data class IntegralMathNode(
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

    data class SumMathNode(
        val lowerBound: SpatialMathNode?,
        val upperBound: SpatialMathNode?,
        val summand: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String {
            val boundsStr = when {
                lowerBound != null && upperBound != null -> "_{${lowerBound.toLatex()}}^{${upperBound.toLatex()}}"
                lowerBound != null -> "_{${lowerBound.toLatex()}}"
                upperBound != null -> "^{${upperBound.toLatex()}}"
                else -> ""
            }
            return "\\sum$boundsStr ${summand.toLatex()}"
        }

        override fun toUnicode(): String {
            val boundsStr = when {
                lowerBound != null && upperBound != null -> "[${lowerBound.toUnicode()} to ${upperBound.toUnicode()}]"
                else -> ""
            }
            return "∑ $boundsStr ${summand.toUnicode()}"
        }
    }

    data class LimitMathNode(
        val variable: SpatialMathNode?,
        val target: SpatialMathNode?,
        val expression: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String {
            val subStr = if (variable != null && target != null) "_{${variable.toLatex()} \\to ${target.toLatex()}}" else ""
            return "\\lim$subStr ${expression.toLatex()}"
        }

        override fun toUnicode(): String {
            val subStr = if (variable != null && target != null) "(${variable.toUnicode()} -> ${target.toUnicode()})" else ""
            return "lim$subStr ${expression.toUnicode()}"
        }
    }

    data class DerivativeMathNode(
        val order: Int = 1,
        val numVar: String = "",
        val denVar: String = "x",
        val isPartial: Boolean = false
    ) : SpatialMathNode {
        override fun toLatex(): String {
            val d = if (isPartial) "\\partial" else "d"
            val orderExp = if (order > 1) "^$order" else ""
            val num = if (numVar.isEmpty()) "$d$orderExp" else "$d$orderExp $numVar"
            val den = "$d ${denVar}$orderExp"
            return "\\frac{$num}{$den}"
        }

        override fun toUnicode(): String {
            val d = if (isPartial) "∂" else "d"
            val orderExp = if (order > 1) "^$order" else ""
            val num = if (numVar.isEmpty()) "$d$orderExp" else "$d$orderExp $numVar"
            val den = "$d $denVar$orderExp"
            return "($num / $den)"
        }
    }

    data class FunctionMathNode(
        val functionName: String,
        val argument: SpatialMathNode
    ) : SpatialMathNode {
        override fun toLatex(): String = "\\$functionName(${argument.toLatex()})"
        override fun toUnicode(): String = "$functionName(${argument.toUnicode()})"
    }

    data class ParenthesizedMathNode(
        val content: SpatialMathNode,
        val openChar: String = "(",
        val closeChar: String = ")"
    ) : SpatialMathNode {
        override fun toLatex(): String = "\\left$openChar ${content.toLatex()} \\right$closeChar"
        override fun toUnicode(): String = "$openChar${content.toUnicode()}$closeChar"
    }

    data class SequenceMathNode(val items: List<SpatialMathNode>) : SpatialMathNode {
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

        // 4. Detect Summation \sum with Bounds
        val summation = symbols.firstOrNull { it.isSummation || it.latex == "\\sum" || it.latex == "\\Sigma" }
        if (summation != null) {
            val sumB = summation.bounds
            val lower = symbols.filter { it !== summation && it.bounds.top >= sumB.centerY && it.bounds.left in (sumB.left - 6.0)..(sumB.right + 18.0) }
            val upper = symbols.filter { it !== summation && it.bounds.bottom <= sumB.centerY && it.bounds.left in (sumB.left - 6.0)..(sumB.right + 18.0) }
            val summand = symbols.filter { it !== summation && !lower.contains(it) && !upper.contains(it) && it.bounds.left > sumB.right }

            if (summand.isNotEmpty()) {
                val lowerNode = if (lower.isNotEmpty()) parseSpatialMath(lower) else null
                val upperNode = if (upper.isNotEmpty()) parseSpatialMath(upper) else null
                val sumNode = SumMathNode(lowerNode, upperNode, parseSpatialMath(summand))

                val before = symbols.filter { it !== summation && it.bounds.right < sumB.left }
                val resultList = mutableListOf<SpatialMathNode>()
                if (before.isNotEmpty()) resultList.add(parseSpatialMath(before))
                resultList.add(sumNode)
                return if (resultList.size == 1) resultList.first() else SequenceMathNode(resultList)
            }
        }

        // 5. Detect Parentheses Pairs
        val openParenIdx = symbols.indexOfFirst { it.latex == "(" }
        if (openParenIdx != -1) {
            val closeParenIdx = symbols.indexOfLast { it.latex == ")" }
            if (closeParenIdx > openParenIdx) {
                val before = symbols.subList(0, openParenIdx)
                val inside = symbols.subList(openParenIdx + 1, closeParenIdx)
                val after = symbols.subList(closeParenIdx + 1, symbols.size)

                val parenNode = ParenthesizedMathNode(parseSpatialMath(inside))
                val resultList = mutableListOf<SpatialMathNode>()
                if (before.isNotEmpty()) resultList.add(parseSpatialMath(before))
                resultList.add(parenNode)
                if (after.isNotEmpty()) resultList.add(parseSpatialMath(after))
                return if (resultList.size == 1) resultList.first() else SequenceMathNode(resultList)
            }
        }

        // 6. Sequential Stream with Powers / Superscripts and Subscripts
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
