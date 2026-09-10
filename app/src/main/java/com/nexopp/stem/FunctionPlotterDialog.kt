package com.nexopp.stem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexopp.format.model.Element

val STEM_PLOT_COLORS = listOf(
    0xFF1976D2.toInt(), // Sapphire Blue
    0xFFD32F2F.toInt(), // Crimson Red
    0xFF388E3C.toInt(), // Emerald Green
    0xFF7B1FA2.toInt(), // Deep Purple
    0xFFF57C00.toInt(), // Amber Orange
    0xFF00838F.toInt(), // Teal
    0xFFE91E63.toInt(), // Magenta / Pink
    0xFF3F51B5.toInt(), // Indigo
    0xFF009688.toInt(), // Mint Turquoise
    0xFFFF9800.toInt(), // Bright Orange
    0xFF673AB7.toInt(), // Violet
    0xFF795548.toInt(), // Brown
    0xFF607D8B.toInt(), // Slate Blue
    0xFF000000.toInt(), // Black
)

val PRESET_FORMULAS = listOf(
    "sin(x)" to "Seno",
    "cos(x)" to "Coseno",
    "x^2" to "Parábola",
    "x^3 - 3*x" to "Cúbica",
    "1/x" to "Hipérbola",
    "exp(-x^2)" to "Gaussiana",
    "sqrt(abs(x))" to "Raíz",
    "sin(x)/x" to "Sinc",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionPlotterDialog(
    originX: Double = 250.0,
    originY: Double = 250.0,
    onDismiss: () -> Unit,
    onInsertPlot: (List<Element>) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Funciones f(x), 1: Paramétricas, 2: Puntos y Análisis, 3: Ejes y Rejilla

    var functions by remember {
        mutableStateOf(
            listOf(
                PlotFunctionItem(formula = "sin(x)", color = STEM_PLOT_COLORS[0]),
                PlotFunctionItem(formula = "cos(x)", color = STEM_PLOT_COLORS[1], isVisible = false)
            )
        )
    }

    var parametricFunctions by remember {
        mutableStateOf(
            listOf(
                ParametricFunctionItem(
                    formulaX = "3*cos(t)",
                    formulaY = "3*sin(t)",
                    tMin = 0.0,
                    tMax = 6.28318,
                    color = STEM_PLOT_COLORS[3],
                    isVisible = false
                )
            )
        )
    }

    var points by remember { mutableStateOf<List<PlotPoint>>(emptyList()) }
    var newPointLabel by remember { mutableStateOf("A") }
    var newPointX by remember { mutableStateOf("") }
    var newPointY by remember { mutableStateOf("") }

    var evalXStr by remember { mutableStateOf("0") }
    var evalResult by remember { mutableStateOf("") }

    // Range Bounds & Axes
    var xMinStr by remember { mutableStateOf("-5") }
    var xMaxStr by remember { mutableStateOf("5") }
    var yMinStr by remember { mutableStateOf("-4") }
    var yMaxStr by remember { mutableStateOf("4") }
    var axisNameX by remember { mutableStateOf("x") }
    var axisNameY by remember { mutableStateOf("y") }
    var stepXStr by remember { mutableStateOf("") }
    var stepYStr by remember { mutableStateOf("") }
    var drawAxes by remember { mutableStateOf(true) }
    var showAxisNumbers by remember { mutableStateOf(true) }
    var gridStyle by remember { mutableStateOf(PlotGridStyle.SUBTLE) }
    var highlightRoots by remember { mutableStateOf(false) }
    var highlightIntersections by remember { mutableStateOf(false) }
    var customColorDialogTargetIndex by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Graficador Matemático STEM", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                val xMin = xMinStr.toDoubleOrNull() ?: -5.0
                                val xMax = xMaxStr.toDoubleOrNull() ?: 5.0
                                val yMin = yMinStr.toDoubleOrNull() ?: -4.0
                                val yMax = yMaxStr.toDoubleOrNull() ?: 4.0
                                val stepX = stepXStr.toDoubleOrNull()
                                val stepY = stepYStr.toDoubleOrNull()

                                val elements = FunctionPlotter.generateAdvancedPlotElements(
                                    functions = functions,
                                    parametricFunctions = parametricFunctions,
                                    points = points,
                                    originX = originX,
                                    originY = originY,
                                    plotWidthPt = 320.0,
                                    plotHeightPt = 240.0,
                                    xMin = xMin,
                                    xMax = xMax,
                                    yMin = yMin,
                                    yMax = yMax,
                                    drawAxes = drawAxes,
                                    axisNameX = axisNameX,
                                    axisNameY = axisNameY,
                                    stepX = stepX,
                                    stepY = stepY,
                                    gridStyle = gridStyle,
                                    showAxisNumbers = showAxisNumbers,
                                    highlightRoots = highlightRoots,
                                    highlightIntersections = highlightIntersections
                                )
                                onInsertPlot(elements)
                                onDismiss()
                            },
                            enabled = functions.any { it.isVisible && it.formula.isNotBlank() } ||
                                    parametricFunctions.any { it.isVisible && it.formulaX.isNotBlank() && it.formulaY.isNotBlank() }
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Insertar en Página")
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                )

                // Tabs Navigation
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Funciones f(x)") },
                        icon = { Icon(Icons.Filled.Functions, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Paramétricas") },
                        icon = { Icon(Icons.Filled.Timeline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Puntos y Raíces") },
                        icon = { Icon(Icons.Filled.ScatterPlot, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Ejes y Cuadrícula") },
                        icon = { Icon(Icons.Filled.Grid4x4, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                HorizontalDivider()

                // Content for selected Tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Tab 0: Multiple Functions f(x)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Funciones simultáneas:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Button(
                                        onClick = {
                                            val nextColor = STEM_PLOT_COLORS[functions.size % STEM_PLOT_COLORS.size]
                                            functions = functions + PlotFunctionItem(formula = "x", color = nextColor)
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Añadir función")
                                    }
                                }

                                functions.forEachIndexed { index, fn ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        tonalElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = fn.isVisible,
                                                    onCheckedChange = { checked ->
                                                        functions = functions.mapIndexed { i, item ->
                                                            if (i == index) item.copy(isVisible = checked) else item
                                                        }
                                                    }
                                                )

                                                // Configurable Function Name (f, g, h, F, G...)
                                                OutlinedTextField(
                                                    value = fn.name,
                                                    onValueChange = { newName ->
                                                        functions = functions.mapIndexed { i, item ->
                                                            if (i == index) item.copy(name = newName.take(3)) else item
                                                        }
                                                    },
                                                    label = { Text("Nom.") },
                                                    singleLine = true,
                                                    modifier = Modifier.width(64.dp)
                                                )

                                                Spacer(Modifier.width(4.dp))

                                                // Configurable Variable (x, y, t, u, v, z, θ, α, β...)
                                                OutlinedTextField(
                                                    value = fn.variable,
                                                    onValueChange = { newVar ->
                                                        functions = functions.mapIndexed { i, item ->
                                                            if (i == index) item.copy(variable = newVar.take(3)) else item
                                                        }
                                                    },
                                                    label = { Text("Var.") },
                                                    singleLine = true,
                                                    modifier = Modifier.width(64.dp)
                                                )

                                                Spacer(Modifier.width(6.dp))

                                                OutlinedTextField(
                                                    value = fn.formula,
                                                    onValueChange = { newFormula ->
                                                        functions = functions.mapIndexed { i, item ->
                                                            if (i == index) item.copy(formula = newFormula) else item
                                                        }
                                                    },
                                                    prefix = {
                                                        Text("${fn.name.ifBlank { "f" }}(${fn.variable.ifBlank { "x" }}) = ", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    },
                                                    singleLine = true,
                                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Spacer(Modifier.width(8.dp))

                                                // Color Picker Swatches (Todos los colores + Personalizado)
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    STEM_PLOT_COLORS.forEach { colorVal ->
                                                        val isSelected = fn.color == colorVal
                                                        Box(
                                                            modifier = Modifier
                                                                .size(22.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(colorVal))
                                                                .clickable {
                                                                    functions = functions.mapIndexed { i, item ->
                                                                        if (i == index) item.copy(color = colorVal) else item
                                                                    }
                                                                }
                                                                .then(
                                                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                                    else Modifier
                                                                )
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { customColorDialogTargetIndex = index },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Palette,
                                                            contentDescription = "Color personalizado",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = if (fn.color !in STEM_PLOT_COLORS) Color(fn.color) else MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }

                                                if (functions.size > 1) {
                                                    IconButton(
                                                        onClick = {
                                                            functions = functions.filterIndexed { i, _ -> i != index }
                                                        }
                                                    ) {
                                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }

                                            // Line style & Stroke width
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Estilo:", style = MaterialTheme.typography.labelSmall)
                                                PlotLineStyle.values().forEach { style ->
                                                    FilterChip(
                                                        selected = fn.lineStyle == style,
                                                        onClick = {
                                                            functions = functions.mapIndexed { i, item ->
                                                                if (i == index) item.copy(lineStyle = style) else item
                                                            }
                                                        },
                                                        label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                                                        modifier = Modifier.height(28.dp)
                                                    )
                                                }
                                            }

                                            // Quick Math Buttons for active editing
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf("x", "^2", "+", "-", "*", "/", "sin(", "cos(", "tan(", "sqrt(", "exp(", "ln(", "abs(", "pi").forEach { symbol ->
                                                    AssistChip(
                                                        onClick = {
                                                            functions = functions.mapIndexed { i, item ->
                                                                if (i == index) item.copy(formula = item.formula + symbol) else item
                                                            }
                                                        },
                                                        label = { Text(symbol, style = MaterialTheme.typography.labelSmall) },
                                                        modifier = Modifier.height(26.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick presets
                                Text("Preajustes rápidos:", style = MaterialTheme.typography.labelMedium)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PRESET_FORMULAS.forEach { (presetFormula, name) ->
                                        FilterChip(
                                            selected = functions.firstOrNull()?.formula == presetFormula,
                                            onClick = {
                                                functions = listOf(PlotFunctionItem(formula = presetFormula, color = STEM_PLOT_COLORS.first()))
                                            },
                                            label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Tab 1: Parametric Functions
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Curvas Paramétricas (x(t), y(t)):", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                                parametricFunctions.forEachIndexed { index, pFn ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        tonalElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = pFn.isVisible,
                                                    onCheckedChange = { checked ->
                                                        parametricFunctions = parametricFunctions.mapIndexed { i, item ->
                                                            if (i == index) item.copy(isVisible = checked) else item
                                                        }
                                                    }
                                                )
                                                Text("Habilitar curva paramétrica ${index + 1}", fontWeight = FontWeight.Bold)
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = pFn.formulaX,
                                                    onValueChange = { newX ->
                                                        parametricFunctions = parametricFunctions.mapIndexed { i, item ->
                                                            if (i == index) item.copy(formulaX = newX) else item
                                                        }
                                                    },
                                                    prefix = { Text("x(t) = ", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                OutlinedTextField(
                                                    value = pFn.formulaY,
                                                    onValueChange = { newY ->
                                                        parametricFunctions = parametricFunctions.mapIndexed { i, item ->
                                                            if (i == index) item.copy(formulaY = newY) else item
                                                        }
                                                    },
                                                    prefix = { Text("y(t) = ", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Tab 2: Points & Roots Analysis
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 1. Live Function Evaluator
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("Evaluación de Función en Punto:", fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = evalXStr,
                                                onValueChange = { evalXStr = it },
                                                label = { Text("Valor de x") },
                                                singleLine = true,
                                                modifier = Modifier.width(120.dp)
                                            )

                                            Button(
                                                onClick = {
                                                    val x = evalXStr.toDoubleOrNull() ?: 0.0
                                                    val evals = functions.filter { it.isVisible }.map { fn ->
                                                        val y = FunctionPlotter.evaluate(fn.formula, x)
                                                        "f(x) = ${if (y.isNaN()) "Indefinido" else String.format(java.util.Locale.US, "%.4f", y)}"
                                                    }
                                                    evalResult = evals.joinToString("  |  ")
                                                }
                                            ) {
                                                Text("Evaluar f(x)")
                                            }

                                            if (evalResult.isNotBlank()) {
                                                Text(evalResult, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }

                                // 2. Automated Roots & Intersections Highlights
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Análisis Automático:", fontWeight = FontWeight.Bold)
                                        com.nexopp.ui.AppSwitchRow(
                                            label = "Calcular y marcar raíces / ceros (f(x) = 0)",
                                            description = "Detecta numéricamente las raíces y coloca marcadores",
                                            checked = highlightRoots,
                                            onCheckedChange = { highlightRoots = it }
                                        )
                                        com.nexopp.ui.AppSwitchRow(
                                            label = "Calcular y marcar intersecciones entre funciones",
                                            description = "Encuentra puntos de corte simultáneos entre curvas",
                                            checked = highlightIntersections,
                                            onCheckedChange = { highlightIntersections = it }
                                        )
                                    }
                                }

                                // 3. Custom Point Markers
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("Puntos Notables y Coordenadas:", fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = newPointLabel,
                                                onValueChange = { newPointLabel = it },
                                                label = { Text("Nombre") },
                                                singleLine = true,
                                                modifier = Modifier.width(80.dp)
                                            )
                                            OutlinedTextField(
                                                value = newPointX,
                                                onValueChange = { newPointX = it },
                                                label = { Text("Coord X") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = newPointY,
                                                onValueChange = { newPointY = it },
                                                label = { Text("Coord Y") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = {
                                                    val px = newPointX.toDoubleOrNull()
                                                    val py = newPointY.toDoubleOrNull()
                                                    if (px != null && py != null) {
                                                        val lbl = newPointLabel.trim().ifBlank { "P" }
                                                        points = points + PlotPoint(x = px, y = py, label = lbl)
                                                        newPointX = ""
                                                        newPointY = ""
                                                        val nextChar = if (lbl.length == 1 && lbl[0] in 'A'..'Y') (lbl[0] + 1).toString() else "P"
                                                        newPointLabel = nextChar
                                                    }
                                                },
                                                enabled = newPointX.isNotBlank() && newPointY.isNotBlank()
                                            ) {
                                                Text("Añadir punto")
                                            }
                                        }

                                        if (points.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                points.forEachIndexed { pIdx, pt ->
                                                    InputChip(
                                                        selected = true,
                                                        onClick = { points = points.filterIndexed { i, _ -> i != pIdx } },
                                                        label = { Text("(${pt.x}, ${pt.y})") },
                                                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Eliminar", modifier = Modifier.size(14.dp)) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Tab 3: Grid & Axes Settings
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Rangos y Límites de Visualización:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = xMinStr,
                                        onValueChange = { xMinStr = it },
                                        label = { Text("X mínimo") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = xMaxStr,
                                        onValueChange = { xMaxStr = it },
                                        label = { Text("X máximo") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = yMinStr,
                                        onValueChange = { yMinStr = it },
                                        label = { Text("Y mínimo") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = yMaxStr,
                                        onValueChange = { yMaxStr = it },
                                        label = { Text("Y máximo") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("Configuración de Ejes y Cuadrícula:", fontWeight = FontWeight.Bold)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = axisNameX,
                                                onValueChange = { axisNameX = it },
                                                label = { Text("Nombre eje X") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = axisNameY,
                                                onValueChange = { axisNameY = it },
                                                label = { Text("Nombre eje Y") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = stepXStr,
                                                onValueChange = { stepXStr = it },
                                                label = { Text("Paso / División X") },
                                                placeholder = { Text("Auto (1.0)") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = stepYStr,
                                                onValueChange = { stepYStr = it },
                                                label = { Text("Paso / División Y") },
                                                placeholder = { Text("Auto (1.0)") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            PlotGridStyle.values().forEach { style ->
                                                FilterChip(
                                                    selected = gridStyle == style,
                                                    onClick = { gridStyle = style },
                                                    label = { Text(style.label) }
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(4.dp))
                                        com.nexopp.ui.AppSwitchRow(
                                            label = "Mostrar valores numéricos en los ejes",
                                            description = "Muestra los números y escala en las marcas de los ejes X e Y",
                                            checked = showAxisNumbers,
                                            onCheckedChange = { showAxisNumbers = it }
                                        )

                                        com.nexopp.ui.AppSwitchRow(
                                            label = "Dibujar ejes coordenados cartesianos",
                                            description = "Incluye flechas en los extremos y etiquetas de los ejes",
                                            checked = drawAxes,
                                            onCheckedChange = { drawAxes = it }
                                        )

                                        // Quick Zoom Controls
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Zoom independiente de gráfica:", style = MaterialTheme.typography.labelSmall)
                                            OutlinedButton(
                                                onClick = {
                                                    val x0 = xMinStr.toDoubleOrNull() ?: -5.0
                                                    val x1 = xMaxStr.toDoubleOrNull() ?: 5.0
                                                    val y0 = yMinStr.toDoubleOrNull() ?: -4.0
                                                    val y1 = yMaxStr.toDoubleOrNull() ?: 4.0
                                                    xMinStr = String.format(java.util.Locale.US, "%.1f", x0 * 0.75)
                                                    xMaxStr = String.format(java.util.Locale.US, "%.1f", x1 * 0.75)
                                                    yMinStr = String.format(java.util.Locale.US, "%.1f", y0 * 0.75)
                                                    yMaxStr = String.format(java.util.Locale.US, "%.1f", y1 * 0.75)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Filled.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Zoom +", fontSize = 11.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    val x0 = xMinStr.toDoubleOrNull() ?: -5.0
                                                    val x1 = xMaxStr.toDoubleOrNull() ?: 5.0
                                                    val y0 = yMinStr.toDoubleOrNull() ?: -4.0
                                                    val y1 = yMaxStr.toDoubleOrNull() ?: 4.0
                                                    xMinStr = String.format(java.util.Locale.US, "%.1f", x0 * 1.33)
                                                    xMaxStr = String.format(java.util.Locale.US, "%.1f", x1 * 1.33)
                                                    yMinStr = String.format(java.util.Locale.US, "%.1f", y0 * 1.33)
                                                    yMaxStr = String.format(java.util.Locale.US, "%.1f", y1 * 1.33)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Filled.ZoomOut, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Zoom -", fontSize = 11.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    xMinStr = "-5"
                                                    xMaxStr = "5"
                                                    yMinStr = "-4"
                                                    yMaxStr = "4"
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Restablecer", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (customColorDialogTargetIndex != null) {
        val targetIdx = customColorDialogTargetIndex!!
        var hexInput by remember(targetIdx) {
            mutableStateOf(String.format(java.util.Locale.US, "%06X", (functions.getOrNull(targetIdx)?.color ?: 0) and 0xFFFFFF))
        }
        AlertDialog(
            onDismissRequest = { customColorDialogTargetIndex = null },
            title = { Text("Color Personalizado de Función") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Introduce el código de color hexadecimal (RRGGBB):", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                        },
                        prefix = { Text("#", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val parsedColor = runCatching {
                        if (hexInput.length == 6) (0xFF000000.toInt() or hexInput.toInt(16)) else null
                    }.getOrNull() ?: (functions.getOrNull(targetIdx)?.color ?: 0xFF1976D2.toInt())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Vista previa:", style = MaterialTheme.typography.labelMedium)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(parsedColor))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = runCatching {
                        if (hexInput.length == 6) (0xFF000000.toInt() or hexInput.toInt(16)) else null
                    }.getOrNull()
                    if (parsed != null) {
                        functions = functions.mapIndexed { i, item ->
                            if (i == targetIdx) item.copy(color = parsed) else item
                        }
                    }
                    customColorDialogTargetIndex = null
                }) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { customColorDialogTargetIndex = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
