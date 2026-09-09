package com.nexopp.stem

import com.nexopp.format.model.Element
import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import com.nexopp.format.model.Tool
import java.util.UUID
import kotlin.math.*

enum class PlotGridStyle(val label: String) {
    NONE("Sin cuadrícula"),
    SUBTLE("Cuadrícula sutil"),
    DENSE("Cuadrícula densa")
}

data class PlotFunctionItem(
    val id: String = UUID.randomUUID().toString(),
    val formula: String,
    val color: Int = 0xFF1976D2.toInt(),
    val isVisible: Boolean = true,
    val strokeWidthPt: Float = 2.0f,
    val label: String = ""
)

data class ParametricFunctionItem(
    val id: String = UUID.randomUUID().toString(),
    val formulaX: String,
    val formulaY: String,
    val tMin: Double = 0.0,
    val tMax: Double = 2 * PI,
    val color: Int = 0xFFD81B60.toInt(),
    val isVisible: Boolean = true,
    val strokeWidthPt: Float = 2.0f,
    val label: String = ""
)

data class PlotPoint(
    val x: Double,
    val y: Double,
    val label: String = "",
    val color: Int = 0xFFE53935.toInt()
)

/**
 * Pure Kotlin mathematical expression evaluator and advanced vector function plotter for STEM notes.
 * Generates standard .xopp [Element.Stroke] objects for 100% native vector math curves, grids, roots, and coordinates.
 */
object FunctionPlotter {

    /**
     * Evaluates a mathematical expression [expr] for variable [x] (or parameter [t]).
     * Returns [Double.NaN] on syntax errors, division by zero or out of domain.
     * @param isRad Whether angle arguments in trig functions are radians (true) or degrees (false).
     */
    fun evaluate(expr: String, x: Double = 0.0, isRad: Boolean = true): Double {
        return try {
            val sanitized = expr.lowercase().replace(" ", "")
            val parser = MathParser(sanitized, x, isRad)
            parser.parse()
        } catch (e: Exception) {
            Double.NaN
        }
    }

    /**
     * Finds approximate roots (zeros where f(x) = 0) within [xMin, xMax].
     */
    fun findRoots(formula: String, xMin: Double, xMax: Double, steps: Int = 200): List<Double> {
        val roots = mutableListOf<Double>()
        val dx = (xMax - xMin) / steps
        var prevX = xMin
        var prevY = evaluate(formula, prevX)

        for (i in 1..steps) {
            val curX = xMin + i * dx
            val curY = evaluate(formula, curX)

            if (!prevY.isNaN() && !curY.isNaN()) {
                if (abs(curY) < 1e-6) {
                    roots.add(curX)
                } else if (prevY * curY < 0) {
                    // Refine root via bisection
                    var low = prevX
                    var high = curX
                    for (iter in 0 until 12) {
                        val mid = (low + high) / 2.0
                        val midY = evaluate(formula, mid)
                        if (abs(midY) < 1e-7 || (high - low) < 1e-6) {
                            low = mid
                            break
                        }
                        if (prevY * midY < 0) {
                            high = mid
                        } else {
                            low = mid
                        }
                    }
                    if (roots.none { abs(it - low) < 1e-3 }) {
                        roots.add(low)
                    }
                }
            }
            prevX = curX
            prevY = curY
        }
        return roots
    }

    /**
     * Finds approximate intersections between two functions within [xMin, xMax].
     */
    fun findIntersections(
        formula1: String,
        formula2: String,
        xMin: Double,
        xMax: Double,
        steps: Int = 200
    ): List<Pair<Double, Double>> {
        val diffFormula = "($formula1)-($formula2)"
        val xIntersections = findRoots(diffFormula, xMin, xMax, steps)
        return xIntersections.mapNotNull { x ->
            val y = evaluate(formula1, x)
            if (!y.isNaN()) Pair(x, y) else null
        }
    }

    /**
     * Generates multi-function vector plot elements for inclusion in a document.
     */
    fun generateAdvancedPlotElements(
        functions: List<PlotFunctionItem>,
        parametricFunctions: List<ParametricFunctionItem> = emptyList(),
        points: List<PlotPoint> = emptyList(),
        originX: Double,
        originY: Double,
        plotWidthPt: Double = 300.0,
        plotHeightPt: Double = 220.0,
        xMin: Double = -5.0,
        xMax: Double = 5.0,
        yMin: Double = -5.0,
        yMax: Double = 5.0,
        drawAxes: Boolean = true,
        axesColor: Int = 0xFF455A64.toInt(),
        gridStyle: PlotGridStyle = PlotGridStyle.SUBTLE,
        gridColor: Int = 0x3390A4AE.toInt(),
        highlightRoots: Boolean = false,
        highlightIntersections: Boolean = false
    ): List<Element> {
        val elements = mutableListOf<Element>()
        val left = originX - plotWidthPt / 2
        val top = originY - plotHeightPt / 2
        val right = originX + plotWidthPt / 2
        val bottom = originY + plotHeightPt / 2

        fun toPageX(mathX: Double): Double =
            left + ((mathX - xMin) / (xMax - xMin)) * plotWidthPt

        fun toPageY(mathY: Double): Double =
            bottom - ((mathY - yMin) / (yMax - yMin)) * plotHeightPt

        val axisX0 = toPageX(0.0).coerceIn(left, right)
        val axisY0 = toPageY(0.0).coerceIn(top, bottom)

        // 1. Grid Lines
        if (gridStyle != PlotGridStyle.NONE) {
            val gridStepX = calculateTickStep(xMin, xMax) / (if (gridStyle == PlotGridStyle.DENSE) 2.0 else 1.0)
            val gridStepY = calculateTickStep(yMin, yMax) / (if (gridStyle == PlotGridStyle.DENSE) 2.0 else 1.0)
            val gridLineWidth = 0.6

            var gx = ceil(xMin / gridStepX) * gridStepX
            while (gx <= xMax) {
                val px = toPageX(gx)
                elements.add(
                    Stroke(
                        tool = Tool.PEN,
                        color = gridColor,
                        capStyle = "round",
                        points = listOf(StrokePoint(px, top, gridLineWidth), StrokePoint(px, bottom, gridLineWidth)),
                        uniformWidth = true
                    )
                )
                gx += gridStepX
            }

            var gy = ceil(yMin / gridStepY) * gridStepY
            while (gy <= yMax) {
                val py = toPageY(gy)
                elements.add(
                    Stroke(
                        tool = Tool.PEN,
                        color = gridColor,
                        capStyle = "round",
                        points = listOf(StrokePoint(left, py, gridLineWidth), StrokePoint(right, py, gridLineWidth)),
                        uniformWidth = true
                    )
                )
                gy += gridStepY
            }
        }

        // 2. Coordinate Axes & Arrow Heads
        if (drawAxes) {
            val axisWidth = 1.2
            val tickWidth = 1.0

            // X-Axis
            elements.add(
                Stroke(
                    tool = Tool.PEN,
                    color = axesColor,
                    capStyle = "round",
                    points = listOf(StrokePoint(left, axisY0, axisWidth), StrokePoint(right + 8.0, axisY0, axisWidth)),
                    uniformWidth = true
                )
            )
            // X Arrowhead
            elements.add(
                Stroke(
                    tool = Tool.PEN,
                    color = axesColor,
                    capStyle = "round",
                    points = listOf(
                        StrokePoint(right + 2.0, axisY0 - 4.0, axisWidth),
                        StrokePoint(right + 8.0, axisY0, axisWidth),
                        StrokePoint(right + 2.0, axisY0 + 4.0, axisWidth)
                    ),
                    uniformWidth = true
                )
            )

            // Y-Axis
            elements.add(
                Stroke(
                    tool = Tool.PEN,
                    color = axesColor,
                    capStyle = "round",
                    points = listOf(StrokePoint(axisX0, bottom, axisWidth), StrokePoint(axisX0, top - 8.0, axisWidth)),
                    uniformWidth = true
                )
            )
            // Y Arrowhead
            elements.add(
                Stroke(
                    tool = Tool.PEN,
                    color = axesColor,
                    capStyle = "round",
                    points = listOf(
                        StrokePoint(axisX0 - 4.0, top - 2.0, axisWidth),
                        StrokePoint(axisX0, top - 8.0, axisWidth),
                        StrokePoint(axisX0 + 4.0, top - 2.0, axisWidth)
                    ),
                    uniformWidth = true
                )
            )

            // X Ticks
            val xStep = calculateTickStep(xMin, xMax)
            var curX = ceil(xMin / xStep) * xStep
            while (curX <= xMax) {
                if (abs(curX) > 1e-6) {
                    val px = toPageX(curX)
                    elements.add(
                        Stroke(
                            tool = Tool.PEN,
                            color = axesColor,
                            capStyle = "round",
                            points = listOf(StrokePoint(px, axisY0 - 3.0, tickWidth), StrokePoint(px, axisY0 + 3.0, tickWidth)),
                            uniformWidth = true
                        )
                    )
                }
                curX += xStep
            }

            // Y Ticks
            val yStep = calculateTickStep(yMin, yMax)
            var curY = ceil(yMin / yStep) * yStep
            while (curY <= yMax) {
                if (abs(curY) > 1e-6) {
                    val py = toPageY(curY)
                    elements.add(
                        Stroke(
                            tool = Tool.PEN,
                            color = axesColor,
                            capStyle = "round",
                            points = listOf(StrokePoint(axisX0 - 3.0, py, tickWidth), StrokePoint(axisX0 + 3.0, py, tickWidth)),
                            uniformWidth = true
                        )
                    )
                }
                curY += yStep
            }
        }

        // 3. Render Standard Functions
        val samples = 220
        val dx = (xMax - xMin) / samples

        for (fn in functions) {
            if (!fn.isVisible || fn.formula.isBlank()) continue
            val currentSegment = mutableListOf<StrokePoint>()
            val curveWidth = fn.strokeWidthPt.toDouble()

            for (i in 0..samples) {
                val mathX = xMin + i * dx
                val mathY = evaluate(fn.formula, mathX)

                if (!mathY.isNaN() && !mathY.isInfinite() && mathY in (yMin - (yMax - yMin) * 2)..(yMax + (yMax - yMin) * 2)) {
                    val px = toPageX(mathX)
                    val py = toPageY(mathY).coerceIn(top - 20.0, bottom + 20.0)
                    currentSegment.add(StrokePoint(px, py, curveWidth))
                } else {
                    if (currentSegment.size >= 2) {
                        elements.add(
                            Stroke(
                                tool = Tool.PEN,
                                color = fn.color,
                                capStyle = "round",
                                points = currentSegment.toList(),
                                uniformWidth = true
                            )
                        )
                    }
                    currentSegment.clear()
                }
            }

            if (currentSegment.size >= 2) {
                elements.add(
                    Stroke(
                        tool = Tool.PEN,
                        color = fn.color,
                        capStyle = "round",
                        points = currentSegment.toList(),
                        uniformWidth = true
                    )
                )
            }

            // Highlight roots if requested
            if (highlightRoots) {
                val roots = findRoots(fn.formula, xMin, xMax)
                for (r in roots) {
                    val rx = toPageX(r)
                    val ry = toPageY(0.0)
                    drawPointDot(elements, rx, ry, fn.color, radius = 3.5)
                }
            }
        }

        // 4. Render Parametric Functions
        val pSamples = 200
        for (pFn in parametricFunctions) {
            if (!pFn.isVisible || pFn.formulaX.isBlank() || pFn.formulaY.isBlank()) continue
            val dt = (pFn.tMax - pFn.tMin) / pSamples
            val currentSegment = mutableListOf<StrokePoint>()
            val curveWidth = pFn.strokeWidthPt.toDouble()

            for (i in 0..pSamples) {
                val t = pFn.tMin + i * dt
                val mathX = evaluate(pFn.formulaX, t)
                val mathY = evaluate(pFn.formulaY, t)

                if (!mathX.isNaN() && !mathY.isNaN() && !mathX.isInfinite() && !mathY.isInfinite()) {
                    val px = toPageX(mathX)
                    val py = toPageY(mathY)
                    currentSegment.add(StrokePoint(px, py, curveWidth))
                } else {
                    if (currentSegment.size >= 2) {
                        elements.add(
                            Stroke(
                                tool = Tool.PEN,
                                color = pFn.color,
                                capStyle = "round",
                                points = currentSegment.toList(),
                                uniformWidth = true
                            )
                        )
                    }
                    currentSegment.clear()
                }
            }

            if (currentSegment.size >= 2) {
                elements.add(
                    Stroke(
                        tool = Tool.PEN,
                        color = pFn.color,
                        capStyle = "round",
                        points = currentSegment.toList(),
                        uniformWidth = true
                    )
                )
            }
        }

        // 5. Highlight Intersections
        if (highlightIntersections && functions.size >= 2) {
            val visibleFns = functions.filter { it.isVisible }
            for (i in 0 until visibleFns.size) {
                for (j in i + 1 until visibleFns.size) {
                    val inters = findIntersections(visibleFns[i].formula, visibleFns[j].formula, xMin, xMax)
                    for ((ix, iy) in inters) {
                        if (ix in xMin..xMax && iy in yMin..yMax) {
                            val px = toPageX(ix)
                            val py = toPageY(iy)
                            drawPointDot(elements, px, py, 0xFFE53935.toInt(), radius = 4.0)
                        }
                    }
                }
            }
        }

        // 6. Render Custom Points & Markers
        for (pt in points) {
            if (pt.x in xMin..xMax && pt.y in yMin..yMax) {
                val px = toPageX(pt.x)
                val py = toPageY(pt.y)
                drawPointDot(elements, px, py, pt.color, radius = 3.8)
                // Crosshairs
                elements.add(
                    Stroke(
                        tool = Tool.PEN,
                        color = pt.color,
                        capStyle = "round",
                        points = listOf(StrokePoint(px - 5, py, 1.0), StrokePoint(px + 5, py, 1.0)),
                        uniformWidth = true
                    )
                )
                elements.add(
                    Stroke(
                        tool = Tool.PEN,
                        color = pt.color,
                        capStyle = "round",
                        points = listOf(StrokePoint(px, py - 5, 1.0), StrokePoint(px, py + 5, 1.0)),
                        uniformWidth = true
                    )
                )
            }
        }

        return elements
    }

    private fun drawPointDot(elements: MutableList<Element>, px: Double, py: Double, color: Int, radius: Double) {
        val pts = mutableListOf<StrokePoint>()
        val segs = 12
        for (s in 0..segs) {
            val angle = 2 * PI * s / segs
            pts.add(StrokePoint(px + radius * cos(angle), py + radius * sin(angle), 1.2))
        }
        elements.add(
            Stroke(
                tool = Tool.PEN,
                color = color,
                capStyle = "round",
                points = pts,
                uniformWidth = true
            )
        )
    }

    /**
     * Backward-compatible simple plotter for single function.
     */
    fun generatePlotElements(
        formula: String,
        originX: Double,
        originY: Double,
        plotWidthPt: Double = 280.0,
        plotHeightPt: Double = 200.0,
        xMin: Double = -5.0,
        xMax: Double = 5.0,
        yMin: Double = -5.0,
        yMax: Double = 5.0,
        strokeColor: Int = 0xFF1976D2.toInt(),
        strokeWidthPt: Float = 1.8f,
        drawAxes: Boolean = true,
        axesColor: Int = 0xFF546E7A.toInt()
    ): List<Element> {
        return generateAdvancedPlotElements(
            functions = listOf(PlotFunctionItem(formula = formula, color = strokeColor, strokeWidthPt = strokeWidthPt)),
            originX = originX,
            originY = originY,
            plotWidthPt = plotWidthPt,
            plotHeightPt = plotHeightPt,
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
            drawAxes = drawAxes,
            axesColor = axesColor
        )
    }

    private fun calculateTickStep(min: Double, max: Double): Double {
        val span = max - min
        return when {
            span <= 4 -> 0.5
            span <= 12 -> 1.0
            span <= 30 -> 2.0
            span <= 60 -> 5.0
            else -> 10.0
        }
    }

    /**
     * Recursive descent parser for mathematical expressions.
     */
    private class MathParser(
        private val str: String,
        private val xVal: Double,
        private val isRad: Boolean = true
    ) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val v = parseExpression()
            if (pos < str.length) return Double.NaN
            return v
        }

        private fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> v += parseTerm()
                    eat('-'.code) -> v -= parseTerm()
                    else -> return v
                }
            }
        }

        private fun parseTerm(): Double {
            var v = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> v *= parseFactor()
                    eat('/'.code) -> {
                        val d = parseFactor()
                        v = if (abs(d) < 1e-12) Double.NaN else v / d
                    }
                    else -> return v
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var v: Double
            val startPos = pos
            if (eat('('.code)) {
                v = parseExpression()
                eat(')'.code)
            } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                v = str.substring(startPos, pos).toDoubleOrNull() ?: Double.NaN
            } else if (ch in 'a'.code..'z'.code) {
                while (ch in 'a'.code..'z'.code || ch in '0'.code..'9'.code) nextChar()
                val func = str.substring(startPos, pos)
                v = when (func) {
                    "x", "t" -> xVal
                    "pi" -> PI
                    "e" -> E
                    else -> {
                        val arg = parseFactor()
                        when (func) {
                            "sin" -> if (isRad) sin(arg) else sin(Math.toRadians(arg))
                            "cos" -> if (isRad) cos(arg) else cos(Math.toRadians(arg))
                            "tan" -> {
                                val rad = if (isRad) arg else Math.toRadians(arg)
                                if (abs(cos(rad)) < 1e-9) Double.NaN else tan(rad)
                            }
                            "asin" -> {
                                val res = asin(arg)
                                if (isRad) res else Math.toDegrees(res)
                            }
                            "acos" -> {
                                val res = acos(arg)
                                if (isRad) res else Math.toDegrees(res)
                            }
                            "atan" -> {
                                val res = atan(arg)
                                if (isRad) res else Math.toDegrees(res)
                            }
                            "sinh" -> sinh(arg)
                            "cosh" -> cosh(arg)
                            "tanh" -> tanh(arg)
                            "sqrt" -> if (arg >= 0) sqrt(arg) else Double.NaN
                            "abs" -> abs(arg)
                            "exp" -> exp(arg)
                            "ln" -> if (arg > 0) ln(arg) else Double.NaN
                            "log", "log10" -> if (arg > 0) log10(arg) else Double.NaN
                            "sinc" -> if (abs(arg) < 1e-9) 1.0 else sin(arg) / arg
                            else -> Double.NaN
                        }
                    }
                }
            } else {
                return Double.NaN
            }

            if (eat('^'.code)) {
                val exponent = parseFactor()
                v = v.pow(exponent)
            }

            return v
        }
    }
}
