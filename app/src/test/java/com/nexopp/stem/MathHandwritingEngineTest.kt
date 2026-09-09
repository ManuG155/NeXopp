package com.nexopp.stem

import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import com.nexopp.format.model.Tool
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MathHandwritingEngineTest {

    private fun makeStroke(points: List<Pair<Double, Double>>): Stroke {
        val pts = points.map { StrokePoint(it.first, it.second, 1.5) }
        return Stroke(
            tool = Tool.PEN,
            color = 0xFF000000.toInt(),
            capStyle = "round",
            points = pts,
            uniformWidth = false
        )
    }

    private fun makeLineStroke(x1: Double, y1: Double, x2: Double, y2: Double, steps: Int = 10): Stroke {
        val pts = (0..steps).map { i ->
            val t = i.toDouble() / steps
            (x1 + (x2 - x1) * t) to (y1 + (y2 - y1) * t)
        }
        return makeStroke(pts)
    }

    private fun makeCircleStroke(cx: Double, cy: Double, r: Double, steps: Int = 30): Stroke {
        val pts = (0..steps).map { i ->
            val theta = i.toDouble() / steps * 2 * PI
            (cx + r * cos(theta)) to (cy + r * sin(theta))
        }
        return makeStroke(pts)
    }

    private fun makeIntegralStroke(x: Double, yTop: Double, yBottom: Double): Stroke {
        val steps = 20
        val pts = (0..steps).map { i ->
            val t = i.toDouble() / steps
            val y = yTop + (yBottom - yTop) * t
            // S-shape bend: top hooks right, bottom hooks left
            val xOffset = 5.0 * sin(2 * PI * t)
            (x + xOffset) to y
        }
        return makeStroke(pts)
    }

    private fun makeRadicalStroke(x: Double, y: Double, w: Double, h: Double): Stroke {
        // Radical: little check up/down, down to bottom, up to top, then bar across top
        val pts = listOf(
            x to (y + h * 0.6),
            (x + w * 0.15) to (y + h),
            (x + w * 0.3) to y,
            (x + w) to y
        )
        return makeStroke(pts)
    }

    @Test
    fun `recognizes horizontal line as minus or fraction bar`() {
        val minusStroke = makeLineStroke(10.0, 50.0, 60.0, 50.0)
        val result = MathHandwritingEngine.recognize(listOf(minusStroke))
        assertEquals("-", result.latex)
    }

    @Test
    fun `recognizes plus sign from two intersecting strokes`() {
        val hStroke = makeLineStroke(10.0, 50.0, 50.0, 50.0)
        val vStroke = makeLineStroke(30.0, 30.0, 30.0, 70.0)
        val result = MathHandwritingEngine.recognize(listOf(hStroke, vStroke))
        assertEquals("+", result.latex)
    }

    @Test
    fun `recognizes equals sign from two parallel horizontal strokes`() {
        val topStroke = makeLineStroke(10.0, 45.0, 50.0, 45.0)
        val botStroke = makeLineStroke(10.0, 55.0, 50.0, 55.0)
        val result = MathHandwritingEngine.recognize(listOf(topStroke, botStroke))
        assertEquals("=", result.latex)
    }

    @Test
    fun `recognizes circle as zero or letter o`() {
        val circleStroke = makeCircleStroke(50.0, 50.0, 20.0)
        val result = MathHandwritingEngine.recognize(listOf(circleStroke))
        assertTrue(result.latex == "0" || result.latex == "O" || result.latex == "o")
    }

    @Test
    fun `recognizes integral stroke`() {
        val intStroke = makeIntegralStroke(50.0, 10.0, 90.0)
        val result = MathHandwritingEngine.recognize(listOf(intStroke))
        assertEquals("\\int", result.latex)
    }

    @Test
    fun `recognizes square root structure`() {
        val radStroke = makeRadicalStroke(10.0, 20.0, 80.0, 60.0)
        val xStroke1 = makeLineStroke(40.0, 40.0, 60.0, 60.0)
        val xStroke2 = makeLineStroke(40.0, 60.0, 60.0, 40.0)
        val result = MathHandwritingEngine.recognize(listOf(radStroke, xStroke1, xStroke2))
        assertTrue("Expected \\sqrt{...}, got: ${result.latex}", result.latex.startsWith("\\sqrt{"))
    }

    @Test
    fun `recognizes fraction structure with numerator and denominator`() {
        // Numerator '1': vertical line at y = 10..30
        val numStroke = makeLineStroke(50.0, 10.0, 50.0, 30.0)
        // Fraction bar: horizontal line at y = 45
        val barStroke = makeLineStroke(20.0, 45.0, 80.0, 45.0)
        // Denominator '2': stroke at y = 60..80
        val denStroke = makeLineStroke(50.0, 60.0, 50.0, 80.0)

        val result = MathHandwritingEngine.recognize(listOf(numStroke, barStroke, denStroke))
        assertTrue("Expected \\frac{...}{...}, got: ${result.latex}", result.latex.startsWith("\\frac{"))
    }

    @Test
    fun `recognizes superscript power structure`() {
        // Base 'x' at (20, 50) size 30x30
        val x1 = makeLineStroke(20.0, 40.0, 40.0, 70.0)
        val x2 = makeLineStroke(20.0, 70.0, 40.0, 40.0)
        // Exponent '2' at (48, 25) size 15x15
        val exp1 = makeLineStroke(48.0, 25.0, 58.0, 25.0)
        val exp2 = makeLineStroke(48.0, 35.0, 58.0, 35.0)

        val result = MathHandwritingEngine.recognize(listOf(x1, x2, exp1, exp2))
        assertTrue("Expected power ^, got: ${result.latex}", result.latex.contains("^"))
    }

    @Test
    fun `unknown ambiguous stroke produces Box without arbitrarily defaulting to x`() {
        // A chaotic scribble that doesn't match standard digits or operators
        val pts = listOf(
            10.0 to 10.0, 50.0 to 90.0, 15.0 to 85.0, 45.0 to 15.0, 30.0 to 50.0, 10.0 to 90.0
        )
        val chaoticStroke = makeStroke(pts)
        val result = MathHandwritingEngine.recognize(listOf(chaoticStroke))
        assertTrue("Expected unknown symbol Box, got: ${result.latex}", result.latex.contains("\\Box") || result.hasUnknownSymbols)
        assertNotEquals("Should not silently assume single clean x", "x", result.latex)
    }
}
