// Ruta: app/src/main/java/com/nexopp/render/BackgroundRenderer.kt
package com.nexopp.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import com.nexopp.format.model.Background

object BackgroundRenderer {

    private const val RULE_SPACING_PT = BackgroundGrid.RULE_SPACING_PT
    private const val GRID_SPACING_PT = BackgroundGrid.GRID_SPACING_PT
    private const val MARGIN_PT = BackgroundGrid.MARGIN_PT

    private val fill = Paint()
    private val line = Paint().apply { color = 0xFF000000.toInt() or BackgroundGrid.LINE_RGB; strokeWidth = 1f }
    private val margin = Paint().apply { color = 0xFF000000.toInt() or BackgroundGrid.MARGIN_RGB; strokeWidth = 1.5f }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt() or BackgroundGrid.DOT_RGB; style = Paint.Style.FILL
    }
    private val image = Paint(Paint.FILTER_BITMAP_FLAG)

    // Nuevos Pinceles para Plantillas de Ingeniería (Cian Técnico)
    private val minorLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x220088CC.toInt(); strokeWidth = 1f }
    private val mediumLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x440088CC.toInt(); strokeWidth = 1.5f }
    private val majorLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x770088CC.toInt(); strokeWidth = 2f }
    private val polarLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x330088CC.toInt(); strokeWidth = 1.5f }

    private val dst = RectF()

    fun draw(
        canvas: Canvas,
        box: PageBox,
        scrollX: Float,
        scrollY: Float,
        pageImage: Bitmap? = null,
        tiles: List<PdfTile> = emptyList(),
        viewWidthPx: Float = 0f,
        viewHeightPx: Float = 0f,
    ) {
        val left = box.toViewX(0.0, scrollX)
        val top = box.toViewY(0.0, scrollY)
        val solid = box.page.background as? Background.Solid
        fill.color = solid?.color ?: AndroidColor.WHITE
        canvas.drawRect(left, top, left + box.widthPx, top + box.heightPx, fill)
        
        if (pageImage != null || tiles.isNotEmpty()) {
            if (pageImage != null && !tilesCover(tiles, box, left, top, viewWidthPx, viewHeightPx)) {
                dst.set(left, top, left + box.widthPx, top + box.heightPx)
                canvas.drawBitmap(pageImage, null, dst, image)
            }
            for (t in tiles) {
                dst.set(
                    left + t.left * box.widthPx, top + t.top * box.heightPx,
                    left + t.right * box.widthPx, top + t.bottom * box.heightPx,
                )
                canvas.drawBitmap(t.bitmap, null, dst, image)
            }
            return
        }
        when (solid?.style) {
            "lined" -> horizontals(canvas, box, left, top)
            "ruled" -> { horizontals(canvas, box, left, top); marginLine(canvas, box, left, top) }
            "graph" -> grid(canvas, box, left, top)
            "dotted" -> dots(canvas, box, left, top)
            "millimeter" -> millimeter(canvas, box, left, top)
            "isometric" -> isometric(canvas, box, left, top)
            "polar" -> polar(canvas, box, left, top)
            else -> Unit 
        }
    }

    private fun tilesCover(
        tiles: List<PdfTile>, box: PageBox, left: Float, top: Float, viewWidthPx: Float, viewHeightPx: Float,
    ): Boolean {
        if (tiles.isEmpty() || viewWidthPx <= 0f || viewHeightPx <= 0f) return false
        if (box.widthPx <= 0f || box.heightPx <= 0f) return false
        val vl = ((0f - left) / box.widthPx).coerceIn(0f, 1f)
        val vt = ((0f - top) / box.heightPx).coerceIn(0f, 1f)
        val vr = ((viewWidthPx - left) / box.widthPx).coerceIn(0f, 1f)
        val vb = ((viewHeightPx - top) / box.heightPx).coerceIn(0f, 1f)
        if (vr <= vl || vb <= vt) return true
        var ul = Float.MAX_VALUE; var ut = Float.MAX_VALUE
        var ur = -Float.MAX_VALUE; var ub = -Float.MAX_VALUE
        var area = 0f
        for (t in tiles) {
            ul = minOf(ul, t.left); ut = minOf(ut, t.top)
            ur = maxOf(ur, t.right); ub = maxOf(ub, t.bottom)
            area += (t.right - t.left) * (t.bottom - t.top)
        }
        if (ul > vl || ut > vt || ur < vr || ub < vb) return false
        val unionArea = (ur - ul) * (ub - ut)
        return area >= unionArea - 1e-4f
    }

    private fun horizontals(canvas: Canvas, box: PageBox, left: Float, top: Float) {
        for (y in BackgroundGrid.lines(box.page.height, RULE_SPACING_PT)) {
            val py = top + (y * box.scale).toFloat()
            canvas.drawLine(left, py, left + box.widthPx, py, line)
        }
    }

    private fun marginLine(canvas: Canvas, box: PageBox, left: Float, top: Float) {
        val x = left + (MARGIN_PT * box.scale).toFloat()
        canvas.drawLine(x, top, x, top + box.heightPx, margin)
    }

    private fun grid(canvas: Canvas, box: PageBox, left: Float, top: Float) {
        for (y in BackgroundGrid.lines(box.page.height, GRID_SPACING_PT)) {
            val py = top + (y * box.scale).toFloat()
            canvas.drawLine(left, py, left + box.widthPx, py, line)
        }
        for (x in BackgroundGrid.lines(box.page.width, GRID_SPACING_PT)) {
            val px = left + (x * box.scale).toFloat()
            canvas.drawLine(px, top, px, top + box.heightPx, line)
        }
    }

    private fun dots(canvas: Canvas, box: PageBox, left: Float, top: Float) {
        val radius = (box.scale).coerceIn(1f, 2.5f)
        for (y in BackgroundGrid.lines(box.page.height, GRID_SPACING_PT)) {
            val py = top + (y * box.scale).toFloat()
            for (x in BackgroundGrid.lines(box.page.width, GRID_SPACING_PT)) {
                canvas.drawCircle(left + (x * box.scale).toFloat(), py, radius, dot)
            }
        }
    }

    // --- NUEVOS FONDOS DE INGENIERÍA ---

    private fun millimeter(canvas: Canvas, box: PageBox, left: Float, top: Float) {
        val mmPt = 72f / 25.4f // 1 pulgada = 72 pt, 1 pulgada = 25.4 mm
        val mmPx = (mmPt * box.scale).toFloat()
        val width = box.widthPx
        val height = box.heightPx

        var x = 0f
        var i = 0
        while (x < width) {
            val paint = if (i % 10 == 0) majorLine else if (i % 5 == 0) mediumLine else minorLine
            canvas.drawLine(left + x, top, left + x, top + height, paint)
            x += mmPx
            i++
        }

        var y = 0f
        var j = 0
        while (y < height) {
            val paint = if (j % 10 == 0) majorLine else if (j % 5 == 0) mediumLine else minorLine
            canvas.drawLine(left, top + y, left + width, top + y, paint)
            y += mmPx
            j++
        }
    }

    private fun isometric(canvas: Canvas, box: PageBox, left: Float, top: Float) {
        val s = (GRID_SPACING_PT * box.scale).toFloat()
        val h = s * 0.8660254f // cos(30°)
        val width = box.widthPx
        val height = box.heightPx

        // Líneas verticales
        var x = 0f
        while (x < width) {
            canvas.drawLine(left + x, top, left + x, top + height, polarLine)
            x += h
        }

        val slope = 0.57735026f // tan(30°)
        val dy = width * slope

        // Diagonales positivas (/)
        var y0 = -dy
        while (y0 < height) {
            canvas.drawLine(left, top + y0, left + width, top + y0 + dy, polarLine)
            y0 += s
        }

        // Diagonales negativas (\)
        var y1 = 0f
        while (y1 < height + dy) {
            canvas.drawLine(left, top + y1, left + width, top + y1 - dy, polarLine)
            y1 += s
        }
    }

    private fun polar(canvas: Canvas, box: PageBox, left: Float, top: Float) {
        val cx = left + box.widthPx / 2f
        val cy = top + box.heightPx / 2f
        val maxR = kotlin.math.hypot(box.widthPx / 2f, box.heightPx / 2f)
        val step = (GRID_SPACING_PT * box.scale).toFloat()

        // Círculos concéntricos
        var r = step
        while (r < maxR) {
            canvas.drawCircle(cx, cy, r, polarLine)
            r += step
        }

        // Líneas radiales (cada 15 grados)
        for (angle in 0 until 180 step 15) {
            val rad = Math.toRadians(angle.toDouble())
            val dx = (kotlin.math.cos(rad) * maxR).toFloat()
            val dy = (kotlin.math.sin(rad) * maxR).toFloat()
            canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, polarLine)
        }
    }
}