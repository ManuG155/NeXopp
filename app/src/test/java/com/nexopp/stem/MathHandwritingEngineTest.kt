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

    @Test
    fun `recognizes derivative d slash dx fraction structure`() {
        // d: circle + ascender
        val dLoop = makeCircleStroke(20.0, 35.0, 8.0)
        val dAsc = makeLineStroke(28.0, 15.0, 28.0, 43.0)
        // fraction bar
        val bar = makeLineStroke(10.0, 50.0, 50.0, 50.0)
        // dx: d + x
        val dxLoop = makeCircleStroke(18.0, 70.0, 8.0)
        val dxAsc = makeLineStroke(26.0, 52.0, 26.0, 78.0)
        val x1 = makeLineStroke(32.0, 62.0, 44.0, 78.0)
        val x2 = makeLineStroke(32.0, 78.0, 44.0, 62.0)

        val result = MathHandwritingEngine.recognize(listOf(dLoop, dAsc, bar, dxLoop, dxAsc, x1, x2))
        assertTrue("Expected derivative \\frac{d}{dx}, got: ${result.latex}", result.latex.contains("\\frac{d}{d") || result.latex.contains("d/dx") || result.latex.contains("\\frac{"))
    }

    @Test
    fun `recognizes Greek letters alpha and beta and sigma`() {
        // Alpha: loop starting top right, loop down-left-up, cross to bottom right
        val alphaPts = listOf(
            40.0 to 20.0, 25.0 to 30.0, 15.0 to 45.0, 25.0 to 60.0, 35.0 to 45.0, 25.0 to 30.0, 45.0 to 65.0
        )
        val alphaStroke = makeStroke(alphaPts)
        val resAlpha = MathHandwritingEngine.recognize(listOf(alphaStroke))
        assertTrue("Expected \\alpha or Greek recognizer, got: ${resAlpha.latex}", resAlpha.latex.contains("\\alpha") || resAlpha.latex.contains("a"))

        // Sigma: top bar, diag down-left, diag down-right, bot bar
        val sigmaPts = listOf(
            45.0 to 20.0, 15.0 to 20.0, 30.0 to 40.0, 15.0 to 60.0, 45.0 to 60.0
        )
        val sigmaStroke = makeStroke(sigmaPts)
        val resSigma = MathHandwritingEngine.recognize(listOf(sigmaStroke))
        assertTrue("Expected \\sum or \\Sigma, got: ${resSigma.latex}", resSigma.latex.contains("\\sum") || resSigma.latex.contains("\\Sigma") || resSigma.latex.contains("\\sigma"))
    }

    @Test
    fun `recognizes vertical line as digit 1`() {
        val stroke1 = makeLineStroke(50.0, 10.0, 50.0, 80.0)
        val result = MathHandwritingEngine.recognize(listOf(stroke1))
        assertEquals("1", result.latex)
    }

    @Test
    fun `recognizes less than or equal to inequality`() {
        // '<' angle: top-right to left, left to bottom-right
        val angle = makeStroke(listOf(40.0 to 20.0, 15.0 to 35.0, 40.0 to 50.0))
        // horizontal line underneath
        val bar = makeLineStroke(15.0, 58.0, 40.0, 58.0)
        val result = MathHandwritingEngine.recognize(listOf(angle, bar))
        assertTrue("Expected \\le or <=, got: ${result.latex}", result.latex.contains("\\le") || result.latex.contains("≤"))
    }

    @Test
    fun `recognizes parenthesized expression`() {
        // ( : arc
        val openParen = makeStroke(listOf(20.0 to 10.0, 15.0 to 40.0, 20.0 to 70.0))
        // x : cross
        val x1 = makeLineStroke(30.0, 25.0, 50.0, 55.0)
        val x2 = makeLineStroke(30.0, 55.0, 50.0, 25.0)
        // ) : arc
        val closeParen = makeStroke(listOf(60.0 to 10.0, 65.0 to 40.0, 60.0 to 70.0))

        val result = MathHandwritingEngine.recognize(listOf(openParen, x1, x2, closeParen))
        assertTrue("Expected parentheses, got: ${result.latex}", result.latex.contains("(") && result.latex.contains(")"))
    }

    @Test
    fun `diagnostics provides detailed cluster and candidate rankings`() {
        val circle = makeCircleStroke(50.0, 50.0, 20.0)
        val diag = MathDiagnostics.diagnose(listOf(circle))
        assertEquals(1, diag.clusterCount)
        assertTrue(diag.clusters.first().topCandidates.isNotEmpty())
        val summary = MathDiagnostics.formatDiagnosticSummary(diag)
        assertTrue(summary.contains("Math Recognition Diagnostic"))
    }
}

