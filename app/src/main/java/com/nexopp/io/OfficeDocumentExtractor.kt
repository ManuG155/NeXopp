package com.nexopp.io

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Pure Kotlin offline text and structure extractor for Office and OpenDocument files:
 * DOCX, XLSX, PPTX, ODT, ODS, ODP, HTML, and EPUB.
 * Converts document structures into formatted Markdown ready for [TextPdfGenerator] typesetting.
 */
object OfficeDocumentExtractor {

    enum class OfficeKind {
        DOCX, XLSX, PPTX, ODT, ODS, ODP, HTML, EPUB, UNKNOWN
    }

    fun detectKind(name: String): OfficeKind {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".docx") -> OfficeKind.DOCX
            lower.endsWith(".xlsx") -> OfficeKind.XLSX
            lower.endsWith(".pptx") -> OfficeKind.PPTX
            lower.endsWith(".odt") -> OfficeKind.ODT
            lower.endsWith(".ods") -> OfficeKind.ODS
            lower.endsWith(".odp") -> OfficeKind.ODP
            lower.endsWith(".html") || lower.endsWith(".htm") -> OfficeKind.HTML
            lower.endsWith(".epub") -> OfficeKind.EPUB
            else -> OfficeKind.UNKNOWN
        }
    }

    fun isOfficeDocument(name: String): Boolean = detectKind(name) != OfficeKind.UNKNOWN

    /**
     * Extracts readable content and structure from [file] as formatted Markdown text.
     */
    fun extractToMarkdown(file: File, name: String = file.name): String {
        val kind = detectKind(name)
        return when (kind) {
            OfficeKind.DOCX -> extractDocx(file)
            OfficeKind.XLSX -> extractXlsx(file)
            OfficeKind.PPTX -> extractPptx(file)
            OfficeKind.ODT -> extractOdt(file)
            OfficeKind.ODS -> extractOds(file)
            OfficeKind.ODP -> extractOdp(file)
            OfficeKind.HTML -> extractHtml(file.readText(Charsets.UTF_8))
            OfficeKind.EPUB -> extractEpub(file)
            OfficeKind.UNKNOWN -> file.readText(Charsets.UTF_8)
        }
    }

    /**
     * Extracts DOCX (Word) text and tables from word/document.xml.
     */
    fun extractDocx(file: File): String {
        return runCatching {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("word/document.xml") ?: return@use ""
                val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseDocxXml(xml)
            }
        }.getOrDefault("")
    }

    private fun parseDocxXml(xml: String): String {
        val sb = StringBuilder()
        // Extract paragraphs and tables
        val pRegex = Regex("<w:p\\b[^>]*>(.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
        val tRegex = Regex("<w:t\\b[^>]*>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
        val headingRegex = Regex("<w:pStyle\\s+w:val=\"(Heading[1-6]|Title)\"\\s*/>")

        val matches = pRegex.findAll(xml)
        for (m in matches) {
            val pContent = m.groupValues[1]
            val textPieces = tRegex.findAll(pContent).map { it.groupValues[1] }.toList()
            val text = textPieces.joinToString("").trim()
            if (text.isEmpty()) continue

            val headingMatch = headingRegex.find(pContent)
            if (headingMatch != null) {
                val hLevel = when (headingMatch.groupValues[1]) {
                    "Title", "Heading1" -> "# "
                    "Heading2" -> "## "
                    "Heading3" -> "### "
                    else -> "#### "
                }
                sb.append(hLevel).append(text).append("\n\n")
            } else {
                sb.append(text).append("\n\n")
            }
        }
        return sb.toString().trim()
    }

    /**
     * Extracts XLSX (Excel) sheets and cells into Markdown tables.
     */
    fun extractXlsx(file: File): String {
        return runCatching {
            ZipFile(file).use { zip ->
                val stringsEntry = zip.getEntry("xl/sharedStrings.xml")
                val sharedStrings = if (stringsEntry != null) {
                    val xml = zip.getInputStream(stringsEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val tRegex = Regex("<t\\b[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                    tRegex.findAll(xml).map { it.groupValues[1] }.toList()
                } else {
                    emptyList()
                }

                val sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml") ?: return@use ""
                val sheetXml = zip.getInputStream(sheetEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseXlsxSheet(sheetXml, sharedStrings)
            }
        }.getOrDefault("")
    }

    private fun parseXlsxSheet(xml: String, sharedStrings: List<String>): String {
        val sb = StringBuilder()
        sb.append("## Hoja de Cálculo\n\n")
        val rowRegex = Regex("<row\\b[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<c\\b([^>]*)>(?:<v>(.*?)</v>)?</c>", RegexOption.DOT_MATCHES_ALL)

        val rows = rowRegex.findAll(xml).map { rowMatch ->
            val rowContent = rowMatch.groupValues[1]
            cellRegex.findAll(rowContent).map { cellMatch ->
                val attrs = cellMatch.groupValues[1]
                val v = cellMatch.groupValues[2]
                if (attrs.contains("t=\"s\"")) {
                    val strIdx = v.toIntOrNull()
                    if (strIdx != null && strIdx in sharedStrings.indices) sharedStrings[strIdx] else v
                } else {
                    v
                }
            }.toList()
        }.filter { it.isNotEmpty() }.toList()

        if (rows.isEmpty()) return ""

        val maxCols = rows.maxOfOrNull { it.size } ?: 0
        if (maxCols == 0) return ""

        // Header Row
        val header = rows.first()
        sb.append("| ").append(header.joinToString(" | ")).append(" |\n")
        sb.append("| ").append((0 until maxCols).joinToString(" | ") { "---" }).append(" |\n")

        for (r in rows.drop(1)) {
            val padded = r + List(maxCols - r.size) { "" }
            sb.append("| ").append(padded.joinToString(" | ")).append(" |\n")
        }
        sb.append("\n")
        return sb.toString()
    }

    /**
     * Extracts PPTX (PowerPoint) presentation slides.
     */
    fun extractPptx(file: File): String {
        return runCatching {
            val sb = StringBuilder()
            ZipFile(file).use { zip ->
                val entries = zip.entries().asSequence()
                    .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
                    .sortedBy { it.name }
                    .toList()

                val tRegex = Regex("<a:t\\b[^>]*>(.*?)</a:t>", RegexOption.DOT_MATCHES_ALL)

                entries.forEachIndexed { index, entry ->
                    val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val texts = tRegex.findAll(xml).map { it.groupValues[1] }.filter { it.isNotBlank() }.toList()
                    if (texts.isNotEmpty()) {
                        sb.append("## Diapositiva ${index + 1}\n\n")
                        texts.forEach { t -> sb.append("- ").append(t.trim()).append("\n") }
                        sb.append("\n")
                    }
                }
            }
            sb.toString().trim()
        }.getOrDefault("")
    }

    /**
     * Extracts ODT (OpenDocument Text) from content.xml.
     */
    fun extractOdt(file: File): String {
        return runCatching {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("content.xml") ?: return@use ""
                val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseOdfXml(xml)
            }
        }.getOrDefault("")
    }

    /**
     * Extracts ODS (OpenDocument Spreadsheet).
     */
    fun extractOds(file: File): String = extractOdt(file)

    /**
     * Extracts ODP (OpenDocument Presentation).
     */
    fun extractOdp(file: File): String = extractOdt(file)

    private fun parseOdfXml(xml: String): String {
        val sb = StringBuilder()
        val pRegex = Regex("<text:p\\b[^>]*>(.*?)</text:p>", RegexOption.DOT_MATCHES_ALL)
        val hRegex = Regex("<text:h\\b[^>]*>(.*?)</text:h>", RegexOption.DOT_MATCHES_ALL)

        val hMatches = hRegex.findAll(xml)
        for (m in hMatches) {
            val text = m.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
            if (text.isNotEmpty()) sb.append("## ").append(text).append("\n\n")
        }

        val pMatches = pRegex.findAll(xml)
        for (m in pMatches) {
            val text = m.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
            if (text.isNotEmpty()) sb.append(text).append("\n\n")
        }
        return sb.toString().trim()
    }

    /**
     * Converts HTML content into structured Markdown.
     */
    fun extractHtml(html: String): String {
        var s = html
        s = s.replace(Regex("(?i)<h1\\b[^>]*>(.*?)</h1>"), "# $1\n\n")
        s = s.replace(Regex("(?i)<h2\\b[^>]*>(.*?)</h2>"), "## $1\n\n")
        s = s.replace(Regex("(?i)<h3\\b[^>]*>(.*?)</h3>"), "### $1\n\n")
        s = s.replace(Regex("(?i)<p\\b[^>]*>(.*?)</p>"), "$1\n\n")
        s = s.replace(Regex("(?i)<li\\b[^>]*>(.*?)</li>"), "- $1\n")
        s = s.replace(Regex("(?i)<br\\s*/?>"), "\n")
        s = s.replace(Regex("<[^>]+>"), "")
        s = s.replace("&nbsp;", " ")
        s = s.replace("&amp;", "&")
        s = s.replace("&lt;", "<")
        s = s.replace("&gt;", ">")
        return s.trim()
    }

    /**
     * Extracts EPUB ebook text chapters.
     */
    fun extractEpub(file: File): String {
        return runCatching {
            val sb = StringBuilder()
            ZipFile(file).use { zip ->
                val entries = zip.entries().asSequence()
                    .filter { it.name.endsWith(".xhtml") || it.name.endsWith(".html") }
                    .sortedBy { it.name }
                    .toList()

                for (entry in entries) {
                    val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val text = extractHtml(html)
                    if (text.isNotBlank()) {
                        sb.append(text).append("\n\n---\n\n")
                    }
                }
            }
            sb.toString().trim()
        }.getOrDefault("")
    }
}
