package com.nexopp.render

import com.nexopp.format.model.Stroke
import com.nexopp.format.model.TextElement
import org.junit.Assert.*
import org.junit.Test

class TableBuilderTest {

    @Test
    fun `builds 3x3 table with outer border and divider strokes`() {
        val spec = TableBuilder.TableSpec(
            rows = 3,
            cols = 3,
            x = 100.0,
            y = 150.0,
            colWidth = 80.0,
            rowHeight = 30.0,
            cellTexts = mapOf(
                (0 to 0) to "X",
                (0 to 1) to "Y",
                (1 to 0) to "10",
                (1 to 1) to "20"
            )
        )
        val elements = TableBuilder.build(spec)

        // 1 outer border + 2 horizontal dividers + 2 vertical dividers = 5 strokes
        val strokes = elements.filterIsInstance<Stroke>()
        assertEquals(5, strokes.size)

        // 4 text elements
        val texts = elements.filterIsInstance<TextElement>()
        assertEquals(4, texts.size)

        // Total elements = 9
        assertEquals(9, elements.size)
    }

    @Test
    fun `table dimensions match rows and cols`() {
        val spec = TableBuilder.TableSpec(
            rows = 4,
            cols = 5,
            x = 50.0,
            y = 50.0,
            colWidth = 100.0,
            rowHeight = 25.0
        )
        assertEquals(500.0, spec.totalWidth, 0.001)
        assertEquals(100.0, spec.totalHeight, 0.001)
    }
}
