package com.nexopp.io

import com.nexopp.format.model.Background
import com.nexopp.format.model.Document
import com.nexopp.format.model.Layer
import com.nexopp.format.model.Page
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream

class ExportManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `parses page ranges accurately`() {
        val manager = ExportManager(
            context = FakeContext(tempFolder.root)
        )

        // "all" or blank
        assertEquals(listOf(0, 1, 2, 3, 4), manager.parsePageRange("all", 5))
        assertEquals(listOf(0, 1, 2, 3, 4), manager.parsePageRange("", 5))

        // "1-3, 5"
        assertEquals(listOf(0, 1, 2, 4), manager.parsePageRange("1-3, 5", 6))

        // "2, 4-5"
        assertEquals(listOf(1, 3, 4), manager.parsePageRange("2, 4-5", 6))

        // Out of bounds clamped
        assertEquals(listOf(0, 1, 2), manager.parsePageRange("1-10", 3))
    }

    @Test
    fun `exports multi-page PDF document to stream`() {
        val manager = ExportManager(
            context = FakeContext(tempFolder.root)
        )

        val doc = Document(
            pages = listOf(
                Page(width = 595.0, height = 842.0, background = Background.Solid(0xFFFFFFFF.toInt(), "ruled"), layers = listOf(Layer(emptyList()))),
                Page(width = 595.0, height = 842.0, background = Background.Solid(0xFFFFFFFF.toInt(), "graph"), layers = listOf(Layer(emptyList())))
            )
        )

        val out = ByteArrayOutputStream()
        manager.exportToStream(doc, out, ExportManager.ExportFormat.PDF, listOf(0, 1))

        val bytes = out.toByteArray()
        // PDF header should start with %PDF-
        assertEquals("%PDF-", String(bytes, 0, 5, Charsets.US_ASCII))
    }

    private class FakeContext(private val root: java.io.File) : android.content.ContextWrapper(null) {
        override fun getCacheDir(): java.io.File = root
        override fun getPackageName(): String = "com.nexopp"
    }
}
