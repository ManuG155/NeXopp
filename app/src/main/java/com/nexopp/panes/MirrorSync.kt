// --- MirrorSync.kt ---
package com.nexopp.panes

import android.os.Handler
import android.os.Looper
import com.nexopp.format.model.Document
import com.nexopp.tabs.OpenTab

class MirrorSync(
    private val panes: List<EditorPane>,
    private val post: (Runnable) -> Unit = { Handler(Looper.getMainLooper()).post(it) },
) {
    private var pendingSource: EditorPane? = null
    private var pendingDoc: Document? = null
    private var scheduled = false

    fun propagate(source: EditorPane, doc: Document) {
        val active = source.tabs.active ?: return
        val key = active.docKey
        if (panes.none { p -> p.tabs.tabs.any { it.docKey == key && it.id != active.id } }) return
        pendingSource = source
        pendingDoc = doc
        if (scheduled) return
        scheduled = true
        post { flush() }
    }

    fun flush() {
        scheduled = false
        val source = pendingSource
        val doc = pendingDoc
        pendingSource = null
        pendingDoc = null
        if (source == null || doc == null) return
        val key = source.tabs.active?.docKey ?: return
        for (p in panes) {
            p.tabs.updateMatching({ it.docKey == key }) { it.copy(document = doc, hydrated = true) }
            if (p === source) continue
            if (p.tabs.active?.docKey == key) p.surface?.applyMirroredDocument(doc)
        }
    }
}