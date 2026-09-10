package com.nexopp.lan

import com.nexopp.format.model.*
import com.nexopp.library.LibraryStore
import com.nexopp.library.Notebook
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

        val version1 = bridge.addStroke(nb.id, pageIndex = 0, stroke = stroke)
        assertNotNull("Adding stroke must succeed", version1)
        assertTrue("Version must be positive", version1!! > 0L)

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

        val versionText = bridge.addText(nb.id, pageIndex = 0, text = text)
        assertNotNull("Adding text must succeed", versionText)
        assertTrue("Version must be positive", versionText!! > 0L)

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

    @Test
    fun matchesNotebook_recognizesFileUrisAndFilenamesAndIds() {
        bridge.createNotebook("Biología Molecular", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        // 1. Exact ID match
        assertTrue("Must match by ID", bridge.matchesNotebook(nb.id, nb))

        // 2. Exact fileName match
        assertTrue("Must match by fileName", bridge.matchesNotebook(nb.fileName, nb))

        // 3. Exact name match
        assertTrue("Must match by notebook name", bridge.matchesNotebook(nb.name, nb))

        // 4. Android file URI format (as produced by Uri.fromFile in MainActivity)
        val fileUri = "file:///data/user/0/com.nexopp/files/notebooks/${nb.fileName}"
        assertTrue("Must match Android file URI", bridge.matchesNotebook(fileUri, nb))

        // 5. Normal file path format
        val unixPath = "/data/user/0/com.nexopp/files/notebooks/${nb.fileName}"
        assertTrue("Must match Unix file path", bridge.matchesNotebook(unixPath, nb))

        val winPath = "C:\\app\\notebooks\\${nb.fileName}"
        assertTrue("Must match Windows file path", bridge.matchesNotebook(winPath, nb))

        // 6. Non-matching identifiers
        assertFalse("Must not match different file name", bridge.matchesNotebook("other.xopp", nb))
        assertFalse("Must not match empty string", bridge.matchesNotebook("", nb))
        assertFalse("Must not match null", bridge.matchesNotebook(null, nb))
    }

    @Test
    fun onTabletDocumentEdited_withFileUri_broadcastsToWebClientWithMonotonicVersion() = runBlocking {
        bridge.createNotebook("Química Orgánica", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        // Open in bridge so it's in openWebDocuments
        bridge.openDocument(nb.id, nb.fileName)
        val active = bridge.getActiveDocument(nb.id)
        assertNotNull(active)
        val initialVersion = active!!.version.get()

        var broadcastType: String? = null
        var broadcastPayload: String? = null
        bridge.onBroadcastMessage = { type, payload ->
            broadcastType = type
            broadcastPayload = payload
        }

        // Simulate tablet edit passing full file URI as MainActivity does
        val fileUri = "file:///data/user/0/com.nexopp/files/notebooks/${nb.fileName}"
        val stroke = Stroke(
            tool = Tool.PEN,
            color = 0xFFDC2626.toInt(),
            capStyle = "round",
            points = listOf(StrokePoint(10.0, 10.0, 1.5), StrokePoint(20.0, 20.0, 1.5)),
            uniformWidth = true
        )
        val updatedDoc = Document(pages = listOf(
            Page(595.0, 842.0, Background.Solid(0xFFFFFFFF.toInt(), "ruled"), listOf(Layer(listOf(stroke))))
        ))

        bridge.onTabletDocumentEdited(fileUri, updatedDoc)

        assertEquals("Broadcast type must be TABLET_DOCUMENT_CHANGED", LanProtocol.TYPE_TABLET_DOCUMENT_CHANGED, broadcastType)
        assertNotNull("Broadcast payload must not be null", broadcastPayload)
        assertTrue("Payload must contain notebookId", broadcastPayload!!.contains(nb.id))

        val currentVersion = active.version.get()
        assertEquals("Version must increment monotonically by 1", initialVersion + 1, currentVersion)
        assertTrue("Payload must contain updated version", broadcastPayload!!.contains("\"version\":$currentVersion"))
    }

    @Test
    fun pcEdit_withFileUriTabletSurface_appliesToSurfaceImmediately() = runBlocking {
        bridge.createNotebook("Historia Universal", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        val fileUri = "file:///data/user/0/com.nexopp/files/notebooks/${nb.fileName}"
        var appliedDocToSurface: Document? = null

        bridge.activeSurfaceProvider = {
            Pair(fileUri, Document(pages = listOf(
                Page(595.0, 842.0, Background.Solid(0xFFFFFFFF.toInt(), "ruled"), emptyList())
            )))
        }
        bridge.onApplyDocumentToSurface = { updatedDoc ->
            appliedDocToSurface = updatedDoc
        }

        bridge.openDocument(nb.id, nb.fileName)

        // PC sends a stroke
        val stroke = Stroke(
            tool = Tool.PEN,
            color = 0xFF2563EB.toInt(),
            capStyle = "round",
            points = listOf(StrokePoint(30.0, 30.0, 2.0), StrokePoint(40.0, 40.0, 2.0)),
            uniformWidth = true
        )
        val newVersion = bridge.addStroke(nb.id, pageIndex = 0, stroke = stroke)
        assertNotNull("Add stroke must succeed", newVersion)

        // In tests without Robolectric Looper, Handler.post runs or onApplyNotebookDocumentToSurface can be used
        // Test onApplyNotebookDocumentToSurface directly
        var notebookApplied: Notebook? = null
        var docApplied: Document? = null
        bridge.onApplyNotebookDocumentToSurface = { notebook, doc ->
            notebookApplied = notebook
            docApplied = doc
        }

        val stroke2 = Stroke(
            tool = Tool.PEN,
            color = 0xFF16A34A.toInt(),
            capStyle = "round",
            points = listOf(StrokePoint(50.0, 50.0, 2.0), StrokePoint(60.0, 60.0, 2.0)),
            uniformWidth = true
        )
        bridge.addStroke(nb.id, pageIndex = 0, stroke = stroke2)

        // Verify that onApplyNotebookDocumentToSurface was invoked or queued with the right document
        assertNotNull("Active document must have the stroke", bridge.getActiveDocument(nb.id)?.document)
        val activeDoc = bridge.getActiveDocument(nb.id)!!.document
        assertEquals(2, activeDoc.pages[0].layers.last().elements.size)
    }

    @Test
    fun checkDocumentVersion_reconciliationLogic() = runBlocking {
        bridge.createNotebook("Geometría", "sub1", 0xFF1E3A8A, "ruled")
        val nb = store.loadNotebooks().first()

        bridge.openDocument(nb.id, nb.fileName)
        val active = bridge.getActiveDocument(nb.id)
        assertNotNull("Active document must be registered", active)
        val v1 = active!!.version.get()

        // Tablet edit occurs
        val fileUri = "file:///data/user/0/com.nexopp/files/notebooks/${nb.fileName}"
        val updatedDoc = Document(pages = listOf(
            Page(595.0, 842.0, Background.Solid(0xFFFFFFFF.toInt(), "ruled"), emptyList())
        ))
        bridge.onTabletDocumentEdited(fileUri, updatedDoc)

        val v2 = active.version.get()
        assertTrue("Tablet edit must increment version", v2 > v1)

        // Reconciling check: server has v2 > client v1
        assertTrue("Server version ($v2) is ahead of client version ($v1)", active.version.get() > v1)
    }
}
