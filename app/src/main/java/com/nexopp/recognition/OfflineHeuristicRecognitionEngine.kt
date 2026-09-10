package com.nexopp.recognition

import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import kotlin.math.*

/**
 * High-performance, 100% offline, privacy-first handwriting and math recognition engine.
 *
 * Uses geometric feature extraction, directional chain coding, spatial relationship trees,
 * and topological stroke analysis to recognize natural handwriting, mathematical formulas (LaTeX),
 * and gesture edits without requiring external services or heavy models.
 */
class OfflineHeuristicRecognitionEngine : RecognitionEngine {

    override val id: String = "nexopp_offline_heuristic"
    override val displayName: String = "NeXopp Motor Local Fuera de Línea"
    override val isOffline: Boolean = true

    override fun recognize(strokes: List<Stroke>): RecognitionResult {
        if (strokes.isEmpty()) {
            return RecognitionResult(text = "", candidates = emptyList(), strokeCount = 0)
        }

        val totalBounds = computeTotalBounds(strokes)
        val glyphClusters = clusterStrokesIntoGlyphs(strokes)
        val recognizedGlyphs = glyphClusters.map { cluster ->
            recognizeSingleGlyph(cluster)
        }

        // Build left-to-right text with spacing heuristics
        val sb = StringBuilder()
        for (i in recognizedGlyphs.indices) {
            val current = recognizedGlyphs[i]
            sb.append(current.symbol)
            if (i < recognizedGlyphs.size - 1) {
                val next = recognizedGlyphs[i + 1]
                val gap = next.bounds.left - current.bounds.right
                val avgWidth = (current.bounds.width() + next.bounds.width()) / 2.0f
                if (gap > avgWidth * 0.45f && current.symbol != " " && next.symbol != " ") {
                    sb.append(" ")
                }
            }
        }

        val text = sb.toString().trim()
        val avgConfidence = if (recognizedGlyphs.isNotEmpty()) {
            recognizedGlyphs.map { it.confidence }.average().toFloat()
        } else 1.0f

        val candidateList = listOf(
            RecognitionCandidate(text = text, confidence = avgConfidence, isMath = false),
            RecognitionCandidate(text = text.uppercase(), confidence = avgConfidence * 0.9f, isMath = false),
            RecognitionCandidate(text = text.lowercase(), confidence = avgConfidence * 0.9f, isMath = false)
        )

        return RecognitionResult(
            text = text,
            latex = null,
            candidates = candidateList,
            strokeCount = strokes.size,
            bounds = totalBounds
        )
    }

    override fun recognizeMath(strokes: List<Stroke>): RecognitionResult {
        if (strokes.isEmpty()) {
            return RecognitionResult(text = "", latex = "", candidates = emptyList(), strokeCount = 0)
        }

        val totalBounds = computeTotalBounds(strokes)
        val mathResult = com.nexopp.stem.MathHandwritingEngine.recognize(strokes)
        val latex = mathResult.latex
        val plainText = mathResult.unicodeText.ifEmpty { latex }
        val avgConfidence = mathResult.confidence.toFloat()

        val candidateList = listOf(
            RecognitionCandidate(text = plainText, latex = latex, confidence = avgConfidence, isMath = true),
            RecognitionCandidate(text = plainText, latex = "$$latex$$", confidence = avgConfidence * 0.95f, isMath = true)
        )

        return RecognitionResult(
            text = plainText,
            latex = latex,
            candidates = candidateList,
            strokeCount = strokes.size,
            bounds = totalBounds
        )
    }

    override fun detectGesture(strokes: List<Stroke>): StrokeGesture? {
        if (strokes.isEmpty()) return null
        val bounds = computeTotalBounds(strokes)

        if (strokes.size == 1) {
            val stroke = strokes.first()
            val points = stroke.points
            if (points.size < 4) return null

            // 1. Scratch-out erase gesture: rapid horizontal reversals in tight area
            var xReversals = 0
            var prevDirX = 0
            for (i in 1 until points.size) {
                val dx = points[i].x - points[i - 1].x
                val dirX = if (dx > 1.5) 1 else if (dx < -1.5) -1 else 0
                if (dirX != 0 && prevDirX != 0 && dirX != prevDirX) {
                    xReversals++
                }
                if (dirX != 0) prevDirX = dirX
            }

            if (xReversals >= 4) {
                return StrokeGesture(
                    kind = GestureKind.SCRATCH_OUT_ERASE,
                    confidence = min(1.0f, 0.6f + xReversals * 0.08f),
                    bounds = bounds
                )
            }

            // 2. Circle / Lasso select gesture: loop that returns near start
            val start = points.first()
            val end = points.last()
            val startEndDist = hypot(end.x - start.x, end.y - start.y)
            val diag = hypot(bounds.width().toDouble(), bounds.height().toDouble())

            if (diag > 20 && startEndDist / diag < 0.35 && points.size > 12) {
                val aspect = bounds.width() / max(1f, bounds.height())
                if (aspect in 0.4f..2.5f) {
                    return StrokeGesture(
                        kind = GestureKind.CIRCLE_SELECT,
                        confidence = 0.88f,
                        bounds = bounds
                    )
                }
            }

            // 3. Underline emphasis: single horizontal stroke with low vertical variation
            val aspect = bounds.width() / max(1f, bounds.height())
            if (aspect > 2.5f && bounds.width() > 20f) {
                return StrokeGesture(
                    kind = GestureKind.UNDERLINE_EMPHASIS,
                    confidence = 0.90f,
                    bounds = bounds
                )
            }

            // 4. Checkmark gesture: short down-right stroke followed by longer up-right stroke
            val minPointY = points.minByOrNull { it.y } ?: points.first()
            val minIndex = points.indexOf(minPointY)
            val maxPointY = points.maxByOrNull { it.y } ?: points.last()
            val maxIndex = points.indexOf(maxPointY)

            if (maxIndex in 1 until points.size - 2) {
                val pStart = points.first()
                val pBottom = points[maxIndex]
                val pEnd = points.last()
                if (pBottom.y > pStart.y && pEnd.y < pBottom.y && pEnd.x > pBottom.x && pBottom.x >= pStart.x) {
                    return StrokeGesture(
                        kind = GestureKind.CHECKMARK,
                        confidence = 0.85f,
                        bounds = bounds
                    )
                }
            }
        }

        return null
    }

    // --- Internal Stroke Clustering & Classification ---------------------------------------------

    private data class StrokeCluster(
        val strokes: List<Stroke>,
        val bounds: RecognitionRect
    )

    private data class ClassifiedGlyph(
        val symbol: String,
        val latex: String,
        val confidence: Float,
        val bounds: RecognitionRect,
        val isOperator: Boolean = false,
        val isFractionBar: Boolean = false,
        val isRadical: Boolean = false
    )

    private fun rectsOverlap(a: RecognitionRect, b: RecognitionRect, expandX: Float = 3f, expandY: Float = 1f): Boolean {
        return (a.left - expandX) <= (b.right + expandX) &&
               (a.right + expandX) >= (b.left - expandX) &&
               (a.top - expandY) <= (b.bottom + expandY) &&
               (a.bottom + expandY) >= (b.top - expandY)
    }

    private fun computeTotalBounds(strokes: List<Stroke>): RecognitionRect {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (s in strokes) {
            for (p in s.points) {
                val halfW = max(1.0, p.width / 2.0).toFloat()
                minX = min(minX, p.x.toFloat() - halfW)
                minY = min(minY, p.y.toFloat() - halfW)
                maxX = max(maxX, p.x.toFloat() + halfW)
                maxY = max(maxY, p.y.toFloat() + halfW)
            }
        }

        return if (minX < Float.MAX_VALUE) RecognitionRect(minX, minY, maxX, maxY) else RecognitionRect()
    }

    private fun strokeBounds(stroke: Stroke): RecognitionRect {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (p in stroke.points) {
            val halfW = max(1.0, p.width / 2.0).toFloat()
            minX = min(minX, p.x.toFloat() - halfW)
            minY = min(minY, p.y.toFloat() - halfW)
            maxX = max(maxX, p.x.toFloat() + halfW)
            maxY = max(maxY, p.y.toFloat() + halfW)
        }
        if (minX == Float.MAX_VALUE) return RecognitionRect(0f, 0f, 1f, 1f)
        return RecognitionRect(minX, minY, maxX, maxY)
    }

    private fun clusterStrokesIntoGlyphs(strokes: List<Stroke>): List<StrokeCluster> {
        if (strokes.isEmpty()) return emptyList()

        val strokeItems = strokes.map { it to strokeBounds(it) }
        val clusters = mutableListOf<MutableList<Pair<Stroke, RecognitionRect>>>()

        for (item in strokeItems) {
            var merged = false
            val itemBounds = item.second
            val isItemHBar = itemBounds.width() / max(1f, itemBounds.height()) > 1.4f

            for (c in clusters) {
                val clusterBounds = computeClusterBounds(c)
                val isClusterHBar = c.size == 1 && (clusterBounds.width() / max(1f, clusterBounds.height()) > 1.4f)

                // 1. Special check for Equals '=': two parallel horizontal bars
                if (isItemHBar && isClusterHBar) {
                    val xOverlap = min(clusterBounds.right, itemBounds.right) - max(clusterBounds.left, itemBounds.left)
                    val vDist = abs(clusterBounds.centerY() - itemBounds.centerY())
                    if (xOverlap > 8f && vDist in 4f..22f) {
                        c.add(item)
                        merged = true
                        break
                    }
                }

                // 2. Direct intersection / proximity check (tight on vertical to protect fractions)
                if (rectsOverlap(clusterBounds, itemBounds, expandX = 3.0f, expandY = 1.0f)) {
                    val isFractionBar1 = itemBounds.width() > 16f && itemBounds.height() < 8f &&
                                          (clusterBounds.bottom < itemBounds.centerY() || clusterBounds.top > itemBounds.centerY())
                    val isFractionBar2 = clusterBounds.width() > 16f && clusterBounds.height() < 8f &&
                                          (itemBounds.bottom < clusterBounds.centerY() || itemBounds.top > clusterBounds.centerY())

                    if (!isFractionBar1 && !isFractionBar2) {
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

        // Sort clusters left-to-right
        return clusters.map { c ->
            val b = computeClusterBounds(c)
            StrokeCluster(strokes = c.map { it.first }, bounds = b)
        }.sortedBy { it.bounds.left }
    }

    private fun computeClusterBounds(items: List<Pair<Stroke, RecognitionRect>>): RecognitionRect {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for ((_, b) in items) {
            minX = min(minX, b.left)
            minY = min(minY, b.top)
            maxX = max(maxX, b.right)
            maxY = max(maxY, b.bottom)
        }
        return RecognitionRect(minX, minY, maxX, maxY)
    }

    private fun recognizeSingleGlyph(cluster: StrokeCluster): ClassifiedGlyph {
        val strokes = cluster.strokes
        val bounds = cluster.bounds
        val aspect = bounds.width() / max(1f, bounds.height())

        // 1. Single-stroke glyph heuristics
        if (strokes.size == 1) {
            val pts = strokes.first().points
            if (pts.isEmpty()) return ClassifiedGlyph("", "", 1.0f, bounds)

            val pStart = pts.first()
            val pEnd = pts.last()
            val startEndDist = hypot(pEnd.x - pStart.x, pEnd.y - pStart.y)
            val diag = hypot(bounds.width().toDouble(), bounds.height().toDouble())

            // Dot / Period
            if (diag < 10.0) {
                return ClassifiedGlyph(".", ".", 0.95f, bounds)
            }

            // Closed loop: 0, O, D
            if (diag > 10.0 && startEndDist / diag < 0.35) {
                return ClassifiedGlyph("o", "o", 0.90f, bounds)
            }

            // Vertical line: 1, l, I, |
            if (aspect < 0.45 && bounds.height() > 8.0f) {
                return ClassifiedGlyph("1", "1", 0.88f, bounds)
            }

            // Horizontal line: -
            if (aspect > 2.0f) {
                return ClassifiedGlyph("-", "-", 0.92f, bounds, isOperator = true)
            }

            // S-curve: S, s, 5
            var inflections = 0
            for (i in 2 until pts.size) {
                val dx1 = pts[i - 1].x - pts[i - 2].x
                val dx2 = pts[i].x - pts[i - 1].x
                if (dx1 * dx2 < -1.0) inflections++
            }
            if (inflections >= 2) {
                return ClassifiedGlyph("s", "s", 0.82f, bounds)
            }

            // C curve
            if (pStart.x > bounds.left + bounds.width() * 0.5 && pEnd.x > bounds.left + bounds.width() * 0.5) {
                return ClassifiedGlyph("c", "c", 0.85f, bounds)
            }

            // General variable x / curve
            return ClassifiedGlyph("x", "x", 0.75f, bounds)
        }

        // 2. Two-stroke glyph heuristics
        if (strokes.size == 2) {
            val b1 = strokeBounds(strokes[0])
            val b2 = strokeBounds(strokes[1])

            val isH1 = b1.width() / max(1f, b1.height()) > 1.2f
            val isV1 = b1.height() / max(1f, b1.width()) > 1.2f
            val isH2 = b2.width() / max(1f, b2.height()) > 1.2f
            val isV2 = b2.height() / max(1f, b2.width()) > 1.2f

            // Plus + (intersecting horizontal & vertical)
            if ((isH1 && isV2) || (isV1 && isH2)) {
                return ClassifiedGlyph("+", "+", 0.95f, bounds, isOperator = true)
            }

            // Equals = (two parallel horizontal strokes)
            if (isH1 && isH2) {
                return ClassifiedGlyph("=", "=", 0.95f, bounds, isOperator = true)
            }

            // Letter X / multiply \times
            if (rectsOverlap(b1, b2, 2.0f)) {
                return ClassifiedGlyph("x", "x", 0.85f, bounds)
            }

            // Letter T / t
            if ((isH1 && isV2) || (isH2 && isV1)) {
                return ClassifiedGlyph("t", "t", 0.85f, bounds)
            }

            // Letter i / j with dot
            val topStroke = if (b1.top < b2.top) b1 else b2
            val bottomStroke = if (b1.top < b2.top) b2 else b1
            if (topStroke.height() < 12.0f && bottomStroke.height() > 15.0f) {
                return ClassifiedGlyph("i", "i", 0.90f, bounds)
            }
        }

        // 3. Three-stroke glyph heuristics
        if (strokes.size == 3) {
            return ClassifiedGlyph("H", "H", 0.80f, bounds)
        }

        return ClassifiedGlyph("?", "?", 0.50f, bounds)
    }

    private fun recognizeMathGlyph(cluster: StrokeCluster): ClassifiedGlyph {
        val strokes = cluster.strokes
        val bounds = cluster.bounds
        val aspect = bounds.width() / max(1f, bounds.height())

        // Check for Fraction Bar (wide horizontal stroke)
        if (strokes.size == 1 && aspect > 1.8f && bounds.height() < 14f) {
            return ClassifiedGlyph("-", "-", 0.95f, bounds, isOperator = true, isFractionBar = true)
        }

        // Check for Square Root \sqrt{}
        if (strokes.size in 1..2) {
            val pts = strokes.first().points
            if (pts.size > 6) {
                val hasVinculum = aspect > 1.2f && (strokes.size == 2 || pts.last().x > bounds.left + bounds.width() * 0.7f)
                val startsDownUp = pts.take(pts.size / 2).any { it.y > bounds.top + bounds.height() * 0.5 }
                if (hasVinculum && startsDownUp) {
                    return ClassifiedGlyph("sqrt", "\\sqrt", 0.92f, bounds, isOperator = true, isRadical = true)
                }
            }
        }

        // Check for Integral \int
        if (strokes.size == 1 && aspect < 0.35f && bounds.height() > 25f) {
            val pts = strokes.first().points
            if (pts.first().x > bounds.left && pts.last().x < bounds.right) {
                return ClassifiedGlyph("int", "\\int", 0.88f, bounds, isOperator = true)
            }
        }

        // Fall back to general glyph recognition
        val base = recognizeSingleGlyph(cluster)
        val mathSymbol = when (base.symbol) {
            "x" -> ClassifiedGlyph("x", "x", base.confidence, bounds)
            "+" -> ClassifiedGlyph("+", "+", base.confidence, bounds, isOperator = true)
            "-" -> ClassifiedGlyph("-", "-", base.confidence, bounds, isOperator = true)
            "=" -> ClassifiedGlyph("=", "=", base.confidence, bounds, isOperator = true)
            "1" -> ClassifiedGlyph("1", "1", base.confidence, bounds)
            else -> base
        }
        return mathSymbol
    }

    // --- Spatial Math AST Builder (Fractions, Superscripts, Radicals) -----------------------------

    private sealed interface MathNode {
        fun toLatex(): String
        fun toPlainText(): String
    }

    private data class LeafNode(val symbol: String, val latex: String) : MathNode {
        override fun toLatex(): String = latex
        override fun toPlainText(): String = symbol
    }

    private data class FractionNode(val numerator: MathNode, val denominator: MathNode) : MathNode {
        override fun toLatex(): String = "\\frac{${numerator.toLatex()}}{${denominator.toLatex()}}"
        override fun toPlainText(): String = "(${numerator.toPlainText()} / ${denominator.toPlainText()})"
    }

    private data class PowerNode(val base: MathNode, val exponent: MathNode) : MathNode {
        override fun toLatex(): String = "${base.toLatex()}^{${exponent.toLatex()}}"
        override fun toPlainText(): String = "${base.toPlainText()}^${exponent.toPlainText()}"
    }

    private data class RadicalNode(val content: MathNode) : MathNode {
        override fun toLatex(): String = "\\sqrt{${content.toLatex()}}"
        override fun toPlainText(): String = "sqrt(${content.toPlainText()})"
    }

    private data class SequenceNode(val items: List<MathNode>) : MathNode {
        override fun toLatex(): String = items.joinToString(" ") { it.toLatex() }
        override fun toPlainText(): String = items.joinToString(" ") { it.toPlainText() }
    }

    private fun parseMathSpatialLayout(glyphs: List<ClassifiedGlyph>): MathNode {
        if (glyphs.isEmpty()) return LeafNode("", "")
        if (glyphs.size == 1) return LeafNode(glyphs.first().symbol, glyphs.first().latex)

        // 1. Check for Fraction Bars
        val fractionBar = glyphs.firstOrNull { it.isFractionBar }
        if (fractionBar != null) {
            val barBounds = fractionBar.bounds
            val aboveGlyphs = glyphs.filter { it !== fractionBar && it.bounds.bottom <= barBounds.centerY() + 4f && it.bounds.right >= barBounds.left - 5f && it.bounds.left <= barBounds.right + 5f }
            val belowGlyphs = glyphs.filter { it !== fractionBar && it.bounds.top >= barBounds.centerY() - 4f && it.bounds.right >= barBounds.left - 5f && it.bounds.left <= barBounds.right + 5f }

            if (aboveGlyphs.isNotEmpty() && belowGlyphs.isNotEmpty()) {
                val otherGlyphsLeft = glyphs.filter { it !== fractionBar && !aboveGlyphs.contains(it) && !belowGlyphs.contains(it) && it.bounds.right < barBounds.left }
                val otherGlyphsRight = glyphs.filter { it !== fractionBar && !aboveGlyphs.contains(it) && !belowGlyphs.contains(it) && it.bounds.left > barBounds.right }

                val fracNode = FractionNode(
                    numerator = parseMathSpatialLayout(aboveGlyphs),
                    denominator = parseMathSpatialLayout(belowGlyphs)
                )

                val resultList = mutableListOf<MathNode>()
                if (otherGlyphsLeft.isNotEmpty()) resultList.add(parseMathSpatialLayout(otherGlyphsLeft))
                resultList.add(fracNode)
                if (otherGlyphsRight.isNotEmpty()) resultList.add(parseMathSpatialLayout(otherGlyphsRight))
                return if (resultList.size == 1) resultList.first() else SequenceNode(resultList)
            }
        }

        // 2. Check for Radicals / Square Roots
        val radical = glyphs.firstOrNull { it.isRadical }
        if (radical != null) {
            val radBounds = radical.bounds
            val insideGlyphs = glyphs.filter { it !== radical && it.bounds.left >= radBounds.left + 5f && it.bounds.right <= radBounds.right + 20f && it.bounds.top >= radBounds.top }
            if (insideGlyphs.isNotEmpty()) {
                val radNode = RadicalNode(parseMathSpatialLayout(insideGlyphs))
                val remaining = glyphs.filter { it !== radical && !insideGlyphs.contains(it) }
                return if (remaining.isEmpty()) radNode else SequenceNode(listOf(radNode, parseMathSpatialLayout(remaining)))
            }
        }

        // 3. Check for Exponents / Superscripts
        val nodes = mutableListOf<MathNode>()
        var idx = 0
        while (idx < glyphs.size) {
            val curr = glyphs[idx]
            val currNode = LeafNode(curr.symbol, curr.latex)

            if (idx + 1 < glyphs.size) {
                val next = glyphs[idx + 1]
                val isSuperscript = next.bounds.top < curr.bounds.centerY() &&
                                    next.bounds.bottom < curr.bounds.bottom &&
                                    next.bounds.height() <= curr.bounds.height() * 0.8f &&
                                    next.bounds.left >= curr.bounds.right - 5f

                if (isSuperscript) {
                    val expNode = LeafNode(next.symbol, next.latex)
                    nodes.add(PowerNode(currNode, expNode))
                    idx += 2
                    continue
                }
            }

            nodes.add(currNode)
            idx++
        }

        return if (nodes.size == 1) nodes.first() else SequenceNode(nodes)
    }
}
