package com.nexopp.ui

import android.graphics.*
import com.nexopp.format.model.ImageElement
import java.io.ByteArrayOutputStream

object LinkCardVisualBuilder {
    const val CARD_WIDTH_PT = 260.0
    const val CARD_HEIGHT_PT = 76.0

    fun buildLinkCard(
        x: Double,
        y: Double,
        url: String,
        title: String,
        siteName: String?,
        mediaType: LinkMediaType = LinkMediaType.GENERIC
    ): ImageElement {
        val widthPx = (CARD_WIDTH_PT * 2).toInt() // 2x scale for crisp retina display
        val heightPx = (CARD_HEIGHT_PT * 2).toInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw card background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E242B")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mediaType == LinkMediaType.VIDEO) Color.parseColor("#E53935") else Color.parseColor("#3B82F6")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val rect = RectF(2f, 2f, widthPx - 2f, heightPx - 2f)
        canvas.drawRoundRect(rect, 24f, 24f, bgPaint)
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

        // Draw Icon Badge on Left
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mediaType == LinkMediaType.VIDEO) Color.parseColor("#3B1B1B") else Color.parseColor("#1B293B")
            style = Paint.Style.FILL
        }
        val badgeRect = RectF(20f, 20f, 112f, heightPx - 20f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgePaint)

        // Draw Icon inside Badge (Play triangle for video, Link symbol for web)
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mediaType == LinkMediaType.VIDEO) Color.parseColor("#FF5252") else Color.parseColor("#60A5FA")
            style = Paint.Style.FILL
        }
        if (mediaType == LinkMediaType.VIDEO) {
            val playPath = Path().apply {
                val cx = badgeRect.centerX()
                val cy = badgeRect.centerY()
                moveTo(cx - 12f, cy - 18f)
                lineTo(cx + 18f, cy)
                lineTo(cx - 12f, cy + 18f)
                close()
            }
            canvas.drawPath(playPath, iconPaint)
        } else {
            val linkTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#60A5FA")
                textSize = 42f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("🔗", badgeRect.centerX(), badgeRect.centerY() + 14f, linkTextPaint)
        }

        // Draw Site Name / Tag
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mediaType == LinkMediaType.VIDEO) Color.parseColor("#EF4444") else Color.parseColor("#60A5FA")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displaySite = siteName?.ifBlank { null } ?: (if (mediaType == LinkMediaType.VIDEO) "YouTube • Vídeo" else "Enlace Web")
        canvas.drawText(displaySite, 130f, 42f, tagPaint)

        // Draw Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displayTitle = if (title.length > 28) title.take(26) + "…" else title
        canvas.drawText(displayTitle, 130f, 82f, titlePaint)

        // Draw URL / Domain
        val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 20f
            typeface = Typeface.DEFAULT
        }
        val cleanUrl = url.removePrefix("https://").removePrefix("http://")
        val displayUrl = if (cleanUrl.length > 34) cleanUrl.take(32) + "…" else cleanUrl
        canvas.drawText(displayUrl, 130f, 118f, urlPaint)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        bitmap.recycle()

        return ImageElement(
            left = x,
            top = y,
            right = x + CARD_WIDTH_PT,
            bottom = y + CARD_HEIGHT_PT,
            data = bytes,
            extraAttrs = mapOf(
                "stem" to "link_card",
                "url" to url,
                "title" to title,
                "site" to displaySite,
                "mediaType" to mediaType.name
            )
        )
    }
}
