package com.nexopp.lan

import com.nexopp.format.model.*
import com.nexopp.library.LibraryStore
import com.nexopp.repository.LocalDocumentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LanSyncBridgeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: LibraryStore
    private lateinit var repo: LocalDocumentRepository
    private lateinit var bridge: LanSyncBridge

    @Before
    fun setup() {
        val baseDir = tempFolder.newFolder("lan_sync_test")
        store = LibraryStore(baseDir)
        repo = LocalDocumentRepository(store.notebooksDir, store)
        bridge = LanSyncBridge(
            libraryStore = store,
            documentRepository = repo
        )
    }

    @Test
    fun getLibraryJson_returnsEmptyInitialState() {
        val json = bridge.getLibraryJson()
        assertEquals(LanProtocol.TYPE_LIBRARY_DATA, json.getString("type"))
        assertEquals(0, json.getJSONArray("subjects").length())
        assertEquals(0, json.getJSONArray("notebooks").length())
    }

    @Test
    fun createSubjectAndNotebook_persistsInLibrary() {
        val subJson = bridge.createSubject("Física Cuántica", 0xFF059669, null)
        val subs = subJson.getJSONArray("subjects")
        assertEquals(1, subs.length())
        val subId = subs.getJSONObject(0).getString("id")
        assertEquals("Física Cuántica", subs.getJSONObject(0).getString("name"))

        val nbJson = bridge.createNotebook("Mecánica", subId, 0xFF1E3A8A, "graph")
        val nbs = nbJson.getJSONArray("notebooks")
        assertEquals(1, nbs.length())
        val nbObj = nbs.getJSONObject(0)
        assertEquals("Mecánica", nbObj.getString("name"))
        assertEquals(subId, nbObj.getString("subjectId"))
        assertEquals("graph", nbObj.getString("initialTemplate"))
    }

    @Test
    fun openDocumentAndAddStroke_syncsAndPersists() = runBlocking {
        // Create notebook
        bridge.createNotebook("Cálculo I", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        // Open document
        val docJson = bridge.openDocument(nb.id, nb.fileName)
        assertNotNull(docJson)
        assertEquals("Cálculo I", docJson!!.getString("title"))
        assertEquals(1, docJson.getJSONArray("pages").length())

        // Add a stroke from PC
        val stroke = Stroke(
            tool = Tool.PEN,
            color = 0xFF2563EB.toInt(),
            capStyle = "round",
            points = listOf(
                StrokePoint(50.0, 50.0, 2.0),
                StrokePoint(100.0, 100.0, 2.0)
            ),
            uniformWidth = true
        )

        val success = bridge.addStroke(nb.id, pageIndex = 0, stroke = stroke)
        assertTrue("Adding stroke must succeed", success)

        // Verify that the stroke was saved to disk by reloading the .xopp
        val reloadedDoc = repo.loadDocument(nb.fileName).getOrNull()
        assertNotNull(reloadedDoc)
        assertEquals(1, reloadedDoc!!.pages.size)
        val layer = reloadedDoc.pages[0].layers.firstOrNull()
        assertNotNull(layer)
        assertEquals(1, layer!!.elements.size)
        val persistedStroke = layer.elements[0] as Stroke
        assertEquals(Tool.PEN, persistedStroke.tool)
        assertEquals(0xFF2563EB.toInt(), persistedStroke.color)
        assertEquals(2, persistedStroke.points.size)
    }

    @Test
    fun addPage_appendsPageAndUpdatesCount() = runBlocking {
        bridge.createNotebook("Notas", "sub1", 0xFF1E3A8A, "plain")
        val nb = store.loadNotebooks().first()

        bridge.openDocument(nb.id, nb.fileName)
        val updatedDocJson = bridge.addPage(nb.id, 595.276, 841.890, "grid")
        assertNotNull(updatedDocJson)

        val pages = updatedDocJson!!.getJSONArray("pages")
        assertEquals(2, pages.length())

        // Verify notebook metadata updated in LibraryStore
        val updatedNb = store.loadNotebooks().first { it.id == nb.id }
        assertEquals(2, updatedNb.pageCount)
    }

    @Test
    fun onTabletDocumentEdited_broadcastsEvent() {
        bridge.createNotebook("Notas Tablet", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        var broadcastType: String? = null
        var broadcastPayload: String? = null
        bridge.onBroadcastMessage = { type, payload ->
            broadcastType = type
            broadcastPayload = payload
        }

        // Simulate tablet drawing
        val updatedDoc = Document(pages = listOf(
            Page(595.0, 842.0, Background.Solid(0xFFFFFFFF.toInt(), "ruled"), emptyList())
        ))

        runBlocking {
            bridge.openDocument(nb.id, nb.fileName)
        }

        bridge.onTabletDocumentEdited(nb.fileName, updatedDoc)

        assertEquals(LanProtocol.TYPE_TABLET_DOCUMENT_CHANGED, broadcastType)
        assertNotNull(broadcastPayload)
        assertTrue(broadcastPayload!!.contains(nb.id))
    }

    @Test
    fun openDocumentAndAddText_syncsAndPersists() = runBlocking {
        // Create notebook
        bridge.createNotebook("Notas de Álgebra", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        bridge.openDocument(nb.id, nb.fileName)

        val text = TextElement(
            font = "Liberation Sans",
            size = 14.0,
            x = 100.0,
            y = 150.0,
            color = 0xFF1E293B.toInt(),
            content = "Texto escrito con teclado PC"
        )

        val success = bridge.addText(nb.id, pageIndex = 0, text = text)
        assertTrue("Adding text must succeed", success)

        // Verify that the text was saved to disk by reloading the .xopp
        val reloadedDoc = repo.loadDocument(nb.fileName).getOrNull()
        assertNotNull(reloadedDoc)
        assertEquals(1, reloadedDoc!!.pages.size)
        val layer = reloadedDoc.pages[0].layers.firstOrNull()
        assertNotNull(layer)
        assertEquals(1, layer!!.elements.size)
        val persistedText = layer.elements[0] as TextElement
        assertEquals("Liberation Sans", persistedText.font)
        assertEquals(14.0, persistedText.size, 0.001)
        assertEquals(100.0, persistedText.x, 0.001)
        assertEquals(150.0, persistedText.y, 0.001)
        assertEquals(0xFF1E293B.toInt(), persistedText.color)
        assertEquals("Texto escrito con teclado PC", persistedText.content)
    }
}
