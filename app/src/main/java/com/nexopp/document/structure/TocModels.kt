package com.nexopp.document.structure

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * An individual entry in the notebook's Table of Contents (Índice).
 */
data class TocEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val pageIndex: Int,
    val order: Int = 0,
    val level: Int = 0 // 0 = Chapter / Main, 1 = Section, 2 = Subsection
)

/**
 * A named bookmark marking a specific page with optional notes.
 */
data class NamedBookmark(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val pageIndex: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * An interactive link between notebook sections/pages or external references.
 */
data class InternalLink(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val targetPageIndex: Int,
    val sourcePageIndex: Int = 0,
    val x: Double = 80.0,
    val y: Double = 120.0,
    val width: Double = 160.0,
    val height: Double = 36.0,
    val color: Int = 0xFF1976D2.toInt(),
    val isExternalUrl: Boolean = false,
    val externalUrl: String? = null
)

/**
 * Complete document structural metadata container.
 */
data class DocumentStructure(
    val tocEntries: List<TocEntry> = emptyList(),
    val namedBookmarks: List<NamedBookmark> = emptyList(),
    val internalLinks: List<InternalLink> = emptyList()
)

/**
 * Manages persistent JSON storage of document structure sidecars without modifying .xopp core XML.
 */
class DocumentStructureStore(private val storageDir: File) {

    fun structureFileFor(notebookFileName: String): File {
        val baseName = notebookFileName.substringBeforeLast(".")
        return File(storageDir, "$baseName.structure.json")
    }

    fun load(notebookFileName: String): DocumentStructure {
        val file = structureFileFor(notebookFileName)
        if (!file.exists() || !file.canRead()) return DocumentStructure()

        return try {
            val json = JSONObject(file.readText())
            val tocList = mutableListOf<TocEntry>()
            val tocArr = json.optJSONArray("toc") ?: JSONArray()
            for (i in 0 until tocArr.length()) {
                val obj = tocArr.getJSONObject(i)
                tocList.add(
                    TocEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        pageIndex = obj.getInt("pageIndex"),
                        order = obj.optInt("order", i),
                        level = obj.optInt("level", 0)
                    )
                )
            }

            val bookmarkList = mutableListOf<NamedBookmark>()
            val bmArr = json.optJSONArray("bookmarks") ?: JSONArray()
            for (i in 0 until bmArr.length()) {
                val obj = bmArr.getJSONObject(i)
                bookmarkList.add(
                    NamedBookmark(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        pageIndex = obj.getInt("pageIndex"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            val linkList = mutableListOf<InternalLink>()
            val linkArr = json.optJSONArray("links") ?: JSONArray()
            for (i in 0 until linkArr.length()) {
                val obj = linkArr.getJSONObject(i)
                linkList.add(
                    InternalLink(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        label = obj.getString("label"),
                        targetPageIndex = obj.getInt("targetPageIndex"),
                        sourcePageIndex = obj.optInt("sourcePageIndex", 0),
                        x = obj.optDouble("x", 80.0),
                        y = obj.optDouble("y", 120.0),
                        width = obj.optDouble("width", 160.0),
                        height = obj.optDouble("height", 36.0),
                        color = obj.optInt("color", 0xFF1976D2.toInt()),
                        isExternalUrl = obj.optBoolean("isExternalUrl", false),
                        externalUrl = obj.optString("externalUrl").takeIf { it.isNotBlank() }
                    )
                )
            }

            DocumentStructure(
                tocEntries = tocList.sortedBy { it.order },
                namedBookmarks = bookmarkList.sortedBy { it.pageIndex },
                internalLinks = linkList
            )
        } catch (_: Exception) {
            DocumentStructure()
        }
    }

    fun save(notebookFileName: String, structure: DocumentStructure) {
        val file = structureFileFor(notebookFileName)
        try {
            val json = JSONObject()

            val tocArr = JSONArray()
            structure.tocEntries.forEach { entry ->
                tocArr.put(JSONObject().apply {
                    put("id", entry.id)
                    put("title", entry.title)
                    put("pageIndex", entry.pageIndex)
                    put("order", entry.order)
                    put("level", entry.level)
                })
            }
            json.put("toc", tocArr)

            val bmArr = JSONArray()
            structure.namedBookmarks.forEach { bm ->
                bmArr.put(JSONObject().apply {
                    put("id", bm.id)
                    put("title", bm.title)
                    put("pageIndex", bm.pageIndex)
                    put("timestamp", bm.timestamp)
                })
            }
            json.put("bookmarks", bmArr)

            val linkArr = JSONArray()
            structure.internalLinks.forEach { link ->
                linkArr.put(JSONObject().apply {
                    put("id", link.id)
                    put("label", link.label)
                    put("targetPageIndex", link.targetPageIndex)
                    put("sourcePageIndex", link.sourcePageIndex)
                    put("x", link.x)
                    put("y", link.y)
                    put("width", link.width)
                    put("height", link.height)
                    put("color", link.color)
                    put("isExternalUrl", link.isExternalUrl)
                    put("externalUrl", link.externalUrl ?: "")
                })
            }
            json.put("links", linkArr)

            file.writeText(json.toString(2))
        } catch (_: Exception) {}
    }

    fun loadStructure(notebookFileName: String): DocumentStructure = load(notebookFileName)
    fun saveStructure(notebookFileName: String, structure: DocumentStructure) = save(notebookFileName, structure)
}
