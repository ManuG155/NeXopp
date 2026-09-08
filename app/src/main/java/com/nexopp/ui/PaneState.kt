// Ruta: app/src/main/java/com/nexopp/ui/PaneState.kt
package com.nexopp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.LayerInfo

const val PANE_COUNT = 2

class PaneState {
    var surface by mutableStateOf<DrawingSurfaceView?>(null)
    var zoom by mutableStateOf(1f)
    var pageCount by mutableStateOf(1)
    var selectedPages by mutableStateOf(0)
    var copiedPages by mutableStateOf(0)
    var pagesEditMode by mutableStateOf(false)
    var currentPage by mutableStateOf(0)
    var scrollY by mutableStateOf(0f)
    var contentHeight by mutableStateOf(0f)
    var viewportHeight by mutableStateOf(0f)
    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
    var hasSelection by mutableStateOf(false)
    var hasTextSelection by mutableStateOf(false)
    var hasClipboard by mutableStateOf(false)
    var hasBackgroundRegion by mutableStateOf(false)
    var splineNodes by mutableStateOf(0)
    var searchOpen by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var searchCurrent by mutableStateOf(0)
    var searchTotal by mutableStateOf(0)
    var layers by mutableStateOf<List<LayerInfo>>(emptyList())
    var backgroundStyle by mutableStateOf<String?>(null)
    var pageSize by mutableStateOf<Pair<Double, Double>?>(null)
}