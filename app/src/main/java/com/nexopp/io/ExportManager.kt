package com.nexopp.io

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nexopp.format.model.Document
import com.nexopp.render.ImageBackgroundCache
import com.nexopp.render.ImageExporter
import com.nexopp.render.PdfExporter
import com.nexopp.render.PdfPageCache
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Manages document export to PDF and Images (PNG/JPEG) and sharing via Android Sharesheet.
 */
class ExportManager(
    private val context: Context,
    private val pdfSource: PdfPageCache? = null,
    private val imageSource: ImageBackgroundCache? = null
) {
    enum class ExportFormat { PDF, PNG, JPEG }
    enum class PageScope { ALL, CURRENT, RANGE }

    val exportDir: File = File(context.cacheDir, "exports").apply { mkdirs() }

    /**
     * Parses user page range input like "1-3, 5, 7-10" into 0-based page indices within [maxPages].
     */
    fun parsePageRange(input: String, maxPages: Int): List<Int> {
        if (input.isBlank() || input.equals("all", ignoreCase = true) || input.equals("todo", ignoreCase = true)) {
            return (0 until maxPages).toList()
        }

        val result = mutableSetOf<Int>()
        val parts = input.split(',', ';', ' ')
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains('-')) {
                val rangeParts = trimmed.split('-')
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].trim().toIntOrNull()?.let { it - 1 } ?: continue
                    val end = rangeParts[1].trim().toIntOrNull()?.let { it - 1 } ?: continue
                    val min = start.coerceIn(0, maxPages - 1)
                    val max = end.coerceIn(0, maxPages - 1)
                    for (i in min..max) {
                        result.add(i)
                    }
                }
            } else {
                val single = trimmed.toIntOrNull()?.let { it - 1 }
                if (single != null && single in 0 until maxPages) {
                    result.add(single)
                }
            }
        }
        return result.sorted()
    }

    /**
     * Exports [doc] into [outputStream] according to [format], [pageIndices], and [scale].
     */
    fun exportToStream(
        doc: Document,
        outputStream: OutputStream,
        format: ExportFormat,
        pageIndices: List<Int>,
        scale: Float = 2.0f,
        quality: Int = 95
    ) {
        when (format) {
            ExportFormat.PDF -> {
                val exporter = PdfExporter(pdfSource, imageSource)
                exporter.export(doc, outputStream, pageIndices)
            }
            ExportFormat.PNG, ExportFormat.JPEG -> {
                val imgFormat = if (format == ExportFormat.PNG) ImageExporter.Format.PNG else ImageExporter.Format.JPEG
                val exporter = ImageExporter(pdfSource, imageSource)
                // For a single stream (e.g. single page export), render the first selected page
                val targetPageIdx = pageIndices.firstOrNull() ?: 0
                val page = doc.pages.getOrNull(targetPageIdx) ?: return
                exporter.exportPage(page, outputStream, imgFormat, quality, scale)
            }
        }
    }

    /**
     * Exports [doc] to local cache and creates a shareable [Intent] with Android Sharesheet.
     */
    fun createShareIntent(
        doc: Document,
        documentTitle: String,
        format: ExportFormat,
        pageIndices: List<Int>,
        scale: Float = 2.0f,
        quality: Int = 95
    ): Intent? {
        exportDir.mkdirs()
        val safeTitle = documentTitle.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").ifBlank { "documento" }

        when (format) {
            ExportFormat.PDF -> {
                val file = File(exportDir, "$safeTitle.pdf")
                FileOutputStream(file).use { fos ->
                    exportToStream(doc, fos, ExportFormat.PDF, pageIndices, scale, quality)
                }
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                return Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, safeTitle)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            ExportFormat.PNG, ExportFormat.JPEG -> {
                val ext = if (format == ExportFormat.PNG) "png" else "jpg"
                val mime = if (format == ExportFormat.PNG) "image/png" else "image/jpeg"
                val imgFormat = if (format == ExportFormat.PNG) ImageExporter.Format.PNG else ImageExporter.Format.JPEG
                val exporter = ImageExporter(pdfSource, imageSource)

                if (pageIndices.size == 1) {
                    val pageNo = pageIndices.first() + 1
                    val file = File(exportDir, "${safeTitle}_p$pageNo.$ext")
                    FileOutputStream(file).use { fos ->
                        val page = doc.pages[pageIndices.first()]
                        exporter.exportPage(page, fos, imgFormat, quality, scale)
                    }
                    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    return Intent(Intent.ACTION_SEND).apply {
                        type = mime
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_SUBJECT, safeTitle)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    val uris = ArrayList<Uri>()
                    for (idx in pageIndices) {
                        val page = doc.pages.getOrNull(idx) ?: continue
                        val file = File(exportDir, "${safeTitle}_p${idx + 1}.$ext")
                        FileOutputStream(file).use { fos ->
                            exporter.exportPage(page, fos, imgFormat, quality, scale)
                        }
                        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                    }
                    return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = mime
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        putExtra(Intent.EXTRA_SUBJECT, safeTitle)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            }
        }
    }
}
