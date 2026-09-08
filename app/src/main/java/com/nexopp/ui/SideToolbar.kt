// Ruta: app/src/main/java/com/nexopp/ui/SideToolbar.kt
package com.nexopp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nexopp.render.GuideKind

@Composable
fun SideToolbar(
    tool: EditorTool,
    onTool: (EditorTool) -> Unit,
    toolGroupSelections: Map<String, EditorTool>,
    onToolGroupSelections: (Map<String, EditorTool>) -> Unit,
    styleCallbacks: ToolbarStyleCallbacks,
    presets: List<ToolPreset> = emptyList(),
    onPresets: (List<ToolPreset>) -> Unit = {},
    onActivatePreset: (ToolPreset) -> Unit = {},
    onCapturePreset: (String) -> ToolPreset = { ToolPreset(it, it, tool, styleCallbacks.color, styleCallbacks.width) },
    recognizeShapes: Boolean = false,
    onRecognizeShapes: (Boolean) -> Unit = {},
    guideKind: GuideKind,
    onGuideKind: (GuideKind) -> Unit,
    layerCallbacks: ToolbarLayerCallbacks,
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    pageCallbacks: ToolbarPagesCallbacks,
    backgroundStyle: String?,
    onBackgroundStyle: (String) -> Unit,
    audio: AudioUiState = AudioUiState(),
    railOrder: List<String> = emptyList(),
    railHidden: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 1.dp, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in visibleRailItems(railOrder, railHidden)) {
                val group = toolGroupForRailItem(item.id)
                if (group != null) {
                    ToolGroupButton(
                        group = group,
                        selected = group.selected(toolGroupSelections),
                        active = tool in group.tools,
                        onTool = onTool,
                        onSelect = { picked ->
                            onToolGroupSelections(group.withSelection(toolGroupSelections, picked))
                            onTool(picked)
                        },
                    )
                } else when (item.id) {
                    "color" -> ColorSizePopupButton(styleCallbacks)
                    "style" -> StylePopupButton(styleCallbacks.lineStyle, styleCallbacks.onLineStyle, styleCallbacks.fill, styleCallbacks.onFill)
                    "presets" -> PresetsPopupButton(presets, onPresets, onActivatePreset, onCapturePreset)
                    "shapes" -> ShapeRecognitionButton(recognizeShapes, onRecognizeShapes)
                    "guides" -> GuidePopupButton(guideKind, onGuideKind)
                    "layers" -> LayersPopupButton(layerCallbacks)
                    "zoom" -> ZoomPopupButton(zoom, onZoomIn, onZoomOut, onZoomReset)
                    "background" -> BackgroundPopupButton(backgroundStyle, onBackgroundStyle)
                    "pages" -> PagesPopupButton(pageCallbacks)
                    "audio" -> AudioPopupButton(audio)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolGroupButton(
    group: ToolGroup,
    selected: EditorTool,
    active: Boolean,
    onTool: (EditorTool) -> Unit,
    onSelect: (EditorTool) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box {
        Box(
            modifier = Modifier
                .size(ToolbarButtonSize)
                .clip(RoundedCornerShape(12.dp))
                .then(if (active) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                .combinedClickable(
                    onClick = { onTool(selected) },
                    onLongClick = { if (group.tools.size > 1) open = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(selected.icon, contentDescription = "Herramienta: ${selected.label}", tint = tint)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuHeading(group.label)
            for (member in group.tools) {
                DropdownMenuItem(
                    text = { Text(member.label) },
                    leadingIcon = { Icon(member.icon, contentDescription = null) },
                    trailingIcon = {
                        if (member == selected) Icon(Icons.Filled.Check, contentDescription = "seleccionado")
                    },
                    onClick = { onSelect(member); open = false; onTool(member) },
                )
            }
        }
    }
}