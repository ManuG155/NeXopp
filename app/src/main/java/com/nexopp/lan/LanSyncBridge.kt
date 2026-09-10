package com.nexopp.lan

import android.os.Handler
import android.os.Looper
import com.nexopp.format.model.*
import com.nexopp.library.LibraryStore
import com.nexopp.library.Notebook
import com.nexopp.library.Subject
import com.nexopp.render.DrawingSurfaceDefaults
import com.nexopp.render.EraserMode
import com.nexopp.render.PageEraser
import com.nexopp.repository.LocalDocumentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class ActiveWebDocument(
    val notebook: Notebook,
    var document: Document,
    val version: AtomicLong = AtomicLong(1L)
)

data class EraseResult(
    val changed: Boolean,
    val version: Long,
    val page: Page
)

/**
 * Bridges LanServer WebSocket communication directly with NeXopp's real document repositories
 * and active editing surfaces. No parallel document databases or duplicate libraries.
 */
class LanSyncBridge(
    val libraryStore: LibraryStore,
    val documentRepository: LocalDocumentRepository,
    var activeSurfaceProvider: (() -> Pair<String?, Document?>)? = null,
    var onApplyDocumentToSurface: ((Document) -> Unit)? = null
) {
    var activeDocumentFinder: ((Notebook) -> Document?)? = null
    var onApplyNotebookDocumentToSurface: ((Notebook, Document) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val openWebDocuments = ConcurrentHashMap<String, ActiveWebDocument>()

    // Listener invoked when the bridge wants to broadcast a message to connected PC clients
    var onBroadcastMessage: ((String, String) -> Unit)? = null

    fun matchesNotebook(identifier: String?, notebook: Notebook): Boolean {
        if (identifier.isNullOrBlank()) return false
        if (identifier == notebook.id || identifier == notebook.fileName || identifier == notebook.name) return true
        if (identifier.endsWith("/${notebook.fileName}") || identifier.endsWith("\\${notebook.fileName}")) return true
        if (identifier.contains(notebook.fileName) || identifier.contains(notebook.id)) return true
        val stripped = identifier.substringAfterLast('/').substringAfterLast('\\')
        return stripped == notebook.fileName || stripped == notebook.id || stripped == notebook.name
    }

    private fun applyToTabletSurface(notebook: Notebook, updatedDoc: Document) {
        if (onApplyNotebookDocumentToSurface != null) {
            mainHandler.post {
                onApplyNotebookDocumentToSurface?.invoke(notebook, updatedDoc)
            }
        } else if (onApplyDocumentToSurface != null) {
            val activeTabletInfo = activeSurfaceProvider?.invoke()
            if (activeTabletInfo != null && matchesNotebook(activeTabletInfo.first, notebook)) {
                mainHandler.post {
                    onApplyDocumentToSurface?.invoke(updatedDoc)
                }
            }
        }
    }

    init {
        scope.launch {
            libraryStore.version.collect { ver ->
                if (ver > 0L) {
                    val libJson = getLibraryJson()
                    onBroadcastMessage?.invoke(LanProtocol.TYPE_LIBRARY_DATA, libJson.toString())
                }
            }
        }
    }

    // --- Library Operations ---

    fun getLibraryJson(): JSONObject {
        val subjects = libraryStore.loadSubjects()
        val notebooks = libraryStore.loadNotebooks()
        val tags = libraryStore.loadTags()
        return LanProtocol.libraryToJson(subjects, notebooks, tags)
    }

    fun createSubject(name: String, color: Long, parentId: String?): JSONObject {
        val subject = Subject(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            color = color,
            parentId = parentId
        )
        libraryStore.addSubject(subject)
        return getLibraryJson()
    }

    fun createNotebook(name: String, subjectId: String, coverColor: Long, template: String): JSONObject {
        val fileName = "${UUID.randomUUID()}.xopp"
        val notebook = Notebook(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            name = name.trim().ifEmpty { "Sin título" },
            fileName = fileName,
            coverColor = coverColor,
            initialTemplate = template,
            lastModified = System.currentTimeMillis()
        )
        libraryStore.addNotebook(notebook)

        // Create initial .xopp file with a single page
        val initialDoc = Document(
            pages = listOf(
                Page(
                    width = DrawingSurfaceDefaults.A4_WIDTH_PT.toDouble(),
                    height = DrawingSurfaceDefaults.A4_HEIGHT_PT.toDouble(),
                    background = Background.Solid(0xFFFFFFFF.toInt(), template),
                    layers = listOf(Layer(emptyList()))
                )
            )
        )
        openWebDocuments[notebook.id] = ActiveWebDocument(notebook, initialDoc)

        // Save initial document synchronously to ensure .xopp exists and avoid background race conditions
        runBlocking(Dispatchers.IO) {
            documentRepository.saveDocument(fileName, initialDoc)
        }

        return getLibraryJson()
    }

    fun renameNotebook(notebookId: String, newName: String): JSONObject {
        val notebooks = libraryStore.loadNotebooks()
        val nb = notebooks.find { it.id == notebookId }
        if (nb != null && newName.isNotBlank()) {
            val updated = nb.copy(name = newName.trim(), lastModified = System.currentTimeMillis())
            libraryStore.updateNotebook(updated)
            val openDoc = openWebDocuments[notebookId]
            if (openDoc != null) {
                openWebDocuments[notebookId] = openDoc.copy(notebook = updated)
            }
        }
        return getLibraryJson()
    }

    fun deleteNotebook(notebookId: String): JSONObject {
        val notebooks = libraryStore.loadNotebooks()
        val nb = notebooks.find { it.id == notebookId }
        if (nb != null) {
            libraryStore.deleteNotebook(nb)
            openWebDocuments.remove(notebookId)
        }
        return getLibraryJson()
    }

    // --- Document Operations ---

    suspend fun openDocument(notebookId: String, requestedFileName: String?): JSONObject? = withContext(Dispatchers.IO) {
        val notebook = libraryStore.loadNotebooks().find {
            it.id == notebookId || (requestedFileName != null && it.fileName == requestedFileName)
        } ?: return@withContext null

        // 1. Check if the tablet has this document currently open in any active pane
        val activeTabletDoc = activeDocumentFinder?.invoke(notebook)
            ?: run {
                val surfaceInfo = activeSurfaceProvider?.invoke()
                if (surfaceInfo != null && matchesNotebook(surfaceInfo.first, notebook)) {
                    surfaceInfo.second
                } else null
            }

        val doc: Document = if (activeTabletDoc != null) {
            activeTabletDoc
        } else {
            // Load from disk via LocalDocumentRepository
            val loadResult = documentRepository.loadDocument(notebook.fileName)
            if (loadResult.isSuccess) {
                loadResult.getOrThrow()
            } else {
                // If not found on disk, create initial page
                Document(
                    pages = listOf(
                        Page(
                            width = DrawingSurfaceDefaults.A4_WIDTH_PT.toDouble(),
                            height = DrawingSurfaceDefaults.A4_HEIGHT_PT.toDouble(),
                            background = Background.Solid(0xFFFFFFFF.toInt(), notebook.initialTemplate),
                            layers = listOf(Layer(emptyList()))
                        )
                    )
                )
            }
        }

        val active = ActiveWebDocument(notebook, doc)
        openWebDocuments[notebook.id] = active

        LanProtocol.documentToJson(
            notebookId = notebook.id,
            fileName = notebook.fileName,
            title = notebook.name,
            document = doc,
            version = active.version.get()
        )
    }

    suspend fun addStroke(notebookId: String, pageIndex: Int, stroke: Stroke): Long? = withContext(Dispatchers.IO) {
        var active = openWebDocuments[notebookId]
        if (active == null) {
            // Try opening it
            openDocument(notebookId, null)
            active = openWebDocuments[notebookId] ?: return@withContext null
        }

        val doc = active.document
        val pages = doc.pages.toMutableList()
        if (pageIndex !in pages.indices) {
            // Add blank page if index out of bounds
            pages.add(
                Page(
                    width = DrawingSurfaceDefaults.A4_WIDTH_PT.toDouble(),
                    height = DrawingSurfaceDefaults.A4_HEIGHT_PT.toDouble(),
                    background = Background.Solid(0xFFFFFFFF.toInt(), active.notebook.initialTemplate),
                    layers = listOf(Layer(listOf(stroke)))
                )
            )
        } else {
            val targetPage = pages[pageIndex]
            val layers = targetPage.layers.toMutableList()
            if (layers.isEmpty()) {
                layers.add(Layer(listOf(stroke)))
            } else {
                val topLayer = layers.last()
                val elements = topLayer.elements.toMutableList()
                elements.add(stroke)
                layers[layers.size - 1] = topLayer.copy(elements = elements)
            }
            pages[pageIndex] = targetPage.copy(layers = layers)
        }

        val updatedDoc = doc.copy(pages = pages)
        active.document = updatedDoc
        val newVersion = active.version.incrementAndGet()

        // 1. If currently open on tablet surface, reflect changes on tablet screen immediately
        applyToTabletSurface(active.notebook, updatedDoc)

        // 2. Persist to .xopp file via repository
        documentRepository.saveDocument(active.notebook.fileName, updatedDoc)

        // 3. Update notebook metadata (page count and last modified)
        libraryStore.updateNotebook(
            active.notebook.copy(
                pageCount = updatedDoc.pages.size,
                lastModified = System.currentTimeMillis()
            )
        )

        newVersion
    }

    suspend fun eraseStrokes(
        notebookId: String,
        pageIndex: Int,
        points: List<LanProtocol.PointPayload>
    ): EraseResult? = withContext(Dispatchers.IO) {
        var active = openWebDocuments[notebookId]
        if (active == null) {
            openDocument(notebookId, null)
            active = openWebDocuments[notebookId] ?: return@withContext null
        }

        val doc = active.document
        val pages = doc.pages.toMutableList()
        if (pageIndex !in pages.indices) return@withContext null

        var currentPage = pages[pageIndex]
        var docChanged = false

        for (pt in points) {
            val erased = PageEraser.erase(
                page = currentPage,
                px = pt.x,
                py = pt.y,
                radius = pt.radius,
                mode = EraserMode.WHOLE_STROKE
            )
            if (erased != null) {
                currentPage = erased
                docChanged = true
            }
        }

        var newVersion = active.version.get()
        if (docChanged) {
            pages[pageIndex] = currentPage
            val updatedDoc = doc.copy(pages = pages)
            active.document = updatedDoc
            newVersion = active.version.incrementAndGet()

            // 1. If currently open on tablet surface, reflect changes on tablet screen immediately
            applyToTabletSurface(active.notebook, updatedDoc)

            // 2. Persist to .xopp file via repository
            documentRepository.saveDocument(active.notebook.fileName, updatedDoc)

            // 3. Update notebook metadata
            libraryStore.updateNotebook(
                active.notebook.copy(
                    lastModified = System.currentTimeMillis()
                )
            )
        }

        EraseResult(
            changed = docChanged,
            version = newVersion,
            page = currentPage
        )
    }

    suspend fun addText(notebookId: String, pageIndex: Int, text: TextElement): Long? = withContext(Dispatchers.IO) {
        var active = openWebDocuments[notebookId]
        if (active == null) {
            openDocument(notebookId, null)
            active = openWebDocuments[notebookId] ?: return@withContext null
        }

        val doc = active.document
        val pages = doc.pages.toMutableList()
        if (pageIndex !in pages.indices) {
            pages.add(
                Page(
                    width = DrawingSurfaceDefaults.A4_WIDTH_PT.toDouble(),
                    height = DrawingSurfaceDefaults.A4_HEIGHT_PT.toDouble(),
                    background = Background.Solid(0xFFFFFFFF.toInt(), active.notebook.initialTemplate),
                    layers = listOf(Layer(listOf(text)))
                )
            )
        } else {
            val targetPage = pages[pageIndex]
            val layers = targetPage.layers.toMutableList()
            if (layers.isEmpty()) {
                layers.add(Layer(listOf(text)))
            } else {
                val topLayer = layers.last()
                val elements = topLayer.elements.toMutableList()
                elements.add(text)
                layers[layers.size - 1] = topLayer.copy(elements = elements)
            }
            pages[pageIndex] = targetPage.copy(layers = layers)
        }

        val updatedDoc = doc.copy(pages = pages)
        active.document = updatedDoc
        val newVersion = active.version.incrementAndGet()

        // 1. If currently open on tablet surface, reflect changes on tablet screen immediately
        applyToTabletSurface(active.notebook, updatedDoc)

        // 2. Persist to .xopp file via repository
        documentRepository.saveDocument(active.notebook.fileName, updatedDoc)

        // 3. Update notebook metadata (page count and last modified)
        libraryStore.updateNotebook(
            active.notebook.copy(
                pageCount = updatedDoc.pages.size,
                lastModified = System.currentTimeMillis()
            )
        )

        newVersion
    }

    suspend fun addPage(notebookId: String, width: Double, height: Double, template: String): JSONObject? = withContext(Dispatchers.IO) {
        val active = openWebDocuments[notebookId] ?: return@withContext null
        val doc = active.document
        val pages = doc.pages.toMutableList()

        val newPage = Page(
            width = width,
            height = height,
            background = Background.Solid(0xFFFFFFFF.toInt(), template),
            layers = listOf(Layer(emptyList()))
        )
        pages.add(newPage)

        val updatedDoc = doc.copy(pages = pages)
        active.document = updatedDoc
        active.version.incrementAndGet()

        applyToTabletSurface(active.notebook, updatedDoc)

        documentRepository.saveDocument(active.notebook.fileName, updatedDoc)
        libraryStore.updateNotebook(
            active.notebook.copy(
                pageCount = updatedDoc.pages.size,
                lastModified = System.currentTimeMillis()
            )
        )

        LanProtocol.documentToJson(
            notebookId = active.notebook.id,
            fileName = active.notebook.fileName,
            title = active.notebook.name,
            document = updatedDoc,
            version = active.version.get()
        )
    }

    suspend fun saveDocument(notebookId: String): Boolean = withContext(Dispatchers.IO) {
        val active = openWebDocuments[notebookId] ?: return@withContext false
        documentRepository.saveDocument(active.notebook.fileName, active.document).isSuccess
    }

    /**
     * Called when the tablet user draws or makes edits locally in NeXopp.
     * Propagates the new document to active web sessions in real time.
     */
    fun onTabletDocumentEdited(fileNameOrId: String, updatedDoc: Document) {
        val entry = openWebDocuments.entries.find {
            matchesNotebook(fileNameOrId, it.value.notebook)
        } ?: return

        val active = entry.value
        active.document = updatedDoc
        val newVersion = active.version.incrementAndGet()

        val json = LanProtocol.documentToJson(
            notebookId = active.notebook.id,
            fileName = active.notebook.fileName,
            title = active.notebook.name,
            document = updatedDoc,
            version = newVersion
        )

        onBroadcastMessage?.invoke(LanProtocol.TYPE_TABLET_DOCUMENT_CHANGED, json.toString())
    }

    fun getActiveDocument(notebookId: String): ActiveWebDocument? {
        var active = openWebDocuments[notebookId]
        if (active == null) {
            active = openWebDocuments.values.find {
                it.notebook.id == notebookId || it.notebook.fileName == notebookId
            }
        }
        return active
    }
}
