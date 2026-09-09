package com.nexopp.recognition

import com.nexopp.format.model.LineStyle
import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import com.nexopp.format.model.Tool
import com.nexopp.library.Notebook
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OfflineRecognitionEngineTest {

    private lateinit var engine: OfflineHeuristicRecognitionEngine
    private lateinit var indexer: StrokeSearchIndexer

    @Before
    fun setup() {
        engine = OfflineHeuristicRecognitionEngine()
        indexer = StrokeSearchIndexer(engine)
    }

    private fun createHorizontalStroke(startX: Double, endX: Double, y: Double): Stroke {
        val points = mutableListOf<StrokePoint>()
        val steps = 10
        for (i in 0..steps) {
            val x = startX + (endX - startX) * (i.toDouble() / steps)
            points.add(StrokePoint(x, y, 2.0))
        }
        return Stroke(Tool.PEN, 0xFF000000.toInt(), null, points, true)
    }

    private fun createVerticalStroke(x: Double, startY: Double, endY: Double): Stroke {
        val points = mutableListOf<StrokePoint>()
        val steps = 10
        for (i in 0..steps) {
            val y = startY + (endY - startY) * (i.toDouble() / steps)
            points.add(StrokePoint(x, y, 2.0))
        }
        return Stroke(Tool.PEN, 0xFF000000.toInt(), null, points, true)
    }

    private fun createScratchOutStroke(): Stroke {
        val points = mutableListOf<StrokePoint>()
        var currX = 50.0
        var currY = 50.0
        for (i in 0..12) {
            currX = if (i % 2 == 0) 80.0 else 50.0
            currY += 2.0
            points.add(StrokePoint(currX, currY, 2.0))
        }
        return Stroke(Tool.PEN, 0xFF000000.toInt(), null, points, true)
    }

    @Test
    fun testEmptyStrokesRecognition() {
        val res = engine.recognize(emptyList())
        assertEquals("", res.text)
        assertEquals(0, res.strokeCount)

        val mathRes = engine.recognizeMath(emptyList())
        assertEquals("", mathRes.text)
        assertEquals("", mathRes.latex)
    }

    @Test
    fun testScratchOutGestureDetection() {
        val scratch = createScratchOutStroke()
        val gesture = engine.detectGesture(listOf(scratch))
        assertNotNull(gesture)
        assertEquals(GestureKind.SCRATCH_OUT_ERASE, gesture!!.kind)
    }

    @Test
    fun testUnderlineGestureDetection() {
        val underline = createHorizontalStroke(10.0, 150.0, 200.0)
        val gesture = engine.detectGesture(listOf(underline))
        assertNotNull(gesture)
        assertEquals(GestureKind.UNDERLINE_EMPHASIS, gesture!!.kind)
    }

    @Test
    fun testPlusSymbolRecognition() {
        val h = createHorizontalStroke(20.0, 40.0, 30.0)
        val v = createVerticalStroke(30.0, 20.0, 40.0)
        val res = engine.recognize(listOf(h, v))
        assertTrue(res.text.contains("+"))
    }

    @Test
    fun testEqualsSymbolRecognition() {
        val h1 = createHorizontalStroke(20.0, 40.0, 25.0)
        val h2 = createHorizontalStroke(20.0, 40.0, 35.0)
        val res = engine.recognize(listOf(h1, h2))
        assertTrue(res.text.contains("="))
    }

    @Test
    fun testMathFractionRecognition() {
        // Numerator '1' above (x: 25, y: 10..22)
        val num = createVerticalStroke(25.0, 10.0, 22.0)
        // Fraction bar (x: 10..40, y: 25)
        val bar = createHorizontalStroke(10.0, 40.0, 25.0)
        // Denominator '1' below (x: 25, y: 28..40)
        val den = createVerticalStroke(25.0, 28.0, 40.0)

        val res = engine.recognizeMath(listOf(num, bar, den))
        assertNotNull(res.latex)
        assertTrue(res.latex!!.contains("\\frac{"))
    }

    @Test
    fun testStrokeSearchIndexer() {
        val nb = Notebook(
            id = "nb_123",
            subjectId = "sub_math",
            name = "Cálculo I",
            fileName = "calculo.xopp"
        )

        val h = createHorizontalStroke(20.0, 40.0, 30.0)
        val v = createVerticalStroke(30.0, 20.0, 40.0)
        indexer.indexPageStrokes("nb_123", 0, listOf(h, v))

        val results = indexer.searchContent("+", nb)
        assertEquals(1, results.size)
        assertEquals("nb_123", results[0].notebook.id)
        assertTrue(results[0].matchSnippet.contains("Pág. 1"))
    }
}
