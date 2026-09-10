// Ruta: app/src/main/java/com/nexopp/ui/SideToolbar.kt
package com.nexopp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexopp.format.model.LineStyle
import com.nexopp.render.clearSelection
import com.nexopp.render.deleteSelection
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.EraserMode
import com.nexopp.render.GuideKind
import com.nexopp.render.Placement
import kotlin.math.roundToInt

// Colores destacados para subrayador (fluorescentes translúcidos)
val HIGHLIGHTER_COLORS = listOf(
    0x80FFFF00.toInt(), // Amarillo neón
    0x8080FF80.toInt(), // Verde neón
    0x8080FFFF.toInt(), // Celeste neón
    0x80FF80FF.toInt(), // Rosa neón
    0x80FFA500.toInt(), // Naranja neón
    0x80DDA0DD.toInt(), // Violeta neón
)

val FAST_PEN_COLORS = listOf(
    0xFF000000.toInt(), // Negro
    0xFF1976D2.toInt(), // Azul
    0xFFD32F2F.toInt(), // Rojo
    0xFF388E3C.toInt(), // Verde
    0xFFF57C00.toInt(), // Naranja
    0xFF7B1FA2.toInt(), // Púrpura
    0xFF0097A7.toInt(), // Cian
    0xFF5D4037.toInt(), // Marrón
)

@Composable
fun PrimaryToolSegment(
    currentTool: EditorTool,
    onSelectTool: (EditorTool) -> Unit,
    toolGroupSelections: Map<String, EditorTool>,
    onToolGroupSelections: (Map<String, EditorTool>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Pluma
        ToolPillButton(
            icon = Icons.Filled.Create,
            label = "Pluma",
            selected = currentTool == EditorTool.PEN,
            onClick = { onSelectTool(EditorTool.PEN) }
        )

        // 2. Subrayador
        ToolPillButton(
            icon = Icons.Filled.Brush,
            label = "Subrayador",
            selected = currentTool == EditorTool.HIGHLIGHTER,
            onClick = { onSelectTool(EditorTool.HIGHLIGHTER) }
        )

        // 3. Borrador (con submenú desplegable)
        ToolGroupDropdownButton(
            currentTool = currentTool,
            groupTools = listOf(EditorTool.ERASER, EditorTool.ERASER_WHOLE),
            defaultIcon = Icons.Filled.Delete,
            groupLabel = "Borrador",
            onSelectTool = onSelectTool
        )

        // 4. Selección / Lazo
        ToolGroupDropdownButton(
            currentTool = currentTool,
            groupTools = listOf(EditorTool.LASSO_SELECT, EditorTool.SELECT, EditorTool.TEXT_SELECT, EditorTool.BG_SELECT),
            defaultIcon = Icons.Filled.HighlightAlt,
            groupLabel = "Selección",
            onSelectTool = onSelectTool
        )

        // 5. Formas geométricas
        ToolGroupDropdownButton(
            currentTool = currentTool,
            groupTools = listOf(
                EditorTool.LINE, EditorTool.ARROW, EditorTool.DOUBLE_ARROW,
                EditorTool.RECTANGLE, EditorTool.ELLIPSE, EditorTool.COORDINATE_AXIS, EditorTool.SPLINE
            ),
            defaultIcon = Icons.Filled.ChangeHistory,
            groupLabel = "Formas",
            onSelectTool = onSelectTool
        )

        // 6. Texto
        ToolPillButton(
            icon = Icons.Filled.TextFields,
            label = "Texto",
            selected = currentTool == EditorTool.TEXT,
            onClick = { onSelectTool(EditorTool.TEXT) }
        )

        // 7. LaTeX tradicional
        ToolPillButton(
            icon = Icons.Filled.Functions,
            label = "LaTeX",
            selected = currentTool == EditorTool.TEXIMAGE,
            onClick = { onSelectTool(EditorTool.TEXIMAGE) }
        )

        // 9. Mano / Navegación
        ToolPillButton(
            icon = Icons.Filled.PanTool,
            label = "Mano",
            selected = currentTool == EditorTool.HAND,
            onClick = { onSelectTool(EditorTool.HAND) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolPillButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolGroupDropdownButton(
    currentTool: EditorTool,
    groupTools: List<EditorTool>,
    defaultIcon: ImageVector,
    groupLabel: String,
    onSelectTool: (EditorTool) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = currentTool in groupTools
    val activeMember = if (isActive) currentTool else groupTools.first()

    Box {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                .combinedClickable(
                    onClick = { onSelectTool(activeMember) },
                    onLongClick = { expanded = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                activeMember.icon,
                contentDescription = activeMember.label,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                groupLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            groupTools.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.label) },
                    leadingIcon = { Icon(member.icon, contentDescription = null) },
                    trailingIcon = {
                        if (member == currentTool) Icon(Icons.Filled.Check, contentDescription = "activo")
                    },
                    onClick = {
                        onSelectTool(member)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DynamicContextualBar(
    tool: EditorTool,
    surface: DrawingSurfaceView?,
    styleCallbacks: ToolbarStyleCallbacks,
    recognizeShapes: Boolean,
    onRecognizeShapes: (Boolean) -> Unit,
    guideKind: GuideKind,
    onGuideKind: (GuideKind) -> Unit,
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onInsertLatex: () -> Unit = {},
    onPlotFunction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCustomColor by remember { mutableStateOf(false) }
    var guideMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier.fillMaxWidth().height(42.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (tool) {
                EditorTool.PEN -> {
                    // Grosores para Pluma
                    val maxPt = styleCallbacks.widthSlots.maxOrNull() ?: styleCallbacks.width
                    styleCallbacks.widthSlots.forEach { pt ->
                        val isSelected = kotlin.math.abs(styleCallbacks.width - pt) < 0.05f
                        val dotSize = (4 + (pt / maxPt) * 16).coerceIn(4f, 20f).dp

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { styleCallbacks.onWidth(pt) }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(Color(styleCallbacks.color))
                            )
                        }
                    }

                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))

                    // Colores rápidos para Pluma
                    FAST_PEN_COLORS.forEach { c ->
                        ColorSwatch(
                            color = c,
                            selected = c == styleCallbacks.color,
                            onClick = { styleCallbacks.onColor(c) }
                        )
                    }

                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))

                    // Reconocimiento de formas (toggle)
                    FilterChip(
                        selected = recognizeShapes,
                        onClick = { onRecognizeShapes(!recognizeShapes) },
                        label = { Text("Reconocer formas", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.height(28.dp)
                    )

                    // Menú de Reglas e Instrumentos STEM
                    Box {
                        FilterChip(
                            selected = guideKind != GuideKind.NONE,
                            onClick = { guideMenuExpanded = true },
                            label = { Text(if (guideKind == GuideKind.NONE) "Instrumentos STEM" else guideKind.label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Filled.Straighten, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.height(28.dp)
                        )

                        DropdownMenu(
                            expanded = guideMenuExpanded,
                            onDismissRequest = { guideMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Desactivar guía") },
                                leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                                onClick = { onGuideKind(GuideKind.NONE); guideMenuExpanded = false }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Regla métrica (mm/cm)") },
                                leadingIcon = { Icon(Icons.Filled.Straighten, contentDescription = null) },
                                onClick = { onGuideKind(GuideKind.RULER); guideMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Cartabón (30°/60°)") },
                                leadingIcon = { Icon(Icons.Filled.ChangeHistory, contentDescription = null) },
                                onClick = { onGuideKind(GuideKind.SETSQUARE); guideMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Escuadra (45°)") },
                                leadingIcon = { Icon(Icons.Filled.Details, contentDescription = null) },
                                onClick = { onGuideKind(GuideKind.SETSQUARE_45); guideMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Transportador (180°)") },
                                leadingIcon = { Icon(Icons.Filled.PieChart, contentDescription = null) },
                                onClick = { onGuideKind(GuideKind.PROTRACTOR); guideMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Compás (Círculos)") },
                                leadingIcon = { Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null) },
                                onClick = { onGuideKind(GuideKind.COMPASS); guideMenuExpanded = false }
                            )
                        }
                    }
                }

                EditorTool.HIGHLIGHTER -> {
                    // Colores fluorescentes para Subrayador
                    HIGHLIGHTER_COLORS.forEach { c ->
                        ColorSwatch(
                            color = c,
                            selected = c == styleCallbacks.color,
                            onClick = { styleCallbacks.onColor(c) }
                        )
                    }

                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))

                    FilterChip(
                        selected = recognizeShapes,
                        onClick = { onRecognizeShapes(!recognizeShapes) },
                        label = { Text("Línea recta", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Filled.HorizontalRule, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.height(28.dp)
                    )

                    // Regla rápida con subrayador
                    FilterChip(
                        selected = guideKind == GuideKind.RULER,
                        onClick = { onGuideKind(if (guideKind == GuideKind.RULER) GuideKind.NONE else GuideKind.RULER) },
                        label = { Text("Regla", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Filled.Straighten, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.height(28.dp)
                    )
                }

                EditorTool.ERASER, EditorTool.ERASER_WHOLE -> {
                    FilterChip(
                        selected = surface?.eraserMode == EraserMode.STANDARD,
                        onClick = { surface?.eraserMode = EraserMode.STANDARD },
                        label = { Text("Borrador Preciso", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = surface?.eraserMode == EraserMode.WHOLE_STROKE,
                        onClick = { surface?.eraserMode = EraserMode.WHOLE_STROKE },
                        label = { Text("Trazo Completo", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.height(28.dp)
                    )

                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))

                    TextButton(
                        onClick = {
                            surface?.clearSelection()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Limpiar selección", style = MaterialTheme.typography.labelSmall)
                    }
                }

                EditorTool.SELECT, EditorTool.LASSO_SELECT, EditorTool.TEXT_SELECT, EditorTool.BG_SELECT -> {
                    FilterChip(
                        selected = tool == EditorTool.LASSO_SELECT,
                        onClick = { surface?.applyTool(EditorTool.LASSO_SELECT) },
                        label = { Text("Lazo libre", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = tool == EditorTool.SELECT,
                        onClick = { surface?.applyTool(EditorTool.SELECT) },
                        label = { Text("Rectangular", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = tool == EditorTool.TEXT_SELECT,
                        onClick = { surface?.applyTool(EditorTool.TEXT_SELECT) },
                        label = { Text("Texto PDF", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )

                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))

                    Text("Selecciona trazos en el lienzo para transformar, copiar o borrar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                EditorTool.LINE, EditorTool.ARROW, EditorTool.DOUBLE_ARROW,
                EditorTool.RECTANGLE, EditorTool.ELLIPSE, EditorTool.COORDINATE_AXIS, EditorTool.SPLINE -> {
                    // Variantes de formas
                    val shapes = listOf(
                        EditorTool.LINE to "Línea",
                        EditorTool.ARROW to "Flecha",
                        EditorTool.RECTANGLE to "Rectángulo",
                        EditorTool.ELLIPSE to "Elipse",
                        EditorTool.COORDINATE_AXIS to "Ejes",
                        EditorTool.SPLINE to "Spline"
                    )

                    shapes.forEach { (st, lbl) ->
                        FilterChip(
                            selected = tool == st,
                            onClick = { surface?.applyTool(st) },
                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))

                    // Botón Graficador de Funciones STEM
                    Button(
                        onClick = onPlotFunction,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Graficar f(x)", style = MaterialTheme.typography.labelSmall)
                    }

                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))

                    // Selector de color para formas
                    FAST_PEN_COLORS.take(5).forEach { c ->
                        ColorSwatch(
                            color = c,
                            selected = c == styleCallbacks.color,
                            onClick = { styleCallbacks.onColor(c) }
                        )
                    }
                }

                EditorTool.TEXT -> {
                    val textSizes = listOf(12f, 16f, 20f, 24f, 32f)
                    Text("Tamaño:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    textSizes.forEach { sz ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text("${sz.toInt()}pt", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))
                    Text("Toca en el lienzo para escribir texto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                EditorTool.TEXIMAGE -> {
                    Button(
                        onClick = onInsertLatex,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ecuación LaTeX", style = MaterialTheme.typography.labelSmall)
                    }

                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 4.dp))

                    val mathQuick = listOf("\\frac{a}{b}", "\\sqrt{x}", "x^2", "\\int", "\\sum", "\\alpha", "\\pi", "\\infty")
                    mathQuick.forEach { sym ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onInsertLatex() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(sym.replace("\\", ""), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                EditorTool.HAND -> {
                    IconButton(onClick = onZoomOut, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Alejar", modifier = Modifier.size(18.dp))
                    }
                    TextButton(onClick = onZoomReset, modifier = Modifier.height(28.dp)) {
                        Text("${(zoom * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onZoomIn, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Acercar", modifier = Modifier.size(18.dp))
                    }
                }

                else -> {
                    Text("Herramienta activa: ${tool.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showCustomColor) {
        CustomColorEditor(
            visible = true,
            palette = styleCallbacks.palette,
            onDismiss = { showCustomColor = false },
            onRedefine = {
                styleCallbacks.onRedefineCustom(it)
                styleCallbacks.onColor(it)
            }
        )
    }
}