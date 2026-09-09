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

    fun overlaps(other: MathBoundingBox, padX: Double = 1.0, padY: Double = 1.0): Boolean {
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
    val isRadical: Boolean = false,
    val isIntegral: Boolean = false,
    val isSummation: Boolean = false,
    val isDelimiter: Boolean = false,
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
    val originalStrokes: List<Stroke>
)

/**
 * 100% Offline, interactive Handwriting Math Recognition Engine for STEM tablets.
 * Transforms freehand stylus math strokes into professional LaTeX & typography formulas.
 */
object MathHandwritingEngine {

    /**
     * Recognizes a list of strokes drawn on the canvas and returns the structured math formula.
     */
    fun recognize(strokes: List<Stroke>): MathRecognitionResult {
        if (strokes.isEmpty()) {
            return MathRecognitionResult(
                latex = "",
                unicodeText = "",
                confidence = 1.0,
                bounds = MathBoundingBox(0.0, 0.0, 0.0, 0.0),
                symbolCount = 0,
                originalStrokes = emptyList()
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

        return MathRecognitionResult(
            latex = latex,
            unicodeText = unicode,
            confidence = avgConfidence,
            bounds = totalBounds,
            symbolCount = symbols.size,
            originalStrokes = strokes
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
        val aspect = bounds.width / max(1.0, bounds.height)
        if (aspect < 0.6) return false
        val pStart = pts.first()
        val pEnd = pts.last()
        val diag = hypot(bounds.width, bounds.height)
        val startEndDist = hypot(pEnd.x - pStart.x, pEnd.y - pStart.y)
        if (startEndDist / max(1.0, diag) < 0.35) return false // Not a closed loop
        val hasVinculum = pEnd.x > bounds.left + bounds.width * 0.55 && pEnd.y < bounds.top + bounds.height * 0.4
        val minPtY = pts.minOf { it.y }
        val maxPtY = pts.maxOf { it.y }
        return hasVinculum && (maxPtY - minPtY) > 6.0 && pStart.x < bounds.left + bounds.width * 0.4
    }

    private fun clusterStrokes(strokes: List<Stroke>): List<StrokeCluster> {
        if (strokes.isEmpty()) return emptyList()

        val items = strokes.map { it to strokeBounds(it) }
        val clusters = mutableListOf<MutableList<Pair<Stroke, MathBoundingBox>>>()

        for (item in items) {
            var merged = false
            val itemStroke = item.first
            val itemBounds = item.second
            val isItemRadical = isRadicalStroke(itemStroke, itemBounds)
            val isItemHBar = itemBounds.width / max(1.0, itemBounds.height) > 1.4

            // If this item is a radical stroke or fraction bar, do not merge other enclosed strokes into it
            for (c in clusters) {
                val cBounds = computeClusterBounds(c)
                val cStrokes = c.map { it.first }
                val isClusterRadical = cStrokes.any { isRadicalStroke(it, cBounds) }

                if (isItemRadical || isClusterRadical) {
                    continue // Keep radical separate from its inner content
                }

                val isClusterHBar = c.size == 1 && (cBounds.width / max(1.0, cBounds.height) > 1.4)

                // 1. Equals sign '=': two parallel horizontal strokes
                if (isItemHBar && isClusterHBar) {
                    val xOverlap = min(cBounds.right, itemBounds.right) - max(cBounds.left, itemBounds.left)
                    val vDist = abs(cBounds.centerY - itemBounds.centerY)
                    if (xOverlap > 4.0 && vDist in 2.0..26.0) {
                        c.add(item)
                        merged = true
                        break
                    }
                }

                // 2. Overlap check for multi-stroke characters (+, x, t, i, etc.)
                if (cBounds.overlaps(itemBounds, padX = 1.0, padY = 1.0)) {
                    // Protect fraction bars from merging with numerator/denominator
                    val isFrac1 = itemBounds.width > 16.0 && itemBounds.height < 10.0 &&
                                  (cBounds.bottom < itemBounds.centerY || cBounds.top > itemBounds.centerY)
                    val isFrac2 = cBounds.width > 16.0 && cBounds.height < 10.0 &&
                                  (itemBounds.bottom < cBounds.centerY || itemBounds.top > cBounds.centerY)

                    if (!isFrac1 && !isFrac2) {
                        c.add(item)
                        merged = true
                        break
                    }
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

        // Single Stroke Classification
        if (strokes.size == 1) {
            val stroke = strokes.first()
            val pts = stroke.points
            if (pts.isEmpty()) return MathSymbol("?", "?", 0.5, bounds, originalStrokes = strokes)

            val pStart = pts.first()
            val pEnd = pts.last()
            val startEndDist = hypot(pEnd.x - pStart.x, pEnd.y - pStart.y)
            val diag = hypot(bounds.width, bounds.height)

            // 1. Small Dot / Period / Multiplication Dot
            if (diag < 10.0) {
                return MathSymbol(
                    latex = "\\cdot",
                    unicode = "·",
                    confidence = 0.95,
                    bounds = bounds,
                    isOperator = true,
                    originalStrokes = strokes
                )
            }

            // 2. Closed loop: 0, O, \theta, \infty
            if (diag > 8.0 && (startEndDist / max(1.0, diag)) < 0.35) {
                // Check if wider than tall (infinity sign / alpha)
                if (aspect > 1.4) {
                    return MathSymbol("\\infty", "∞", 0.88, bounds, originalStrokes = strokes)
                }
                return MathSymbol("0", "0", 0.90, bounds, originalStrokes = strokes)
            }

            // 3. Fraction Bar (prominent horizontal line)
            if (aspect > 2.0 && bounds.height < 14.0) {
                return MathSymbol(
                    latex = "-",
                    unicode = "-",
                    confidence = 0.96,
                    bounds = bounds,
                    isOperator = true,
                    isFractionBar = true,
                    originalStrokes = strokes
                )
            }

            // 4. Integral sign \int (tall S-like curve)
            if (aspect < 0.45 && bounds.height > 20.0) {
                var yAscending = 0
                var yDescending = 0
                for (i in 1 until pts.size) {
                    if (pts[i].y > pts[i - 1].y) yDescending++ else yAscending++
                }
                if (yDescending > pts.size * 0.55 || yAscending > pts.size * 0.55) {
                    return MathSymbol(
                        latex = "\\int",
                        unicode = "∫",
                        confidence = 0.92,
                        bounds = bounds,
                        isOperator = true,
                        isIntegral = true,
                        originalStrokes = strokes
                    )
                }
            }

            // 5. Square Root / Radical sign \sqrt
            if (isRadicalStroke(stroke, bounds)) {
                return MathSymbol(
                    latex = "\\sqrt",
                    unicode = "√",
                    confidence = 0.90,
                    bounds = bounds,
                    isOperator = true,
                    isRadical = true,
                    originalStrokes = strokes
                )
            }

            // 6. Vertical line: 1, |, l
            if (aspect < 0.35 && bounds.height > 10.0) {
                return MathSymbol("1", "1", 0.88, bounds, originalStrokes = strokes)
            }

            // 7. Horizontal line / minus: -
            if (aspect > 1.8) {
                return MathSymbol("-", "-", 0.94, bounds, isOperator = true, originalStrokes = strokes)
            }

            // 8. Parentheses: (, )
            if (aspect in 0.2..0.6 && bounds.height > 14.0) {
                val midPt = pts[pts.size / 2]
                if (midPt.x < pStart.x && midPt.x < pEnd.x) {
                    return MathSymbol("(", "(", 0.90, bounds, isDelimiter = true, originalStrokes = strokes)
                } else if (midPt.x > pStart.x && midPt.x > pEnd.x) {
                    return MathSymbol(")", ")", 0.90, bounds, isDelimiter = true, originalStrokes = strokes)
                }
            }

            // 9. S-curve / s
            var inflections = 0
            for (i in 2 until pts.size) {
                val dx1 = pts[i - 1].x - pts[i - 2].x
                val dx2 = pts[i].x - pts[i - 1].x
                if (dx1 * dx2 < -1.0) inflections++
            }
            if (inflections >= 2) {
                return MathSymbol("s", "s", 0.85, bounds, originalStrokes = strokes)
            }

            // 10. C-curve / c
            if (pStart.x > bounds.left + bounds.width * 0.45 && pEnd.x > bounds.left + bounds.width * 0.45) {
                return MathSymbol("c", "c", 0.85, bounds, originalStrokes = strokes)
            }

            // 11. Greek \alpha
            if (aspect in 0.8..1.5 && pts.size > 8) {
                val lastQuarterX = pts.takeLast(pts.size / 4).map { it.x }.average()
                if (lastQuarterX > bounds.centerX) {
                    return MathSymbol("\\alpha", "α", 0.84, bounds, originalStrokes = strokes)
                }
            }

            // Default fallback variable
            return MathSymbol("x", "x", 0.78, bounds, originalStrokes = strokes)
        }

        // Two Strokes Classification
        if (strokes.size == 2) {
            val b1 = strokeBounds(strokes[0])
            val b2 = strokeBounds(strokes[1])

            val isH1 = b1.width / max(1.0, b1.height) > 1.2
            val isV1 = b1.height / max(1.0, b1.width) > 1.2
            val isH2 = b2.width / max(1.0, b2.height) > 1.2
            val isV2 = b2.height / max(1.0, b2.width) > 1.2

            // Plus '+': intersecting horizontal & vertical
            if ((isH1 && isV2) || (isV1 && isH2)) {
                return MathSymbol("+", "+", 0.96, bounds, isOperator = true, originalStrokes = strokes)
            }

            // Equals '=': two parallel horizontal strokes
            if (isH1 && isH2) {
                return MathSymbol("=", "=", 0.96, bounds, isOperator = true, originalStrokes = strokes)
            }

            // Cross 'x' or multiplication \times
            if (b1.overlaps(b2, 1.0, 1.0)) {
                return MathSymbol("x", "x", 0.88, bounds, originalStrokes = strokes)
            }

            // Radical with separate overbar
            if (b1.left < b2.left && isH2) {
                return MathSymbol("\\sqrt", "√", 0.92, bounds, isOperator = true, isRadical = true, originalStrokes = strokes)
            }

            // Letter 't'
            if ((isV1 && isH2) || (isV2 && isH1)) {
                return MathSymbol("t", "t", 0.85, bounds, originalStrokes = strokes)
            }

            // Letter 'i' or 'j' with dot
            val topS = if (b1.top < b2.top) b1 else b2
            val botS = if (b1.top < b2.top) b2 else b1
            if (topS.height < 12.0 && botS.height > 12.0) {
                return MathSymbol("i", "i", 0.90, bounds, originalStrokes = strokes)
            }
        }

        // Three Strokes Classification: Greek \pi, \sum, =, \neq
        if (strokes.size == 3) {
            val bList = strokes.map { strokeBounds(it) }
            val hCount = bList.count { it.width / max(1.0, it.height) > 1.2 }
            val vCount = bList.count { it.height / max(1.0, it.width) > 1.2 }

            // Greek \pi: horizontal top bar + 2 vertical legs
            if (hCount >= 1 && vCount >= 2) {
                return MathSymbol("\\pi", "π", 0.90, bounds, originalStrokes = strokes)
            }

            // Not equal \neq: equals + diagonal slash
            if (hCount >= 2) {
                return MathSymbol("\\neq", "≠", 0.92, bounds, isOperator = true, originalStrokes = strokes)
            }

            // Summation \sum (Sigma)
            return MathSymbol("\\sum", "∑", 0.88, bounds, isOperator = true, isSummation = true, originalStrokes = strokes)
        }

        return MathSymbol("?", "?", 0.50, bounds, originalStrokes = strokes)
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

        // 1. Detect Fraction Bars
        val fractionBar = symbols.firstOrNull { it.isFractionBar }
        if (fractionBar != null) {
            val barB = fractionBar.bounds
            val above = symbols.filter { it !== fractionBar && it.bounds.bottom <= barB.centerY + 5.0 && it.bounds.right >= barB.left - 6.0 && it.bounds.left <= barB.right + 6.0 }
            val below = symbols.filter { it !== fractionBar && it.bounds.top >= barB.centerY - 5.0 && it.bounds.right >= barB.left - 6.0 && it.bounds.left <= barB.right + 6.0 }

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
            val inside = symbols.filter { it !== radical && it.bounds.left >= radB.left + 4.0 && it.bounds.right <= radB.right + 30.0 && it.bounds.top >= radB.top - 5.0 }
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
            val lower = symbols.filter { it !== integral && it.bounds.top >= intB.centerY && it.bounds.left in (intB.left - 5.0)..(intB.right + 15.0) }
            val upper = symbols.filter { it !== integral && it.bounds.bottom <= intB.centerY && it.bounds.left in (intB.left - 5.0)..(intB.right + 15.0) }
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
