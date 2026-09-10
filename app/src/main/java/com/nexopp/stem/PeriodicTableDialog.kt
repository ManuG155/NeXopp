package com.nexopp.stem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexopp.ui.TabletDimensions

enum class ElementCategory(val label: String, val colorHex: Long) {
    ALKALI_METAL("Metales alcalinos", 0xFFE53935),
    ALKALINE_EARTH("Alcalinotérreos", 0xFFFB8C00),
    TRANSITION_METAL("Metales de transición", 0xFF0288D1),
    POST_TRANSITION("Metales del bloque p", 0xFF5E35B1),
    METALLOID("Metaloides", 0xFF00897B),
    NON_METAL("No metales", 0xFF43A047),
    HALOGEN("Halógenos", 0xFFD81B60),
    NOBLE_GAS("Gases nobles", 0xFF8E24AA),
    LANTHANIDE("Lantánidos", 0xFF6D4C41),
    ACTINIDE("Actínidos", 0xFF546E7A)
}

data class ChemicalElement(
    val atomicNumber: Int,
    val symbol: String,
    val name: String,
    val atomicMass: Double,
    val category: ElementCategory,
    val group: Int,
    val period: Int,
    val electronegativity: Double? = null,
    val electronConfig: String = ""
)

object PeriodicTableRegistry {
    val elements: List<ChemicalElement> get() = PeriodicTableData.allElements
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicTableDialog(
    onDismiss: () -> Unit,
    onInsertElement: (com.nexopp.format.model.Element) -> Unit,
    onInsertText: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ElementCategory?>(null) }
    var selectedElement by remember { mutableStateOf<ChemicalElement?>(PeriodicTableRegistry.elements.first()) }
    var isGridView by remember { mutableStateOf(false) }

    val allElements = remember { PeriodicTableRegistry.elements }

    // Lookup table for the 18-col x 7-row main periodic grid
    val elementGridMap = remember(allElements) {
        val map = mutableMapOf<Pair<Int, Int>, ChemicalElement>()
        for (el in allElements) {
            // Main grid contains elements that are not separate lanthanides/actinides
            if (el.atomicNumber in 57..71 || el.atomicNumber in 89..103) {
                // Separate rows below
            } else {
                map[el.period to el.group] = el
            }
        }
        map
    }

    val lanthanides = remember(allElements) {
        allElements.filter { it.atomicNumber in 57..71 }.sortedBy { it.atomicNumber }
    }

    val actinides = remember(allElements) {
        allElements.filter { it.atomicNumber in 89..103 }.sortedBy { it.atomicNumber }
    }

    val filteredElements = remember(searchQuery, selectedCategory) {
        allElements.filter { el ->
            val matchesQuery = searchQuery.isBlank() ||
                    el.name.contains(searchQuery, ignoreCase = true) ||
                    el.symbol.contains(searchQuery, ignoreCase = true) ||
                    el.atomicNumber.toString() == searchQuery.trim()
            val matchesCat = selectedCategory == null || el.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    fun isElementMatch(el: ChemicalElement): Boolean {
        val matchesQuery = searchQuery.isBlank() ||
                el.name.contains(searchQuery, ignoreCase = true) ||
                el.symbol.contains(searchQuery, ignoreCase = true) ||
                el.atomicNumber.toString() == searchQuery.trim()
        val matchesCat = selectedCategory == null || el.category == selectedCategory
        return matchesQuery && matchesCat
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(TabletDimensions.DialogMaxWidthFraction)
                .fillMaxHeight(TabletDimensions.DialogMaxHeightFraction),
            shape = RoundedCornerShape(TabletDimensions.DialogCornerRadius),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(TabletDimensions.TopBarIconSize)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Tabla Periódica Completa (118 Elementos)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    if (isGridView) "Vista lista y búsqueda rápida" else "Disposición visual IUPAC (18 columnas × 7 períodos)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        // Toggle View Button
                        FilterChip(
                            selected = isGridView,
                            onClick = { isGridView = !isGridView },
                            label = { Text(if (isGridView) "Ver Tabla 18x7" else "Ver Cuadrícula") },
                            leadingIcon = {
                                Icon(
                                    if (isGridView) Icons.Filled.TableChart else Icons.Filled.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                val fullTable = PeriodicTableVisualBuilder.buildFullPeriodicTable()
                                onInsertElement(fullTable)
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Insertar Tabla Completa")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                selectedElement?.let { el ->
                                    val card = PeriodicTableVisualBuilder.buildElementCard(el)
                                    onInsertElement(card)
                                    onDismiss()
                                }
                            },
                            enabled = selectedElement != null
                        ) {
                            Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Insertar Ficha Elemento")
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                )

                // Search & Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar elemento por nombre, símbolo o Z…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .width(280.dp)
                            .height(48.dp)
                    )

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("Todos") }
                            )
                        }
                        items(ElementCategory.values()) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                },
                                label = { Text(cat.label) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.colorHex))
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Main Content Area: Visual Periodic Table + Detail Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    // Left area: Interactive Periodic Table or Grid
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (isGridView) {
                            // Quick Filtered Grid
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 76.dp),
                                contentPadding = PaddingValues(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredElements, key = { it.atomicNumber }) { el ->
                                    val isSelected = selectedElement?.atomicNumber == el.atomicNumber
                                    PeriodicElementCell(
                                        element = el,
                                        isSelected = isSelected,
                                        isDimmed = false,
                                        onClick = { selectedElement = el }
                                    )
                                }
                            }
                        } else {
                            // True 18-column x 7-row Visual Periodic Table layout
                            val hScroll = rememberScrollState()
                            val vScroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(hScroll)
                                    .verticalScroll(vScroll)
                                    .padding(4.dp)
                            ) {
                                // Group Header numbers (1 to 18)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.width(22.dp)) // period label gutter
                                    for (g in 1..18) {
                                        Box(
                                            modifier = Modifier.width(58.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$g",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Main 7 Periods
                                for (p in 1..7) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Period number label
                                        Box(
                                            modifier = Modifier
                                                .width(22.dp)
                                                .height(58.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$p",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }

                                        for (g in 1..18) {
                                            // Special placeholder slots in Group 3 for Lanthanides & Actinides
                                            if (p == 6 && g == 3) {
                                                PlaceholderGroupCell(
                                                    range = "57-71",
                                                    symbol = "La-Lu",
                                                    category = ElementCategory.LANTHANIDE,
                                                    onClick = {
                                                        selectedElement = lanthanides.firstOrNull()
                                                    }
                                                )
                                            } else if (p == 7 && g == 3) {
                                                PlaceholderGroupCell(
                                                    range = "89-103",
                                                    symbol = "Ac-Lr",
                                                    category = ElementCategory.ACTINIDE,
                                                    onClick = {
                                                        selectedElement = actinides.firstOrNull()
                                                    }
                                                )
                                            } else {
                                                val el = elementGridMap[p to g]
                                                if (el != null) {
                                                    val isSelected = selectedElement?.atomicNumber == el.atomicNumber
                                                    val isMatch = isElementMatch(el)
                                                    PeriodicElementCell(
                                                        element = el,
                                                        isSelected = isSelected,
                                                        isDimmed = (searchQuery.isNotBlank() || selectedCategory != null) && !isMatch,
                                                        onClick = { selectedElement = el }
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.size(58.dp))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                Spacer(Modifier.height(14.dp))

                                // Lanthanides Row (57-71)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Spacing to align with Group 3 (gutter 22dp + 2 cols * (58dp + 4dp))
                                    Box(
                                        modifier = Modifier.width(22.dp + (58.dp + 4.dp) * 2),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            "Lantánidos *",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(ElementCategory.LANTHANIDE.colorHex),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    lanthanides.forEach { el ->
                                        val isSelected = selectedElement?.atomicNumber == el.atomicNumber
                                        val isMatch = isElementMatch(el)
                                        PeriodicElementCell(
                                            element = el,
                                            isSelected = isSelected,
                                            isDimmed = (searchQuery.isNotBlank() || selectedCategory != null) && !isMatch,
                                            onClick = { selectedElement = el }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Actinides Row (89-103)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Spacing to align with Group 3
                                    Box(
                                        modifier = Modifier.width(22.dp + (58.dp + 4.dp) * 2),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            "Actínidos **",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(ElementCategory.ACTINIDE.colorHex),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    actinides.forEach { el ->
                                        val isSelected = selectedElement?.atomicNumber == el.atomicNumber
                                        val isMatch = isElementMatch(el)
                                        PeriodicElementCell(
                                            element = el,
                                            isSelected = isSelected,
                                            isDimmed = (searchQuery.isNotBlank() || selectedCategory != null) && !isMatch,
                                            onClick = { selectedElement = el }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    // Right Detail Panel
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 3.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (selectedElement != null) {
                                val el = selectedElement!!
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    // Big Element Badge
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(el.category.colorHex).copy(alpha = 0.20f),
                                        border = BorderStroke(2.5.dp, Color(el.category.colorHex)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Z = ${el.atomicNumber}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    String.format(java.util.Locale.US, "%.3f u", el.atomicMass),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    el.symbol,
                                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                                                )
                                            }
                                            Text(
                                                el.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // Category Tag
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(el.category.colorHex).copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(el.category.colorHex))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                el.category.label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(el.category.colorHex)
                                            )
                                        }
                                    }

                                    // Element Properties
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PropertyRow("Grupo / Periodo:", "Grupo ${el.group}  •  Periodo ${el.period}")
                                        PropertyRow(
                                            "Electronegatividad:",
                                            el.electronegativity?.let { "$it (Pauling)" } ?: "No disponible"
                                        )
                                        PropertyRow(
                                            "Configuración:",
                                            el.electronConfig.ifBlank { "N/A" },
                                            isMonospace = true
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val fullTable = PeriodicTableVisualBuilder.buildFullPeriodicTable()
                                            onInsertElement(fullTable)
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Insertar Tabla Completa")
                                    }
                                    Button(
                                        onClick = {
                                            val card = PeriodicTableVisualBuilder.buildElementCard(el)
                                            onInsertElement(card)
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Insertar Ficha Visual (Elemento)")
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Selecciona un elemento para ver sus propiedades",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
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
private fun PeriodicElementCell(
    element: ChemicalElement,
    isSelected: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(element.category.colorHex).copy(
            alpha = when {
                isSelected -> 0.55f
                isDimmed -> 0.07f
                else -> 0.22f
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(element.category.colorHex).copy(alpha = if (isDimmed) 0.25f else 0.85f)
        ),
        modifier = Modifier
            .size(58.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${element.atomicNumber}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDimmed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    String.format(java.util.Locale.US, "%.1f", element.atomicMass),
                    fontSize = 7.5.sp,
                    color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    element.symbol,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDimmed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                element.name,
                fontSize = 7.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaceholderGroupCell(
    range: String,
    symbol: String,
    category: ElementCategory,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(category.colorHex).copy(alpha = 0.18f),
        border = BorderStroke(1.dp, Color(category.colorHex).copy(alpha = 0.6f)),
        modifier = Modifier
            .size(58.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(range, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(category.colorHex))
            Text(symbol, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(category.colorHex))
            Text(category.label.take(8) + "…", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String, isMonospace: Boolean = false) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            )
        )
    }
}
