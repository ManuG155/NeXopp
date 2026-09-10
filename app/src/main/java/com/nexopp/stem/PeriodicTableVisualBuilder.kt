package com.nexopp.stem

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.nexopp.format.model.ImageElement
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Builds real visual graphical elements (ImageElement containing PNG raster data)
 * representing the Periodic Table element card (matching user's reference)
 * and the complete 18x7 visual IUPAC periodic table.
 */
object PeriodicTableVisualBuilder {

    /**
     * Builds a single Element Card matching the attached reference image:
     * Dark green rounded container, bright green border, "Z = 1" at top-left,
     * atomic mass "1.008 u" at top-right, large bold symbol "H" in center,
     * and full name "Hidrógeno" at bottom center.
     */
    fun buildElementCard(
        element: ChemicalElement,
        originX: Double = 80.0,
        originY: Double = 120.0,
        widthPt: Double = 180.0,
        heightPt: Double = 110.0
    ): ImageElement {
        val density = 3.0f
        val bmpW = (widthPt * density).toInt().coerceAtLeast(300)
        val bmpH = (heightPt * density).toInt().coerceAtLeast(180)

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cardRect = RectF(12f, 12f, (bmpW - 12).toFloat(), (bmpH - 12).toFloat())
        val cornerRadius = 32f

        // 1. Background (deep dark forest green matching reference)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF142E22.toInt() // Dark forest green
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        // 2. Card Border (bright green matching reference #34C759)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF34C759.toInt() // Bright iOS/Material vibrant green
            style = Paint.Style.STROKE
            strokeWidth = 7f
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        // 3. Top Left: "Z = {atomicNumber}"
        textPaint.textSize = 34f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Z = ${element.atomicNumber}", cardRect.left + 36f, cardRect.top + 52f, textPaint)

        // 4. Top Right: "{mass} u"
        textPaint.textSize = 32f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textAlign = Paint.Align.RIGHT
        val massStr = String.format(Locale.US, "%.3f u", element.atomicMass)
        canvas.drawText(massStr, cardRect.right - 36f, cardRect.top + 52f, textPaint)

        // 5. Center: Huge Bold Symbol (e.g. "H")
        textPaint.textSize = 120f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        val centerY = cardRect.centerY() + 32f
        canvas.drawText(element.symbol, cardRect.centerX(), centerY, textPaint)

        // 6. Bottom Center: Element Name (e.g. "Hidrógeno")
        textPaint.textSize = 38f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(element.name, cardRect.centerX(), cardRect.bottom - 36f, textPaint)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val data = stream.toByteArray()
        bitmap.recycle()

        return ImageElement(
            left = originX,
            top = originY,
            right = originX + widthPt,
            bottom = originY + heightPt,
            data = data,
            extraAttrs = mapOf(
                "stem" to "periodic_element_card",
                "atomicNumber" to "${element.atomicNumber}",
                "symbol" to element.symbol,
                "name" to element.name
            )
        )
    }

    /**
     * Builds the complete 18x7 visual periodic table with all 118 elements,
     * IUPAC layout with lanthanides & actinides rows, colored category blocks.
     */
    fun buildFullPeriodicTable(
        originX: Double = 60.0,
        originY: Double = 100.0,
        widthPt: Double = 540.0,
        heightPt: Double = 330.0
    ): ImageElement {
        val density = 2.5f
        val bmpW = (widthPt * density).toInt().coerceAtLeast(800)
        val bmpH = (heightPt * density).toInt().coerceAtLeast(500)

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val pad = 16f
        val cardRect = RectF(pad, pad, bmpW - pad, bmpH - pad)
        val cornerRadius = 24f

        // Card background (sleek dark theme)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF121A21.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2A3B4C.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // Title Header
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("TABLA PERIÓDICA DE LOS ELEMENTOS (IUPAC)", cardRect.left + 24f, cardRect.top + 42f, titlePaint)

        // Grid parameters: 18 columns + period gutter, 7 periods + 2 rows for Ln/Ac
        val allElements = PeriodicTableRegistry.elements
        val gridMap = mutableMapOf<Pair<Int, Int>, ChemicalElement>()
        for (el in allElements) {
            if (el.atomicNumber !in 57..71 && el.atomicNumber !in 89..103) {
                gridMap[el.period to el.group] = el
            }
        }
        val lanthanides = allElements.filter { it.atomicNumber in 57..71 }.sortedBy { it.atomicNumber }
        val actinides = allElements.filter { it.atomicNumber in 89..103 }.sortedBy { it.atomicNumber }

        val startX = cardRect.left + 24f
        val startY = cardRect.top + 64f
        val cellW = (cardRect.width() - 48f) / 18.5f
        val cellH = (cardRect.height() - 88f) / 9.8f
        val gap = 3f

        val cellBg = Paint(Paint.ANTI_ALIAS_FLAG)
        val cellBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEEFFFFFF.toInt()
            textSize = cellH * 0.28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val symPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = cellH * 0.44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        fun drawCell(x: Float, y: Float, el: ChemicalElement) {
            val rect = RectF(x, y, x + cellW - gap, y + cellH - gap)
            cellBg.color = (el.category.colorHex.toInt() and 0x00FFFFFF) or 0xDD000000.toInt()
            canvas.drawRoundRect(rect, 4f, 4f, cellBg)
            cellBorder.color = el.category.colorHex.toInt()
            canvas.drawRoundRect(rect, 4f, 4f, cellBorder)

            canvas.drawText("${el.atomicNumber}", rect.left + 3f, rect.top + cellH * 0.32f, numPaint)
            canvas.drawText(el.symbol, rect.centerX(), rect.centerY() + cellH * 0.28f, symPaint)
        }

        // Draw main 18x7 table
        for (p in 1..7) {
            val y = startY + (p - 1) * cellH
            for (g in 1..18) {
                val x = startX + (g - 1) * cellW
                if (p == 6 && g == 3) {
                    val rect = RectF(x, y, x + cellW - gap, y + cellH - gap)
                    cellBg.color = 0x556D4C41
                    canvas.drawRoundRect(rect, 4f, 4f, cellBg)
                    symPaint.textSize = cellH * 0.32f
                    canvas.drawText("57-71", rect.centerX(), rect.centerY() + 6f, symPaint)
                    symPaint.textSize = cellH * 0.44f
                } else if (p == 7 && g == 3) {
                    val rect = RectF(x, y, x + cellW - gap, y + cellH - gap)
                    cellBg.color = 0x55546E7A
                    canvas.drawRoundRect(rect, 4f, 4f, cellBg)
                    symPaint.textSize = cellH * 0.32f
                    canvas.drawText("89-103", rect.centerX(), rect.centerY() + 6f, symPaint)
                    symPaint.textSize = cellH * 0.44f
                } else {
                    val el = gridMap[p to g]
                    if (el != null) drawCell(x, y, el)
                }
            }
        }

        // Draw Lanthanides (row 8)
        val lnY = startY + 7.4f * cellH
        for (i in lanthanides.indices) {
            val x = startX + (i + 2) * cellW
            drawCell(x, lnY, lanthanides[i])
        }

        // Draw Actinides (row 9)
        val acY = startY + 8.5f * cellH
        for (i in actinides.indices) {
            val x = startX + (i + 2) * cellW
            drawCell(x, acY, actinides[i])
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val data = stream.toByteArray()
        bitmap.recycle()

        return ImageElement(
            left = originX,
            top = originY,
            right = originX + widthPt,
            bottom = originY + heightPt,
            data = data,
            extraAttrs = mapOf("stem" to "periodic_table_full")
        )
    }
}
