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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.nexopp.audio.AudioSession
import com.nexopp.format.SaveFormat
import com.nexopp.io.DocumentIo
import com.nexopp.io.IncomingDocument
import com.nexopp.io.UriStaging
import com.nexopp.library.LibraryScreen
import com.nexopp.library.LibraryStore
import com.nexopp.panes.EditorPane
import com.nexopp.panes.MirrorSync
import com.nexopp.render.BitmapBudget
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.ImportPdfMode
import com.nexopp.render.PdfFonts
import com.nexopp.render.Placement
import com.nexopp.render.TextPdfGenerator
import com.nexopp.render.cancelSpline
import com.nexopp.render.finishSpline
import com.nexopp.render.splineInProgress
import com.nexopp.render.undoLastSplineNode
import com.nexopp.tabs.TabManager
import com.nexopp.tabs.TabStore
import com.nexopp.ui.AppSettings
import com.nexopp.ui.EditorScreen
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
            XoppTheme(darkTheme = settings.themeMode.isDark(), dynamicColor = settings.dynamicColor) {
                
                if (currentScreen.value == AppScreen.LIBRARY) {
                    LibraryScreen(
                        store = libraryStore,
                        onOpenNotebook = { 
                            // Más adelante aquí abriremos cuadernos guardados. 
                            // Por ahora, saltamos directamente al lienzo.
                            currentScreen.value = AppScreen.EDITOR
                        },
                        onSettings = { /* Implementaremos el menú de ajustes de la biblioteca pronto */ }
                    )
                } else {
                    EditorScreen(
                        onOpen = { openLauncher.launch(arrayOf("*/*")) },
                        onSave = { saveActiveTab() },
                        busy = busy.value,
                        onExit = { 
                            // En lugar de salir de la app, volvemos a la biblioteca
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
                            view.onDocumentEdited = { doc -> mirrors.propagate(p, doc) }
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
        panes.forEach { snapshotActiveTab(it); it.persist() }
        panes.forEach { it.awaitPersist(PERSIST_WAIT_MS) }
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