package com.nexopp.render

import android.graphics.Bitmap
import android.graphics.Canvas
import com.nexopp.format.model.Background
import com.nexopp.format.model.Page
import java.io.OutputStream

/**
 * High-resolution raster image exporter for [Page] objects.
 * Renders paper styles, backgrounds, and vector elements at configurable DPI scale (1x, 2x, 3x).
 */
class ImageExporter(
    private val pdfSource: PdfPageCache? = null,
    private val imageSource: ImageBackgroundCache? = null,
) {

    enum class Format(val extension: String, val mimeType: String, val compressFormat: Bitmap.CompressFormat) {
        PNG("png", "image/png", Bitmap.CompressFormat.PNG),
        JPEG("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
        WEBP("webp", "image/webp", Bitmap.CompressFormat.WEBP)
    }

    /**
     * Renders [page] to an Android [Bitmap] at [scale] factor.
     * Scale 1.0 = ~72 DPI (screen point size), 2.0 = ~144 DPI (high-res), 3.0 = ~216-300 DPI (print ready).
     */
    fun renderToBitmap(page: Page, scale: Float = 2.0f): Bitmap? {
        if (page.width <= 0.0 || page.height <= 0.0) return null

        val widthPx = (page.width * scale).toInt().coerceAtLeast(1)
        val heightPx = (page.height * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Page Background (Color + Technical Grid / Lines)
        val box = PageBox(
            index = 0,
            topPx = 0f,
            leftPx = 0f,
            heightPx = heightPx.toFloat(),
            scale = scale,
            page = page
        )

        var bgBmp: Bitmap? = null
        val bg = page.background
        if (bg is Background.Pdf && pdfSource != null) {
            bgBmp = pdfSource.render(bg.pageNo, widthPx)
        } else if (bg is Background.Pixmap && imageSource != null) {
            bgBmp = imageSource.render(bg.filename, widthPx)
        }

        BackgroundRenderer.draw(
            canvas = canvas,
            box = box,
            scrollX = 0f,
            scrollY = 0f,
            pageImage = bgBmp
        )

        // 2. Draw Elements (Strokes, Text, LaTeX, Images)
        val elements = ElementRenderer()
        val strokePainter = StrokePainter()
        try {
            PageRenderer.drawElements(
                canvas = canvas,
                page = page,
                scale = scale,
                offsetX = 0f,
                offsetY = 0f,
                strokes = strokePainter,
                elements = elements
            )
        } finally {
            elements.close()
        }

        return bitmap
    }

    /**
     * Exports [page] compressed into [outputStream].
     */
    fun exportPage(
        page: Page,
        outputStream: OutputStream,
        format: Format = Format.PNG,
        quality: Int = 95,
        scale: Float = 2.0f
    ): Boolean {
        val bmp = renderToBitmap(page, scale) ?: return false
        try {
            return bmp.compress(format.compressFormat, quality, outputStream)
        } finally {
            bmp.recycle()
        }
    }
}
