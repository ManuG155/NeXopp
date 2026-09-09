package com.nexopp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexopp.format.model.LineStyle
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.GuideKind

enum class TopBarSection(val label: String, val icon: ImageVector) {
    ESCRITURA("Escritura", Icons.Filled.Create),
    DIBUJO("Dibujo", Icons.Filled.SquareFoot),
    MATEMATICAS("Matemáticas", Icons.Filled.Calculate),
    GRAFICAS("Gráficas", Icons.AutoMirrored.Filled.ShowChart),
    TEXTO("Texto", Icons.Filled.TextFields),
    INSERTAR("Insertar", Icons.Filled.AddCircleOutline),
    PAGINAS("Páginas", Icons.Filled.AutoStories),
    AUDIO("Audio", Icons.Filled.Mic),
    EXPORTAR("Exportar", Icons.Filled.Share),
    AJUSTES("Ajustes", Icons.Filled.Settings)
}

val TEXT_FONT_SIZES = listOf(8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48)

@Composable
fun CategorizedTopBar(
    ui: EditorUiState,
    pane: PaneState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    audio: AudioUiState,
    onExit: () -> Unit,
    onAddPageQuick: () -> Unit,
    onOpenPageManager: () -> Unit,
    onOpenStructure: () -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenFunctionPlotter: () -> Unit,
    onOpenScientificCalculator: () -> Unit,
    onOpenTableDialog: () -> Unit,
    onOpenTechnicalSymbols: () -> Unit,
    onOpenPeriodicTable: () -> Unit,
    onOpenExportDialog: () -> Unit,
    onToggleFloatingCalculator: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDocument: () -> Unit,
    onNewDocument: () -> Unit,
    onSaveDocument: () -> Unit,
    onSaveAsDocument: () -> Unit,
    onImportPdf: () -> Unit,
    splitView: Boolean,
    onToggleSplitView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surface = pane.surface
    var activeSection by remember { mutableStateOf(TopBarSection.ESCRITURA) }
    var isMinimized by remember { mutableStateOf(false) }

    val styleCallbacks = ToolbarStyleCallbacks(
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
    )

    val layerCallbacks = toolbarLayerCallbacks(surface, pane)

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Fila 1: Barra Superior Principal con Navegación, Undo/Redo permanente, Pestañas de Secciones y Controles de Ventana
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Izquierda: Volver, Deshacer (Siempre visible), Rehacer (Siempre visible), Añadir Página '+' (Siempre accesible)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a la Biblioteca", modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(2.dp))

                    // Deshacer Permanente
                    IconButton(
                        onClick = { pane.surface?.undo() },
                        enabled = pane.canUndo,
                        modifier = Modifier
                            .size(34.dp)
                            .alpha(if (pane.canUndo) 1.0f else 0.38f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer", modifier = Modifier.size(20.dp))
                    }

                    // Rehacer Permanente
                    IconButton(
                        onClick = { pane.surface?.redo() },
                        enabled = pane.canRedo,
                        modifier = Modifier
                            .size(34.dp)
                            .alpha(if (pane.canRedo) 1.0f else 0.38f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer", modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(2.dp))

                    // Botón '+' Añadir Página Inmediata
                    IconButton(
                        onClick = onAddPageQuick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir Página Inmediatamente", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                // Centro: Selector de Secciones Categorizadas
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarSection.values().forEach { sec ->
                        val isSelected = activeSection == sec
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    activeSection = sec
                                    if (isMinimized) isMinimized = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    sec.icon,
                                    contentDescription = sec.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    sec.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Derecha: Capas, Fondo, Búsqueda, Minimizar/Expandir y Menú
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BackgroundPopupButton(pane.backgroundStyle, onBackgroundStyle = { surface?.setPageBackgroundStyle(it) })
                    LayersPopupButton(layerCallbacks)
                    SearchControls(pane)

                    // Toggle Minimizar / Expandir Barra
                    IconButton(
                        onClick = { isMinimized = !isMinimized },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            if (isMinimized) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                            contentDescription = if (isMinimized) "Expandir barra de herramientas" else "Minimizar barra para pantalla completa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OverflowMenu(
                        onOpen = onOpenDocument,
                        onNewTab = onNewDocument,
                        onSave = onSaveDocument,
                        onSaveAs = onSaveAsDocument,
                        onImportPdf = onImportPdf,
                        onExportPdf = onOpenExportDialog,
                        onSettings = onOpenSettings,
                        splitView = splitView,
                        onToggleSplitView = onToggleSplitView,
                        onOpenStructure = onOpenStructure,
                        onOpenAttachments = onOpenAttachments,
                        onAddPage = onAddPageQuick
                    )
                }
            }

            // Fila 2: Panel Contextual Dinámico según la Sección Activa (Plegable)
            AnimatedVisibility(
                visible = !isMinimized,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (activeSection) {
                                TopBarSection.ESCRITURA -> {
                                    EscrituraSectionContent(
                                        ui = ui,
                                        surface = surface,
                                        styleCallbacks = styleCallbacks,
                                        settings = settings,
                                        onSettingsChange = onSettingsChange
                                    )
                                }
                                TopBarSection.DIBUJO -> {
                                    DibujoSectionContent(
                                        ui = ui,
                                        surface = surface,
                                        styleCallbacks = styleCallbacks,
                                        settings = settings,
                                        onSettingsChange = onSettingsChange
                                    )
                                }
                                TopBarSection.MATEMATICAS -> {
                                    MatematicasSectionContent(
                                        ui = ui,
                                        pane = pane,
                                        surface = surface,
                                        onOpenTechnicalSymbols = onOpenTechnicalSymbols,
                                        onOpenPeriodicTable = onOpenPeriodicTable,
                                        onOpenScientificCalculator = onOpenScientificCalculator,
                                        onToggleFloatingCalculator = onToggleFloatingCalculator
                                    )
                                }
                                TopBarSection.GRAFICAS -> {
                                    GraficasSectionContent(
                                        onOpenPlotter = onOpenFunctionPlotter,
                                        surface = surface,
                                        pane = pane
                                    )
                                }
                                TopBarSection.TEXTO -> {
                                    TextoSectionContent(
                                        ui = ui,
                                        surface = surface,
                                        styleCallbacks = styleCallbacks
                                    )
                                }
                                TopBarSection.INSERTAR -> {
                                    InsertarSectionContent(
                                        ui = ui,
                                        pane = pane,
                                        surface = surface,
                                        onOpenTableDialog = onOpenTableDialog,
                                        onOpenTechnicalSymbols = onOpenTechnicalSymbols,
                                        onToggleFloatingCalculator = onToggleFloatingCalculator
                                    )
                                }
                                TopBarSection.PAGINAS -> {
                                    PaginasSectionContent(
                                        pane = pane,
                                        surface = surface,
                                        onAddPageQuick = onAddPageQuick,
                                        onOpenPageManager = onOpenPageManager,
                                        onOpenStructure = onOpenStructure,
                                        onOpenAttachments = onOpenAttachments
                                    )
                                }
                                TopBarSection.AUDIO -> {
                                    AudioSectionContent(
                                        audio = audio
                                    )
                                }
                                TopBarSection.EXPORTAR -> {
                                    ExportarSectionContent(
                                        onOpenExportDialog = onOpenExportDialog
                                    )
                                }
                                TopBarSection.AJUSTES -> {
                                    AjustesSectionContent(
                                        onOpenSettings = onOpenSettings,
                                        settings = settings,
                                        onSettingsChange = onSettingsChange
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EscrituraSectionContent(
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    styleCallbacks: ToolbarStyleCallbacks,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    // 1. Herramientas: Pluma, Subrayador, Borrador, Lazo
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        SectionToolChip(
            label = "Pluma",
            icon = Icons.Filled.Create,
            selected = ui.tool == EditorTool.PEN,
            onClick = { ui.tool = EditorTool.PEN; surface?.applyTool(EditorTool.PEN) }
        )
        SectionToolChip(
            label = "Subrayador",
            icon = Icons.Filled.Brush,
            selected = ui.tool == EditorTool.HIGHLIGHTER,
            onClick = { ui.tool = EditorTool.HIGHLIGHTER; surface?.applyTool(EditorTool.HIGHLIGHTER) }
        )
        SectionToolChip(
            label = "Borrador",
            icon = Icons.Filled.Delete,
            selected = ui.tool == EditorTool.ERASER || ui.tool == EditorTool.ERASER_WHOLE,
            onClick = { ui.tool = EditorTool.ERASER; surface?.applyTool(EditorTool.ERASER) }
        )
        SectionToolChip(
            label = "Lazo",
            icon = Icons.Filled.HighlightAlt,
            selected = ui.tool == EditorTool.LASSO_SELECT || ui.tool == EditorTool.SELECT,
            onClick = { ui.tool = EditorTool.LASSO_SELECT; surface?.applyTool(EditorTool.LASSO_SELECT) }
        )
    }

    VerticalDivider(modifier = Modifier.height(24.dp))

    // 2. Colores Rápidos Integrados
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        FAST_PEN_COLORS.take(6).forEach { colorInt ->
            val isSelected = ui.color == colorInt
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { styleCallbacks.onColor(colorInt) }
            )
        }
    }

    // 3. Selector de Grosor y Selector de Color Completo
    ToolbarColorPopup(styleCallbacks = styleCallbacks)

    VerticalDivider(modifier = Modifier.height(24.dp))

    // 4. Reconocer Formas Switch
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Formas auto", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(4.dp))
        Switch(
            checked = settings.recognizeShapes,
            onCheckedChange = {
                surface?.recognizeShapes = it
                onSettingsChange(settings.copy(recognizeShapes = it))
            },
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun DibujoSectionContent(
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    styleCallbacks: ToolbarStyleCallbacks,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val drawingTools = listOf(
        EditorTool.LINE to "Línea",
        EditorTool.ARROW to "Flecha",
        EditorTool.DOUBLE_ARROW to "Doble flecha",
        EditorTool.RECTANGLE to "Rectángulo",
        EditorTool.ELLIPSE to "Elipse",
        EditorTool.COORDINATE_AXIS to "Ejes",
        EditorTool.SPLINE to "Spline"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        drawingTools.forEach { (tool, label) ->
            SectionToolChip(
                label = label,
                icon = tool.icon,
                selected = ui.tool == tool,
                onClick = { ui.tool = tool; surface?.applyTool(tool) }
            )
        }
    }

    VerticalDivider(modifier = Modifier.height(24.dp))

    // Guías y Reglas
    var guideExpanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { guideExpanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Filled.SquareFoot, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Guía: ${settings.guideKind.name.lowercase().capitalize()}", fontSize = 11.sp)
        }
        DropdownMenu(expanded = guideExpanded, onDismissRequest = { guideExpanded = false }) {
            GuideKind.values().forEach { g ->
                DropdownMenuItem(
                    text = { Text(g.name.lowercase().capitalize()) },
                    onClick = {
                        surface?.placeGuide(g)
                        onSettingsChange(settings.copy(guideKind = g))
                        guideExpanded = false
                    }
                )
            }
        }
    }

    ToolbarColorPopup(styleCallbacks = styleCallbacks)
}

@Composable
private fun MatematicasSectionContent(
    ui: EditorUiState,
    pane: PaneState,
    surface: DrawingSurfaceView?,
    onOpenTechnicalSymbols: () -> Unit,
    onOpenPeriodicTable: () -> Unit,
    onOpenScientificCalculator: () -> Unit,
    onToggleFloatingCalculator: () -> Unit
) {
    // Modo Matemáticas Manuscrito Directo (Escritura 2D en lienzo con Stylus)
    Button(
        onClick = {
            ui.tool = EditorTool.MATH_INK
            surface?.applyTool(EditorTool.MATH_INK)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (ui.tool == EditorTool.MATH_INK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (ui.tool == EditorTool.MATH_INK) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Modo Matemáticas Manuscrito", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }

    // LaTeX Tradicional
    OutlinedButton(
        onClick = {
            ui.texPlacement = com.nexopp.render.Placement(pane.currentPage, 100.0, 100.0)
        },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(Icons.Filled.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("LaTeX Tradicional", fontSize = 11.sp)
    }

    OutlinedButton(
        onClick = onOpenTechnicalSymbols,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text("∫ Símbolos Técnicos", fontSize = 11.sp)
    }

    OutlinedButton(
        onClick = onOpenPeriodicTable,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text("⚗ Tabla Periódica", fontSize = 11.sp)
    }

    OutlinedButton(
        onClick = onToggleFloatingCalculator,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Calculadora Flotante", fontSize = 11.sp)
    }
}

@Composable
private fun GraficasSectionContent(
    onOpenPlotter: () -> Unit,
    surface: DrawingSurfaceView?,
    pane: PaneState
) {
    Button(
        onClick = onOpenPlotter,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Trazador de Funciones Vectorial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }

    VerticalDivider(modifier = Modifier.height(24.dp))

    // Preajustes rápidos de funciones STEM
    val presets = listOf(
        "sin(x)" to "sen(x)",
        "cos(x)" to "cos(x)",
        "x^2" to "x²",
        "exp(-x^2)" to "Gauss"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        presets.forEach { (formula, name) ->
            OutlinedButton(
                onClick = {
                    val elements = com.nexopp.stem.FunctionPlotter.generatePlotElements(
                        formula = formula,
                        originX = 250.0,
                        originY = 250.0
                    )
                    surface?.insertElements(elements)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(name, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TextoSectionContent(
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    styleCallbacks: ToolbarStyleCallbacks
) {
    SectionToolChip(
        label = "Texto",
        icon = Icons.Filled.TextFields,
        selected = ui.tool == EditorTool.TEXT,
        onClick = { ui.tool = EditorTool.TEXT; surface?.applyTool(EditorTool.TEXT) }
    )

    VerticalDivider(modifier = Modifier.height(24.dp))

    // Rich Text Format: Negrita, Cursiva, Subrayado
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var selectedFontSize by remember { mutableStateOf(11) } // Default 11 pt
    var fontSizeMenuExpanded by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { isBold = !isBold },
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        ) {
            Icon(Icons.Filled.FormatBold, contentDescription = "Negrita", modifier = Modifier.size(18.dp))
        }

        IconButton(
            onClick = { isItalic = !isItalic },
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        ) {
            Icon(Icons.Filled.FormatItalic, contentDescription = "Cursiva", modifier = Modifier.size(18.dp))
        }

        IconButton(
            onClick = { isUnderline = !isUnderline },
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isUnderline) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        ) {
            Icon(Icons.Filled.FormatUnderlined, contentDescription = "Subrayado", modifier = Modifier.size(18.dp))
        }
    }

    VerticalDivider(modifier = Modifier.height(24.dp))

    // Selector Numérico de Tamaño de Fuente (Default 11 pt)
    Box {
        OutlinedButton(
            onClick = { fontSizeMenuExpanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("$selectedFontSize pt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = fontSizeMenuExpanded, onDismissRequest = { fontSizeMenuExpanded = false }) {
            TEXT_FONT_SIZES.forEach { size ->
                DropdownMenuItem(
                    text = { Text("$size pt", fontWeight = if (size == selectedFontSize) FontWeight.Bold else FontWeight.Normal) },
                    trailingIcon = {
                        if (size == selectedFontSize) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = {
                        selectedFontSize = size
                        fontSizeMenuExpanded = false
                    }
                )
            }
        }
    }

    ToolbarColorPopup(styleCallbacks = styleCallbacks)
}

@Composable
private fun InsertarSectionContent(
    ui: EditorUiState,
    pane: PaneState,
    surface: DrawingSurfaceView?,
    onOpenTableDialog: () -> Unit,
    onOpenTechnicalSymbols: () -> Unit,
    onToggleFloatingCalculator: () -> Unit
) {
    OutlinedButton(
        onClick = onOpenTableDialog,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Tabla Interactiva", fontSize = 12.sp)
    }

    OutlinedButton(
        onClick = { ui.texPlacement = com.nexopp.render.Placement(pane.currentPage, 100.0, 100.0) },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.Filled.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Fórmula LaTeX", fontSize = 12.sp)
    }

    OutlinedButton(
        onClick = onOpenTechnicalSymbols,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Símbolo Técnico", fontSize = 12.sp)
    }

    OutlinedButton(
        onClick = onToggleFloatingCalculator,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Calculadora Flotante", fontSize = 12.sp)
    }
}

@Composable
private fun PaginasSectionContent(
    pane: PaneState,
    surface: DrawingSurfaceView?,
    onAddPageQuick: () -> Unit,
    onOpenPageManager: () -> Unit,
    onOpenStructure: () -> Unit,
    onOpenAttachments: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable { onOpenPageManager() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Filled.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(6.dp))
            Text(
                "Página ${pane.currentPage + 1} de ${pane.pageCount.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    Button(
        onClick = onAddPageQuick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Añadir Página", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }

    OutlinedButton(
        onClick = { surface?.removePage() },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Eliminar Página", fontSize = 11.sp)
    }

    OutlinedButton(
        onClick = onOpenStructure,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Índice / TOC", fontSize = 11.sp)
    }

    OutlinedButton(
        onClick = onOpenAttachments,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Adjuntos", fontSize = 11.sp)
    }
}

@Composable
private fun AudioSectionContent(
    audio: AudioUiState
) {
    Button(
        onClick = { audio.onToggleRecord() },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (audio.recording) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (audio.recording) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(
            if (audio.recording) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (audio.recording) "Detener Grabación" else "Grabar Audio (Offline)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }

    if (audio.playing) {
        OutlinedButton(
            onClick = { audio.onStopPlayback() },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Detener Reproducción", fontSize = 11.sp)
        }
    }
}

@Composable
private fun ExportarSectionContent(
    onOpenExportDialog: () -> Unit
) {
    Button(
        onClick = onOpenExportDialog,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Exportar y Compartir...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }

    Text(
        "PDF Vectorial, Imágenes PNG/JPEG, XOPP, Markdown y Backups.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AjustesSectionContent(
    onOpenSettings: () -> Unit,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    Button(
        onClick = onOpenSettings,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Abrir Ajustes Completos", fontSize = 12.sp)
    }

    VerticalDivider(modifier = Modifier.height(24.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Rechazo de palma", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(4.dp))
        Switch(
            checked = !settings.fingerDraws,
            onCheckedChange = { onSettingsChange(settings.copy(fingerDraws = !it)) },
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun SectionToolChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
