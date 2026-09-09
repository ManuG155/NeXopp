package com.nexopp.stem

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FunctionPlotterTest {

    private val eps = 1e-4

    @Test
    fun `evaluates basic arithmetic and powers`() {
        assertEquals(5.0, FunctionPlotter.evaluate("2 + 3", 0.0), eps)
        assertEquals(10.0, FunctionPlotter.evaluate("2 * x", 5.0), eps)
        assertEquals(25.0, FunctionPlotter.evaluate("x^2", 5.0), eps)
        assertEquals(27.0, FunctionPlotter.evaluate("x^3", 3.0), eps)
        assertEquals(7.0, FunctionPlotter.evaluate("2 * x + 1", 3.0), eps)
    }

    @Test
    fun `evaluates trigonometric and transcendental functions`() {
        assertEquals(sin(PI / 2), FunctionPlotter.evaluate("sin(x)", PI / 2), eps)
        assertEquals(1.0, FunctionPlotter.evaluate("cos(0)", 0.0), eps)
        assertEquals(3.0, FunctionPlotter.evaluate("sqrt(x)", 9.0), eps)
        assertEquals(1.0, FunctionPlotter.evaluate("exp(0)", 0.0), eps)
        assertEquals(1.0, FunctionPlotter.evaluate("ln(e)", 0.0), eps)
    }

    @Test
    fun `handles invalid expressions gracefully`() {
        assertTrue(FunctionPlotter.evaluate("1 / x", 0.0).isNaN())
        assertTrue(FunctionPlotter.evaluate("sqrt(x)", -4.0).isNaN())
        assertTrue(FunctionPlotter.evaluate("invalid syntax", 1.0).isNaN())
    }

    @Test
    fun `evaluates natural formula inputs with prefixes and implicit multiplication`() {
        // f(x) = sin(x) + x²
        assertEquals(sin(2.0) + 4.0, FunctionPlotter.evaluate("f(x) = sin(x) + x²", 2.0), eps)
        // y = 2x + 3
        assertEquals(9.0, FunctionPlotter.evaluate("y = 2x + 3", 3.0), eps)
        // f(x) = e^(-x²)
        assertEquals(kotlin.math.exp(-4.0), FunctionPlotter.evaluate("f(x) = e^(-x²)", 2.0), eps)
        // Spanish trig: sen(x), tg(x)
        assertEquals(sin(PI / 2), FunctionPlotter.evaluate("sen(x)", PI / 2), eps)
        assertEquals(kotlin.math.tan(PI / 4), FunctionPlotter.evaluate("tg(x)", PI / 4), eps)
        // Unicode radical: √x
        assertEquals(3.0, FunctionPlotter.evaluate("√x", 9.0), eps)
    }

    @Test
    fun `generates valid vector elements with axes`() {
        val elements = FunctionPlotter.generatePlotElements(
            formula = "sin(x)",
            originX = 200.0,
            originY = 200.0,
            plotWidthPt = 200.0,
            plotHeightPt = 150.0,
            xMin = -PI,
            xMax = PI,
            yMin = -1.5,
            yMax = 1.5,
            drawAxes = true
        )
        assertTrue(elements.isNotEmpty())
        // Should contain axes and the curve stroke
        assertTrue(elements.size >= 3)
    }
}
