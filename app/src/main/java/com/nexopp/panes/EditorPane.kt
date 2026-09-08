// --- EditorPane.kt ---
package com.nexopp.panes

import com.nexopp.format.SaveFormat
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.tabs.TabManager
import com.nexopp.tabs.TabStore

class EditorPane(
    val store: TabStore,
) {
    var surface: DrawingSurfaceView? = null
    val tabs = TabManager()
    var saveFormat: SaveFormat = SaveFormat.ORIGINAL
    var pendingSaveName: String = "document.xopp"

    fun persist() {
        val session = tabs.session()
        writer.execute { store.save(session) }
    }

    fun awaitPersist(timeoutMs: Long) {
        val done = java.util.concurrent.CountDownLatch(1)
        writer.execute(done::countDown)
        runCatching { done.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS) }
    }

    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor()
}