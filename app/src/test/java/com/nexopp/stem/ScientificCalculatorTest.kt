package com.nexopp.stem

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class ScientificCalculatorTest {

    private val eps = 1e-4

    @Test
    fun `evaluates arithmetic and nested brackets`() {
        assertEquals(14.0, evaluateExpression("2 + 3 * 4", true), eps)
        assertEquals(20.0, evaluateExpression("(2 + 3) * 4", true), eps)
        assertEquals(2.5, evaluateExpression("5 / 2", true), eps)
    }

    @Test
    fun `evaluates trigonometric in degrees and radians`() {
        // DEG mode
        assertEquals(1.0, evaluateExpression("sin(90)", false), eps)
        assertEquals(0.0, evaluateExpression("cos(90)", false), eps)
        assertEquals(1.0, evaluateExpression("tan(45)", false), eps)

        // RAD mode
        assertEquals(1.0, evaluateExpression("sin(pi / 2)", true), eps)
        assertEquals(-1.0, evaluateExpression("cos(pi)", true), eps)
    }

    @Test
    fun `evaluates powers roots and logarithms`() {
        assertEquals(8.0, evaluateExpression("2^3", true), eps)
        assertEquals(4.0, evaluateExpression("sqrt(16)", true), eps)
        assertEquals(1.0, evaluateExpression("ln(e)", true), eps)
        assertEquals(2.0, evaluateExpression("log10(100)", true), eps)
    }

    @Test
    fun `handles syntax errors and division by zero`() {
        assertTrue(evaluateExpression("5 / 0", true).isNaN())
        assertTrue(evaluateExpression("sin((", true).isNaN())
    }
}
