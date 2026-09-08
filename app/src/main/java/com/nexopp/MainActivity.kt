// Ruta: app/src/main/java/com/nexopp/MainActivity.kt
package com.nexopp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.nexopp.audio.AudioSession
import com.nexopp.format.SaveFormat
import com.nexopp.io.AutosaveManager
import com.nexopp.io.CrashRecoveryManager
import com.nexopp.io.DocumentIo
import com.nexopp.io.IncomingDocument
import com.nexopp.io.UriStaging
import com.nexopp.library.LibraryScreen
import com.nexopp.library.LibraryStore
import com.nexopp.library.Notebook
import com.nexopp.panes.EditorPane
import com.nexopp.panes.MirrorSync
import com.nexopp.render.BitmapBudget
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.ImportPdfMode
import com.nexopp.render.PdfFonts
import com.nexopp.render.Placement
import com.nexopp.render.TextPdfGenerator
import com.nexopp.render.blankDocument
import com.nexopp.render.cancelSpline
import com.nexopp.render.finishSpline
import com.nexopp.render.splineInProgress
import com.nexopp.render.undoLastSplineNode
import com.nexopp.tabs.OpenTab
import com.nexopp.tabs.TabManager
import com.nexopp.tabs.TabStore
import com.nexopp.ui.AppSettings
import com.nexopp.ui.EditorScreen
import com.nexopp.ui.SettingsScreen
import com.nexopp.ui.SettingsStore
import com.nexopp.ui.theme.XoppTheme
import com.nexopp.ui.theme.isDark
import java.io.File

class MainActivity : ComponentActivity() {

    internal enum class AppScreen { LIBRARY, EDITOR }
    internal var currentScreen = mutableStateOf(AppScreen.LIBRARY)
    internal val libraryStore by lazy { LibraryStore(this) }

    internal val panes: List<EditorPane> by lazy {
        TABS_DIRS.map { EditorPane(TabStore(File(filesDir, it))) }
    }

    internal val mirrors: MirrorSync by lazy { MirrorSync(panes) }
    internal var activePane = mutableStateOf(0)
    internal var splitView = mutableStateOf(false)
    internal val pane: EditorPane get() = panes[activePane.value.coerceIn(panes.indices)]
    internal val surface: DrawingSurfaceView? get() = pane.surface
    internal var pendingImagePlacement: Placement? = null
    private var pendingImportMode: ImportPdfMode = ImportPdfMode.REPLACE

    internal var pendingSaveName: String
        get() = pane.pendingSaveName
        set(value) { pane.pendingSaveName = value }

    internal var saveFormat: SaveFormat
        get() = pane.saveFormat
        set(value) { pane.saveFormat = value }

    internal val audio: AudioSession by lazy { AudioSession(this) }
    internal val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    internal var audioFolder: Uri? = null
    internal var audioTick = mutableStateOf(0)
    internal val tabs: TabManager get() = pane.tabs
    internal var tabsTick = mutableStateOf(0)

    internal val previewWorker: java.util.concurrent.ExecutorService by lazy {
        java.util.concurrent.Executors.newSingleThreadExecutor()
    }

    internal val io: DocumentIo by lazy {
        val fonts = PdfFonts(assets)
        DocumentIo(contentResolver, cacheDir, filesDir, TextPdfGenerator(fonts::load))
    }

    internal val autosaveManager by lazy {
        AutosaveManager(io).apply {
            onSaved = { savedFile ->
                val doc = surface?.toDocument()
                if (doc != null) {
                    val nbs = libraryStore.loadNotebooks()
                    val nb = nbs.find { it.fileName == savedFile.name }
                    if (nb != null) {
                        libraryStore.updateNotebook(
                            nb.copy(
                                lastModified = System.currentTimeMillis(),
                                pageCount = doc.pages.size
                            )
                        )
                    }
                }
            }
        }
    }

    internal val crashRecoveryManager by lazy {
        CrashRecoveryManager(File(filesDir, "recovery"), io)
    }

    internal val staging: UriStaging by lazy { UriStaging(contentResolver, File(cacheDir, "staging")) }
    internal var busy = mutableStateOf<String?>(null)

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { insertPickedImage(it) }
        }

    private class OpenDocumentForEditing : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent =
            super.createIntent(context, input).addFlags(
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
    }

    private val openLauncher =
        registerForActivityResult(OpenDocumentForEditing()) { uri ->
            uri?.let { openDocument(it) }
        }

    internal val saveLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(XOPP_MIME)) { uri ->
            uri?.let { saveDocument(it) }
        }

    private val importPdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importPdf(it, pendingImportMode) }
        }

    internal val audioFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let(::adoptAudioFolder)
        }

    internal val recordPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) beginRecording() else toast("Se necesita permiso de micrófono para grabar")
        }

    private val exportPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(PDF_MIME)) { uri ->
            uri?.let { exportPdf(it) }
        }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val view = surface
        if (event.action == KeyEvent.ACTION_UP && view != null && view.splineInProgress()) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> { view.finishSpline(); return true }
                KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL ->
                    { view.undoLastSplineNode(); return true }
                KeyEvent.KEYCODE_ESCAPE -> { view.cancelSpline(); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        BitmapBudget.configure(applicationContext)
        val store = settingsStore
        audioFolder = store.load().audioFolderUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
        audio.onStateChanged = { runOnUiThread { audioTick.value++ } }
        takeIncoming(intent)
        
        setContent {
            var settings by remember { mutableStateOf(store.load().also { applyStorageLimits(it) }) }
            var showLibrarySettings by remember { mutableStateOf(false) }

            XoppTheme(darkTheme = settings.themeMode.isDark(), dynamicColor = settings.dynamicColor) {
                
                if (currentScreen.value == AppScreen.LIBRARY) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LibraryScreen(
                            store = libraryStore,
                            onOpenNotebook = { notebook -> openLibraryNotebook(notebook) },
                            onSettings = { showLibrarySettings = true }
                        )
                        if (showLibrarySettings) {
                            SettingsScreen(
                                settings = settings,
                                onChange = { settings = it; store.save(it); applyStorageLimits(it) },
                                onBack = { showLibrarySettings = false }
                            )
                        }
                    }
                } else {
                    EditorScreen(
                        onOpen = { openLauncher.launch(arrayOf("*/*")) },
                        onSave = { saveActiveTab() },
                        busy = busy.value,
                        onExit = { 
                            autosaveManager.flushNow()
                            panes.forEach { snapshotActiveTab(it); it.persist() }
                            currentScreen.value = AppScreen.LIBRARY 
                        },
                        onSaveAs = { name, format -> beginSaveAs(name, format) },
                        currentSaveFormat = { saveFormat },
                        onImportPdf = { mode ->
                            pendingImportMode = mode
                            importPdfLauncher.launch(arrayOf(PDF_MIME))
                        },
                        onExportPdf = { exportPdfLauncher.launch("document.pdf") },
                        onPickImage = { placement ->
                            pendingImagePlacement = placement
                            pickImageLauncher.launch(arrayOf("image/*"))
                        },
                        onSurfaceCreated = { index, view ->
                            val p = panes[index]
                            p.surface = view
                            view.onDocumentEdited = { doc ->
                                mirrors.propagate(p, doc)
                                val tab = p.tabs.active
                                if (tab != null) {
                                    crashRecoveryManager.checkpoint(tab.id, doc, view.pdfSourceFile(), view.imageSources())
                                    val targetUri = tab.uri?.let(Uri::parse)
                                    if (targetUri != null && targetUri.scheme == "file") {
                                        val file = targetUri.path?.let(::File)
                                        if (file != null) {
                                            autosaveManager.scheduleAutosave(
                                                AutosaveManager.SaveTask(
                                                    document = doc,
                                                    targetFile = file,
                                                    pdfSource = view.pdfSourceFile(),
                                                    format = p.saveFormat,
                                                    images = view.imageSources()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            attachAudio(view)
                            restoreTabs(p) { openIncoming() }
                        },
                        settings = settings,
                        onSettingsChange = { settings = it; store.save(it); applyStorageLimits(it) },
                        audio = audioUiState(),
                        tabs = panes.map(::tabsUiState),
                        splitView = splitView.value,
                        onToggleSplitView = ::toggleSplitView,
                        activePane = activePane.value,
                        onActivePane = { activePane.value = it },
                    )
                }
            }
        }
    }

    internal fun openLibraryNotebook(notebook: Notebook) {
        val notebooksDir = File(filesDir, "notebooks").apply { mkdirs() }
        val file = File(notebooksDir, notebook.fileName)
        val uri = Uri.fromFile(file)
        
        currentScreen.value = AppScreen.EDITOR
        
        if (file.exists()) {
            openLibraryDocument(notebook, uri)
        } else {
            snapshotActiveTab()
            val initialDoc = com.nexopp.format.model.Document(
                pages = listOf(
                    com.nexopp.format.model.Page(
                        width = com.nexopp.render.DrawingSurfaceDefaults.A4_WIDTH_PT,
                        height = com.nexopp.render.DrawingSurfaceDefaults.A4_HEIGHT_PT,
                        background = com.nexopp.format.model.Background.Solid(0xFFFFFFFF.toInt(), notebook.initialTemplate),
                        layers = listOf(com.nexopp.format.model.Layer(emptyList()))
                    )
                )
            )
            val newTab = OpenTab(TabStore.newId(), notebook.name, initialDoc, uri.toString())
            tabs.open(newTab)
            pendingSaveName = notebook.name
            tabsTick.value++
            persistTabs()
            tabs.active?.let { show(it, pane) }
        }
    }

    internal fun openLibraryDocument(notebook: Notebook, uri: Uri) {
        snapshotActiveTab()
        val created = tabs.open(OpenTab(TabStore.newId(), notebook.name, blankDocument(), uri.toString()))
        pendingSaveName = notebook.name
        tabsTick.value++
        io.persist(uri)
        inBackground("Abriendo ${notebook.name}…", { io.stageIn(uri, "open") }) { result ->
            result.mapCatching { staged -> try { loadDocument(staged, uri) } finally { staged.delete() } }
                .onSuccess { 
                    tabs.updateActive { it.copy(title = notebook.name) } 
                    snapshotActiveTab() 
                }
                .onFailure {
                    toast("Error al abrir: ${it.message}")
                    tabs.close(created)
                    tabs.active?.let(::showTab)
                }
            tabsTick.value++
            persistTabs()
        }
    }

    private var pendingIntentUri: Uri? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeIncoming(intent)
        openIncoming()
    }

    private fun takeIncoming(intent: Intent?) {
        val stream = @Suppress("DEPRECATION") intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        val chosen = IncomingDocument.uriString(
            action = intent?.action,
            data = intent?.data?.toString(),
            stream = stream?.toString(),
        ) ?: return
        pendingIntentUri = Uri.parse(chosen)
    }

    private fun openIncoming() {
        val uri = pendingIntentUri ?: return
        pendingIntentUri = null
        currentScreen.value = AppScreen.EDITOR
        openDocument(uri)
    }

    internal fun <T> inBackground(label: String, work: () -> T, done: (Result<T>) -> Unit) {
        busy.value = label
        Thread {
            val result = runCatching(work)
            runOnUiThread {
                busy.value = null
                done(result)
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        autosaveManager.flushNow()
        panes.forEach { snapshotActiveTab(it); it.persist() }
        panes.forEach { it.awaitPersist(PERSIST_WAIT_MS) }
    }

    override fun onStop() {
        super.onStop()
        autosaveManager.flushNow()
    }

    override fun onDestroy() {
        audio.release()
        super.onDestroy()
    }

    internal fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    internal companion object {
        const val XOPP_MIME = "application/octet-stream"
        const val PDF_MIME = "application/pdf"
        val TABS_DIRS = listOf("tabs", "tabs-right")
        const val PERSIST_WAIT_MS = 2_000L
        internal const val UNTITLED = "Sin título"
    }
}