package com.nexopp.stem

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class AdvancedFunctionPlotterTest {

    private val eps = 1e-2

    @Test
    fun `finds numerical roots of polynomial and trigonometric functions`() {
        // x^2 - 4 = 0 -> x = -2, 2
        val roots = FunctionPlotter.findRoots("x^2 - 4", -5.0, 5.0)
        assertEquals(2, roots.size)
        assertTrue(roots.any { abs(it - (-2.0)) < eps })
        assertTrue(roots.any { abs(it - 2.0) < eps })

        // sin(x) = 0 on [1, 4] -> x = pi (3.14159)
        val sinRoots = FunctionPlotter.findRoots("sin(x)", 1.0, 4.0)
        assertEquals(1, sinRoots.size)
        assertTrue(abs(sinRoots[0] - PI) < eps)
    }

    @Test
    fun `finds numerical intersections between two functions`() {
        // f(x) = x^2, g(x) = x -> x = 0, 1
        val inters = FunctionPlotter.findIntersections("x^2", "x", -2.0, 2.0)
        assertEquals(2, inters.size)
        assertTrue(inters.any { abs(it.first - 0.0) < eps && abs(it.second - 0.0) < eps })
        assertTrue(inters.any { abs(it.first - 1.0) < eps && abs(it.second - 1.0) < eps })
    }

    @Test
    fun `generates advanced multi-function plot with parametric and grid`() {
        val fns = listOf(
            PlotFunctionItem(formula = "sin(x)", color = 0xFF1976D2.toInt()),
            PlotFunctionItem(formula = "cos(x)", color = 0xFFD32F2F.toInt())
        )
        val pFns = listOf(
            ParametricFunctionItem(
                formulaX = "cos(t)",
                formulaY = "sin(t)",
                tMin = 0.0,
                tMax = 2 * PI,
                color = 0xFF388E3C.toInt()
            )
        )
        val pts = listOf(PlotPoint(0.0, 1.0, "Pico"))

        val elements = FunctionPlotter.generateAdvancedPlotElements(
            functions = fns,
            parametricFunctions = pFns,
            points = pts,
            originX = 150.0,
            originY = 150.0,
            plotWidthPt = 200.0,
            plotHeightPt = 200.0,
            gridStyle = PlotGridStyle.SUBTLE,
            highlightRoots = true,
            highlightIntersections = true
        )

        assertTrue(elements.isNotEmpty())
        // Should contain grid lines, axes, both curves, parametric circle, points and intersection markers
        assertTrue(elements.size >= 10)
    }
}
