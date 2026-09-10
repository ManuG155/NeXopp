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

    @Test
    fun createRenameDeleteNotebook_incrementsLibraryStoreVersionAndPersists() {
        val initialVersion = store.version.value

        // 1. Create notebook
        bridge.createNotebook("Notebook Version Test", "sub1", 0xFF1E3A8A, "ruled")
        val v1 = store.version.value
        assertTrue("Store version must increment on create", v1 > initialVersion)
        val loaded1 = store.loadNotebooks().firstOrNull { it.name == "Notebook Version Test" }
        assertNotNull("Notebook must persist in store", loaded1)

        // 2. Rename notebook
        bridge.renameNotebook(loaded1!!.id, "Notebook Renamed")
        val v2 = store.version.value
        assertTrue("Store version must increment on rename", v2 > v1)
        val loaded2 = store.loadNotebooks().firstOrNull { it.id == loaded1.id }
        assertNotNull(loaded2)
        assertEquals("Notebook Renamed", loaded2!!.name)

        // 3. Delete notebook
        bridge.deleteNotebook(loaded1.id)
        val v3 = store.version.value
        assertTrue("Store version must increment on delete", v3 > v2)
        val loaded3 = store.loadNotebooks().firstOrNull { it.id == loaded1.id }
        assertNull("Notebook must be removed from store", loaded3)
    }

    @Test
    fun eraseStrokes_removesTargetStrokeAndPersistsWithoutIt() = runBlocking {
        bridge.createNotebook("Test Eraser", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        bridge.openDocument(nb.id, nb.fileName)

        val stroke = Stroke(
            tool = Tool.PEN,
            color = 0xFF000000.toInt(),
            capStyle = "round",
            points = listOf(
                StrokePoint(100.0, 100.0, 2.0),
                StrokePoint(120.0, 120.0, 2.0)
            ),
            uniformWidth = true
        )
        bridge.addStroke(nb.id, pageIndex = 0, stroke = stroke)

        // Verify stroke exists before erase
        val beforeDoc = repo.loadDocument(nb.fileName).getOrNull()
        assertNotNull(beforeDoc)
        assertEquals(1, beforeDoc!!.pages[0].layers[0].elements.size)

        // Erase at (110.0, 110.0) with radius 5.0
        val erasePoints = listOf(LanProtocol.PointPayload(110.0, 110.0, 5.0))
        val result = bridge.eraseStrokes(nb.id, pageIndex = 0, points = erasePoints)

        assertNotNull("Erase result must not be null", result)
        assertTrue("Erase result must indicate change", result!!.changed)
        assertEquals("Target page must have 0 elements after erase", 0, result.page.layers[0].elements.size)

        // Verify disk persistence: the .xopp must no longer contain the stroke
        val afterDoc = repo.loadDocument(nb.fileName).getOrNull()
        assertNotNull(afterDoc)
        assertEquals(1, afterDoc!!.pages.size)
        assertEquals("Persisted .xopp must have 0 elements", 0, afterDoc.pages[0].layers[0].elements.size)
    }

    @Test
    fun eraseStrokes_missDoesNotModifyDocument() = runBlocking {
        bridge.createNotebook("Test Eraser Miss", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        bridge.openDocument(nb.id, nb.fileName)

        val stroke = Stroke(
            tool = Tool.PEN,
            color = 0xFF000000.toInt(),
            capStyle = "round",
            points = listOf(
                StrokePoint(50.0, 50.0, 2.0),
                StrokePoint(60.0, 60.0, 2.0)
            ),
            uniformWidth = true
        )
        bridge.addStroke(nb.id, pageIndex = 0, stroke = stroke)

        // Erase point far away (500.0, 500.0)
        val erasePoints = listOf(LanProtocol.PointPayload(500.0, 500.0, 5.0))
        val result = bridge.eraseStrokes(nb.id, pageIndex = 0, points = erasePoints)

        assertNotNull(result)
        assertFalse("Erase miss must not report change", result!!.changed)
        assertEquals("Page must still have the stroke", 1, result.page.layers[0].elements.size)

        val diskDoc = repo.loadDocument(nb.fileName).getOrNull()
        assertNotNull(diskDoc)
        assertEquals("Persisted doc must still contain the stroke", 1, diskDoc!!.pages[0].layers[0].elements.size)
    }
}
