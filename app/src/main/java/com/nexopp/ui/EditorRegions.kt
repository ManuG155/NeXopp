// Ruta: app/src/main/java/com/nexopp/ui/EditorRegions.kt
package com.nexopp.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexopp.render.BarrelDoubleAction
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.PlaceKind
import com.nexopp.render.Placement
import com.nexopp.render.SearchStatus
import com.nexopp.ui.theme.rememberCanvasChromeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    ui: EditorUiState,
    pane: PaneState,
    tabs: TabsUiState,
    onOpen: () -> Unit,
    onNewTab: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    splitView: Boolean,
    onToggleSplitView: () -> Unit,
) {
    TopAppBar(
        title = {},
        modifier = Modifier.height(40.dp),
        actions = {
            SearchControls(pane)
            IconButton(onClick = { pane.surface?.undo() }, enabled = pane.canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
            }
            IconButton(onClick = { pane.surface?.redo() }, enabled = pane.canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer")
            }
            TabOverviewButton(tabs)
            OverflowMenu(
                onOpen = onOpen,
                onNewTab = onNewTab,
                onSave = onSave,
                onSaveAs = { ui.showSaveAs = true },
                onImportPdf = { ui.showImportPdf = true },
                onExportPdf = onExportPdf,
                onSettings = { ui.showSettings = true },
                splitView = splitView,
                onToggleSplitView = onToggleSplitView,
            )
        },
    )
}

@Composable
private fun SearchControls(pane: PaneState) {
    fun apply(status: SearchStatus) {
        pane.searchCurrent = status.current
        pane.searchTotal = status.total
    }
    if (!pane.searchOpen) {
        IconButton(onClick = {
            pane.searchOpen = true
            pane.surface?.setSearchQuery(pane.searchQuery)?.let(::apply)
        }) {
            Icon(Icons.Filled.Search, contentDescription = "Buscar")
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompactSearchField(
            value = pane.searchQuery,
            onValueChange = {
                pane.searchQuery = it
                pane.surface?.setSearchQuery(it)?.let(::apply) ?: apply(SearchStatus())
            },
        )
        Text("${pane.searchCurrent}/${pane.searchTotal}")
        CompactIconButton(
            contentDescription = "Coincidencia anterior",
            enabled = pane.searchTotal > 0,
            onClick = { pane.surface?.previousSearchHit()?.let(::apply) },
        ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Coincidencia anterior") }
        CompactIconButton(
            contentDescription = "Siguiente coincidencia",
            enabled = pane.searchTotal > 0,
            onClick = { pane.surface?.nextSearchHit()?.let(::apply) },
        ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Siguiente coincidencia") }
        CompactIconButton(contentDescription = "Cerrar búsqueda", onClick = {
            pane.searchOpen = false
            pane.searchQuery = ""
            pane.surface?.clearSearch()?.let(::apply) ?: apply(SearchStatus())
        }) { Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda") }
    }
}

@Composable
private fun CompactIconButton(
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        icon()
    }
}

@Composable
private fun CompactSearchField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .width(88.dp)
            .height(36.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text("Buscar", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        },
    )
}

@Composable
fun EditorToolbar(
    ui: EditorUiState,
    pane: PaneState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    audio: AudioUiState,
) {
    val surface = pane.surface
    SideToolbar(
        tool = ui.tool,
        onTool = { ui.tool = it; surface?.applyTool(it) },
        audio = audio,
        railOrder = settings.railOrder,
        railHidden = settings.railHidden,
        toolGroupSelections = settings.toolGroupSelections,
        onToolGroupSelections = { onSettingsChange(settings.copy(toolGroupSelections = it)) },
        styleCallbacks = ToolbarStyleCallbacks(
            color = ui.color,
            onColor = { ui.color = it; surface?.colorArgb = it; onSettingsChange(settings.withColorUsed(it)) },
            palette = rememberColorPaletteState(settings, onSettingsChange),
            onRedefineCustom = { newColor -> redefineCustomColor(newColor, ui, surface, settings, onSettingsChange) },
            width = ui.width,
            onWidth = { ui.width = it; surface?.baseWidthPt = it; onSettingsChange(settings.copy(lastWidth = it)) },
            widthSlots = settings.penWidths,
            onRedefineSlot = { i, newPt -> redefineWidthSlot(i, newPt, ui, surface, settings, onSettingsChange) },
            lineStyle = ui.lineStyle,
            onLineStyle = { ui.lineStyle = it; surface?.currentLineStyle = it },
            fill = settings.currentFill,
            onFill = { applyFill(it, surface, settings, onSettingsChange) },
        ),
        presets = settings.presets,
        onPresets = { onSettingsChange(settings.copy(presets = it)) },
        onActivatePreset = { applyToolPreset(it, ui, surface, settings, onSettingsChange) },
        onCapturePreset = { name -> ToolPreset.capture(ui, settings, name) },
        recognizeShapes = settings.recognizeShapes,
        onRecognizeShapes = {
            surface?.recognizeShapes = it
            onSettingsChange(settings.copy(recognizeShapes = it))
        },
        guideKind = settings.guideKind,
        onGuideKind = {
            surface?.placeGuide(it)
            onSettingsChange(settings.copy(guideKind = it))
        },
        layerCallbacks = toolbarLayerCallbacks(surface, pane),
        zoom = pane.zoom,
        onZoomIn = { surface?.zoomIn() },
        onZoomOut = { surface?.zoomOut() },
        onZoomReset = { surface?.resetZoom() },
        pageCallbacks = toolbarPagesCallbacks(pane, settings, onSettingsChange, surface),
        backgroundStyle = pane.backgroundStyle,
        onBackgroundStyle = { surface?.setPageBackgroundStyle(it) },
    )
}

private fun redefineCustomColor(
    newColor: Int,
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val old = settings.customColor
    onSettingsChange(settings.copy(customColor = newColor))
    if (ui.color == old) {
        ui.color = newColor
        surface?.colorArgb = newColor
        onSettingsChange(settings.copy(customColor = newColor).withColorUsed(newColor))
    }
}

private fun redefineWidthSlot(
    i: Int,
    newPt: Float,
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val old = settings.penWidths[i]
    val slots = settings.penWidths.toMutableList().also { it[i] = newPt }
    val active = ui.width == old
    onSettingsChange(
        settings.copy(penWidths = slots, lastWidth = if (active) newPt else settings.lastWidth)
    )
    if (active) { ui.width = newPt; surface?.baseWidthPt = newPt }
}

private fun applyFill(
    fill: Int?,
    surface: DrawingSurfaceView?,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    surface?.currentFill = fill
    onSettingsChange(
        settings.copy(fillEnabled = fill != null, fillAlpha = fill ?: settings.fillAlpha)
    )
}

@Composable
private fun toolbarLayerCallbacks(surface: DrawingSurfaceView?, pane: PaneState): ToolbarLayerCallbacks =
    ToolbarLayerCallbacks(
        layers = pane.layers,
        hasSelection = pane.hasSelection,
        onAddLayer = { surface?.addLayer() },
        onDeleteLayer = { i -> surface?.deleteLayer(i) },
        onMergeLayerDown = { i -> surface?.mergeLayerDown(i) },
        onRenameLayer = { i, name -> surface?.renameLayer(i, name) },
        onMoveLayer = { from, to -> surface?.moveLayer(from, to) },
        onActivateLayer = { surface?.setActiveLayer(it) },
        onToggleLayerHidden = { i, visible -> surface?.setLayerHidden(i, visible) },
        onMoveSelectionToLayer = { surface?.moveSelectionToLayer(it) },
    )

@Composable
private fun toolbarPagesCallbacks(
    pane: PaneState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    surface: DrawingSurfaceView?,
): ToolbarPagesCallbacks =
    ToolbarPagesCallbacks(
        pageCount = pane.pageCount,
        currentPage = pane.currentPage,
        onAddPage = { surface?.addPage() },
        onRemovePage = { surface?.removePage() },
        onGoToPage = { surface?.goToPage(it) },
        pageSize = pane.pageSize,
        onPageSize = { w, h -> surface?.setPageSize(w, h) },
        pageColumns = settings.pageColumns,
        onPageColumns = {
            surface?.setColumns(it)
            onSettingsChange(settings.copy(pageColumns = it))
        },
        pagesEditMode = pane.pagesEditMode,
        onPagesEditMode = { pane.pagesEditMode = it; surface?.setPagesEditMode(it) },
        selectedPages = pane.selectedPages,
        onDeleteSelectedPages = { surface?.deleteSelectedPages() },
        onClearPageSelection = { surface?.clearPageSelection() },
        copiedPages = pane.copiedPages,
        onCopySelectedPages = { surface?.copySelectedPages() },
        onPastePages = { surface?.pasteCopiedPages() },
    )

private fun DrawingSurfaceView.applyInitialStyle(ui: EditorUiState, settings: AppSettings) {
    applyTool(ui.tool)
    applySettings(settings)
    colorArgb = ui.color
    baseWidthPt = ui.width
    currentLineStyle = ui.lineStyle
    currentFill = settings.currentFill
}

private fun DrawingSurfaceView.bindTo(state: PaneState) {
    onLayersChanged = {
        state.layers = visibleLayers()
        state.backgroundStyle = visiblePageBackgroundStyle()
        state.pageSize = visiblePageSize()
    }
    state.layers = visibleLayers()
    state.backgroundStyle = visiblePageBackgroundStyle()
    state.pageSize = visiblePageSize()
    onHistoryChanged = { u, r -> state.canUndo = u; state.canRedo = r }
    onZoomChanged = { z -> state.zoom = z }
    onPageCountChanged = { n -> state.pageCount = n }
    onPageSelectionChanged = { n -> state.selectedPages = n }
    onPageClipboardChanged = { n -> state.copiedPages = n }
    onCurrentPageChanged = { page ->
        state.currentPage = page
        state.backgroundStyle = visiblePageBackgroundStyle()
        state.pageSize = visiblePageSize()
    }
    onScrollChanged = { y, total, vp -> state.scrollY = y; state.contentHeight = total; state.viewportHeight = vp }
    onSelectionChanged = { s -> state.hasSelection = s }
    onTextSelectionChanged = { s -> state.hasTextSelection = s }
    onClipboardChanged = { c -> state.hasClipboard = c }
    onBackgroundRegionChanged = { r -> state.hasBackgroundRegion = r }
    onSplineChanged = { n -> state.splineNodes = n }
    onSearchChanged = { s -> state.searchCurrent = s.current; state.searchTotal = s.total }
}

private fun DrawingSurfaceView.bindEditorActions(
    ui: EditorUiState,
    index: Int,
    onActivePane: (Int) -> Unit,
    onPickImage: (Placement) -> Unit,
    getSettings: () -> AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    onToggleFullPage = { ui.fullPage = !ui.fullPage }
    onBarrelDoubleClick = { action ->
        when (action) {
            BarrelDoubleAction.TOGGLE_ERASER -> ui.toggleTool(EditorTool.ERASER)
            BarrelDoubleAction.TOGGLE_SELECT -> ui.toggleTool(EditorTool.SELECT)
            BarrelDoubleAction.TOGGLE_FULL_PAGE -> ui.fullPage = !ui.fullPage
            else -> Unit
        }
        applyTool(ui.tool)
    }
    onPaletteAction = { action ->
        applyPaletteAction(action, ui, this, getSettings(), onSettingsChange)
    }
    onPlace = { kind, placement ->
        onActivePane(index)
        when (kind) {
            PlaceKind.TEXT -> ui.textPlacement = placement
            PlaceKind.TEX -> ui.texPlacement = placement
            PlaceKind.IMAGE -> onPickImage(placement)
        }
    }
}

@Composable
fun EditorPaneView(
    index: Int,
    ui: EditorUiState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    tabs: List<TabsUiState>,
    onActivePane: (Int) -> Unit,
    onSurfaceCreated: (Int, DrawingSurfaceView) -> Unit,
    onPickImage: (Placement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ui.panes[index]
    val chrome = rememberCanvasChromeColors()
    val latestSettings = rememberUpdatedState(settings)
    Column(
        modifier = modifier.pointerInput(index) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                onActivePane(index)
            }
        },
    ) {
        if (!ui.fullPage) TabStrip(tabs[index.coerceIn(tabs.indices)], modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    DrawingSurfaceView(ctx).also {
                        it.applyInitialStyle(ui, settings)
                        it.bindTo(state)
                        it.bindEditorActions(ui, index, onActivePane, onPickImage, { latestSettings.value }, onSettingsChange)
                        state.surface = it
                        onSurfaceCreated(index, it)
                    }
                },
                update = { it.applyChromeColors(chrome.backdrop, chrome.selection, chrome.guide) },
                modifier = Modifier.fillMaxSize(),
            )
            ScrollThumb(
                scrollY = state.scrollY,
                totalHeightPx = state.contentHeight,
                viewportPx = state.viewportHeight,
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                onScrollTo = { state.surface?.scrollToY(it) },
                modifier = Modifier.matchParentSize(),
            )
            PageCounter(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                modifier = Modifier
                    .align(pageCounterAlignment(settings.pageCounterVertical, settings.pageCounterHorizontal))
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun OverflowMenu(
    onOpen: () -> Unit,
    onNewTab: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onImportPdf: () -> Unit,
    onExportPdf: () -> Unit,
    onSettings: () -> Unit,
    splitView: Boolean,
    onToggleSplitView: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.Menu, contentDescription = "Menú")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Abrir") },
            leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
            onClick = { open = false; onOpen() },
        )
        DropdownMenuItem(
            text = { Text("Nuevo documento") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null) },
            onClick = { open = false; onNewTab() },
        )
        DropdownMenuItem(
            text = { Text("Importar PDF") },
            leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
            onClick = { open = false; onImportPdf() },
        )
        DropdownMenuItem(
            text = { Text("Exportar a PDF") },
            leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
            onClick = { open = false; onExportPdf() },
        )
        DropdownMenuItem(
            text = { Text("Guardar") },
            leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null) },
            onClick = { open = false; onSave() },
        )
        DropdownMenuItem(
            text = { Text("Guardar como…") },
            leadingIcon = { Icon(Icons.Filled.SaveAs, contentDescription = null) },
            onClick = { open = false; onSaveAs() },
        )
        DropdownMenuItem(
            text = { Text(if (splitView) "Cerrar vista dividida" else "Vista dividida") },
            leadingIcon = { Icon(Icons.Filled.VerticalSplit, contentDescription = null) },
            onClick = { open = false; onToggleSplitView() },
        )
        DropdownMenuItem(
            text = { Text("Ajustes") },
            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            onClick = { open = false; onSettings() },
        )
    }
}