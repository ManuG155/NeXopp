package com.nexopp.stem

import com.nexopp.format.model.Element
import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import com.nexopp.format.model.Tool
import kotlin.math.*

/**
 * Pure Kotlin mathematical expression evaluator and vector function plotter for STEM notes.
 * Generates standard .xopp [Element.Stroke] objects so plots are 100% native vector curves.
 */
object FunctionPlotter {

    /**
     * Evaluates a mathematical expression [expr] for variable [x].
     * Returns [Double.NaN] on syntax errors, division by zero or out of domain.
     */
    fun evaluate(expr: String, x: Double): Double {
        return try {
            val sanitized = expr.lowercase().replace(" ", "")
            val parser = MathParser(sanitized, x)
            parser.parse()
        } catch (e: Exception) {
            Double.NaN
        }
    }

    /**
     * Generates vector strokes for function [formula] on page coordinates.
     * @param originX Page X coordinate for plot centre (in pt).
     * @param originY Page Y coordinate for plot centre (in pt).
     * @param plotWidthPt Width of the plot area on page in points (e.g. 280.0 pt).
     * @param plotHeightPt Height of the plot area on page in points (e.g. 200.0 pt).
     * @param xMin Minimum X in math units (e.g. -5.0).
     * @param xMax Maximum X in math units (e.g. 5.0).
     * @param yMin Minimum Y in math units (e.g. -5.0).
     * @param yMax Maximum Y in math units (e.g. 5.0).
     * @param strokeColor Argb stroke color for curve (e.g. 0xFF1976D2.toInt()).
     * @param strokeWidthPt Curve stroke width in pt (e.g. 1.8f).
     * @param drawAxes Whether to generate coordinate axes with graduation marks.
     * @param axesColor Argb color for coordinate axes.
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
        val elements = mutableListOf<Element>()
        val left = originX - plotWidthPt / 2
        val top = originY - plotHeightPt / 2
        val right = originX + plotWidthPt / 2
        val bottom = originY + plotHeightPt / 2

        fun toPageX(mathX: Double): Double =
            left + ((mathX - xMin) / (xMax - xMin)) * plotWidthPt

        fun toPageY(mathY: Double): Double =
            bottom - ((mathY - yMin) / (yMax - yMin)) * plotHeightPt

        val axisWidth = 1.2
        val tickWidth = 1.0

        // 1. Draw Coordinate Axes & Ticks
        if (drawAxes) {
            val axisX0 = toPageX(0.0).coerceIn(left, right)
            val axisY0 = toPageY(0.0).coerceIn(top, bottom)

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
            // X-Axis Arrow Head
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
            // Y-Axis Arrow Head
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

            // X-Axis Ticks
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

            // Y-Axis Ticks
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

        // 2. Evaluate and Plot Function Curve
        val samples = 180
        val dx = (xMax - xMin) / samples
        val currentSegment = mutableListOf<StrokePoint>()
        val curveWidth = strokeWidthPt.toDouble()

        for (i in 0..samples) {
            val mathX = xMin + i * dx
            val mathY = evaluate(formula, mathX)

            if (!mathY.isNaN() && !mathY.isInfinite() && mathY in (yMin - (yMax - yMin) * 2)..(yMax + (yMax - yMin) * 2)) {
                val px = toPageX(mathX)
                val py = toPageY(mathY).coerceIn(top - 20.0, bottom + 20.0)
                currentSegment.add(StrokePoint(px, py, curveWidth))
            } else {
                if (currentSegment.size >= 2) {
                    elements.add(
                        Stroke(
                            tool = Tool.PEN,
                            color = strokeColor,
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
                    color = strokeColor,
                    capStyle = "round",
                    points = currentSegment.toList(),
                    uniformWidth = true
                )
            )
        }

        return elements
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
    private class MathParser(private val str: String, private val xVal: Double) {
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
                while (ch in 'a'.code..'z'.code) nextChar()
                val func = str.substring(startPos, pos)
                v = when (func) {
                    "x" -> xVal
                    "pi" -> PI
                    "e" -> E
                    else -> {
                        val arg = parseFactor()
                        when (func) {
                            "sin" -> sin(arg)
                            "cos" -> cos(arg)
                            "tan" -> if (abs(cos(arg)) < 1e-9) Double.NaN else tan(arg)
                            "asin" -> asin(arg)
                            "acos" -> acos(arg)
                            "atan" -> atan(arg)
                            "sinh" -> sinh(arg)
                            "cosh" -> cosh(arg)
                            "tanh" -> tanh(arg)
                            "sqrt" -> if (arg >= 0) sqrt(arg) else Double.NaN
                            "abs" -> abs(arg)
                            "exp" -> exp(arg)
                            "ln", "log" -> if (arg > 0) ln(arg) else Double.NaN
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
