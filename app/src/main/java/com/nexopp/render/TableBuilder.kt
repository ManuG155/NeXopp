// Ruta: app/src/main/java/com/nexopp/render/TableBuilder.kt
package com.nexopp.render

import com.nexopp.format.model.Element
import com.nexopp.format.model.LineStyle
import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import com.nexopp.format.model.TextElement
import com.nexopp.format.model.Tool

/**
 * Generates table geometry (grid strokes and cell text elements) for insertion onto the canvas.
 * Guaranteed 100% compatible with standard XOPP strokes and text elements for lossless round-trip.
 */
object TableBuilder {

    data class TableSpec(
        val rows: Int,
        val cols: Int,
        val x: Double,
        val y: Double,
        val colWidth: Double = 75.0,
        val rowHeight: Double = 24.0,
        val borderColor: Int = 0xFF000000.toInt(),
        val borderWidth: Double = 1.2,
        val headerFillAlpha: Int? = 0x22,
        val cellTexts: Map<Pair<Int, Int>, String> = emptyMap(),
        val font: String = "Sans",
        val fontSize: Double = 11.0,
        val textColor: Int = 0xFF000000.toInt()
    ) {
        val totalWidth: Double get() = cols * colWidth
        val totalHeight: Double get() = rows * rowHeight
    }

    fun build(spec: TableSpec): List<Element> {
        val elements = mutableListOf<Element>()
        val totalW = spec.totalWidth
        val totalH = spec.totalHeight

        // 1. Outer Border Rectangle Stroke
        val outerPts = listOf(
            StrokePoint(spec.x, spec.y, spec.borderWidth),
            StrokePoint(spec.x + totalW, spec.y, spec.borderWidth),
            StrokePoint(spec.x + totalW, spec.y + totalH, spec.borderWidth),
            StrokePoint(spec.x, spec.y + totalH, spec.borderWidth),
            StrokePoint(spec.x, spec.y, spec.borderWidth)
        )
        elements.add(
            Stroke(
                tool = Tool.PEN,
                color = spec.borderColor,
                capStyle = "round",
                points = outerPts,
                uniformWidth = true,
                lineStyle = LineStyle.PLAIN,
                extraAttrs = mapOf("stem" to "table_border")
            )
        )

        // 2. Horizontal Divider Lines
        for (r in 1 until spec.rows) {
            val lineY = spec.y + r * spec.rowHeight
            val linePts = listOf(
                StrokePoint(spec.x, lineY, spec.borderWidth * 0.8),
                StrokePoint(spec.x + totalW, lineY, spec.borderWidth * 0.8)
            )
            elements.add(
                Stroke(
                    tool = Tool.PEN,
                    color = spec.borderColor,
                    capStyle = "round",
                    points = linePts,
                    uniformWidth = true,
                    lineStyle = LineStyle.PLAIN,
                    extraAttrs = mapOf("stem" to "table_hdiv")
                )
            )
        }

        // 3. Vertical Divider Lines
        for (c in 1 until spec.cols) {
            val lineX = spec.x + c * spec.colWidth
            val linePts = listOf(
                StrokePoint(lineX, spec.y, spec.borderWidth * 0.8),
                StrokePoint(lineX, spec.y + totalH, spec.borderWidth * 0.8)
            )
            elements.add(
                Stroke(
                    tool = Tool.PEN,
                    color = spec.borderColor,
                    capStyle = "round",
                    points = linePts,
                    uniformWidth = true,
                    lineStyle = LineStyle.PLAIN,
                    extraAttrs = mapOf("stem" to "table_vdiv")
                )
            )
        }

        // 4. Cell Text Elements
        for (r in 0 until spec.rows) {
            for (c in 0 until spec.cols) {
                val text = spec.cellTexts[r to c]?.trim()
                if (!text.isNullOrBlank()) {
                    val cellLeft = spec.x + c * spec.colWidth + 4.0
                    val cellTop = spec.y + r * spec.rowHeight + 4.0
                    elements.add(
                        TextElement(
                            font = spec.font,
                            size = spec.fontSize,
                            x = cellLeft,
                            y = cellTop,
                            color = spec.textColor,
                            content = text,
                            extraAttrs = mapOf("stem" to "table_cell", "row" to "$r", "col" to "$c")
                        )
                    )
                }
            }
        }

        return elements
    }
}
