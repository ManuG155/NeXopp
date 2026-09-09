package com.nexopp.recognition

import com.nexopp.format.model.Stroke
import com.nexopp.library.ContentSearchProvider
import com.nexopp.library.Notebook
import com.nexopp.library.SearchMatchKind
import com.nexopp.library.SearchResultItem
import java.util.concurrent.ConcurrentHashMap

/**
 * Indexes recognized stroke content and provides global handwriting search across notebooks.
 */
class StrokeSearchIndexer(
    private val recognitionEngine: RecognitionEngine = OfflineHeuristicRecognitionEngine()
) : ContentSearchProvider {

    // Cache of indexed text per notebook ID and page
    private val notebookIndexCache = ConcurrentHashMap<String, MutableMap<Int, String>>()

    /**
     * Index strokes for a given page of a notebook.
     */
    fun indexPageStrokes(notebookId: String, pageIndex: Int, strokes: List<Stroke>) {
        if (strokes.isEmpty()) return
        val recognized = recognitionEngine.recognize(strokes)
        if (recognized.text.isNotBlank()) {
            val pageMap = notebookIndexCache.getOrPut(notebookId) { ConcurrentHashMap() }
            pageMap[pageIndex] = recognized.text
        }
    }

    /**
     * Clear cached index for a notebook.
     */
    fun invalidateNotebook(notebookId: String) {
        notebookIndexCache.remove(notebookId)
    }

    override fun searchContent(query: String, notebook: Notebook): List<SearchResultItem> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return emptyList()

        val pageMap = notebookIndexCache[notebook.id] ?: return emptyList()
        val results = mutableListOf<SearchResultItem>()

        for ((pageIndex, text) in pageMap) {
            val lowerText = text.lowercase()
            if (lowerText.contains(cleanQuery)) {
                // Extract snippet around match
                val matchIdx = lowerText.indexOf(cleanQuery)
                val snippetStart = maxOf(0, matchIdx - 20)
                val snippetEnd = minOf(text.length, matchIdx + cleanQuery.length + 20)
                val snippet = text.substring(snippetStart, snippetEnd).trim()

                results.add(
                    SearchResultItem(
                        notebook = notebook,
                        subject = null,
                        matchedTags = emptyList(),
                        matchKind = SearchMatchKind.HANDWRITING_OCR,
                        matchSnippet = "Pág. ${pageIndex + 1}: \"...$snippet...\""
                    )
                )
            }
        }

        return results
    }
}
