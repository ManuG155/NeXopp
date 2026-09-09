package com.nexopp.stem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
    val elements: List<ChemicalElement> = listOf(
        ChemicalElement(1, "H", "Hidrógeno", 1.008, ElementCategory.NON_METAL, 1, 1, 2.20, "1s¹"),
        ChemicalElement(2, "He", "Helio", 4.0026, ElementCategory.NOBLE_GAS, 18, 1, null, "1s²"),
        ChemicalElement(3, "Li", "Litio", 6.94, ElementCategory.ALKALI_METAL, 1, 2, 0.98, "[He] 2s¹"),
        ChemicalElement(4, "Be", "Berilio", 9.0122, ElementCategory.ALKALINE_EARTH, 2, 2, 1.57, "[He] 2s²"),
        ChemicalElement(5, "B", "Boro", 10.81, ElementCategory.METALLOID, 13, 2, 2.04, "[He] 2s² 2p¹"),
        ChemicalElement(6, "C", "Carbono", 12.011, ElementCategory.NON_METAL, 14, 2, 2.55, "[He] 2s² 2p²"),
        ChemicalElement(7, "N", "Nitrógeno", 14.007, ElementCategory.NON_METAL, 15, 2, 3.04, "[He] 2s² 2p³"),
        ChemicalElement(8, "O", "Oxígeno", 15.999, ElementCategory.NON_METAL, 16, 2, 3.44, "[He] 2s² 2p⁴"),
        ChemicalElement(9, "F", "Flúor", 18.998, ElementCategory.HALOGEN, 17, 2, 3.98, "[He] 2s² 2p⁵"),
        ChemicalElement(10, "Ne", "Neón", 20.180, ElementCategory.NOBLE_GAS, 18, 2, null, "[He] 2s² 2p⁶"),
        ChemicalElement(11, "Na", "Sodio", 22.990, ElementCategory.ALKALI_METAL, 1, 3, 0.93, "[Ne] 3s¹"),
        ChemicalElement(12, "Mg", "Magnesio", 24.305, ElementCategory.ALKALINE_EARTH, 2, 3, 1.31, "[Ne] 3s²"),
        ChemicalElement(13, "Al", "Aluminio", 26.982, ElementCategory.POST_TRANSITION, 13, 3, 1.61, "[Ne] 3s² 3p¹"),
        ChemicalElement(14, "Si", "Silicio", 28.085, ElementCategory.METALLOID, 14, 3, 1.90, "[Ne] 3s² 3p²"),
        ChemicalElement(15, "P", "Fósforo", 30.974, ElementCategory.NON_METAL, 15, 3, 2.19, "[Ne] 3s² 3p³"),
        ChemicalElement(16, "S", "Azufre", 32.06, ElementCategory.NON_METAL, 16, 3, 2.58, "[Ne] 3s² 3p⁴"),
        ChemicalElement(17, "Cl", "Cloro", 35.45, ElementCategory.HALOGEN, 17, 3, 3.16, "[Ne] 3s² 3p⁵"),
        ChemicalElement(18, "Ar", "Argón", 39.948, ElementCategory.NOBLE_GAS, 18, 3, null, "[Ne] 3s² 3p⁶"),
        ChemicalElement(19, "K", "Potasio", 39.098, ElementCategory.ALKALI_METAL, 1, 4, 0.82, "[Ar] 4s¹"),
        ChemicalElement(20, "Ca", "Calcio", 40.078, ElementCategory.ALKALINE_EARTH, 2, 4, 1.00, "[Ar] 4s²"),
        ChemicalElement(21, "Sc", "Escandio", 44.956, ElementCategory.TRANSITION_METAL, 3, 4, 1.36, "[Ar] 3d¹ 4s²"),
        ChemicalElement(22, "Ti", "Titanio", 47.867, ElementCategory.TRANSITION_METAL, 4, 4, 1.54, "[Ar] 3d² 4s²"),
        ChemicalElement(23, "V", "Vanadio", 50.942, ElementCategory.TRANSITION_METAL, 5, 4, 1.63, "[Ar] 3d³ 4s²"),
        ChemicalElement(24, "Cr", "Cromo", 51.996, ElementCategory.TRANSITION_METAL, 6, 4, 1.66, "[Ar] 3d⁵ 4s¹"),
        ChemicalElement(25, "Mn", "Manganeso", 54.938, ElementCategory.TRANSITION_METAL, 7, 4, 1.55, "[Ar] 3d⁵ 4s²"),
        ChemicalElement(26, "Fe", "Hierro", 55.845, ElementCategory.TRANSITION_METAL, 8, 4, 1.83, "[Ar] 3d⁶ 4s²"),
        ChemicalElement(27, "Co", "Cobalto", 58.933, ElementCategory.TRANSITION_METAL, 9, 4, 1.88, "[Ar] 3d⁷ 4s²"),
        ChemicalElement(28, "Ni", "Níquel", 58.693, ElementCategory.TRANSITION_METAL, 10, 4, 1.91, "[Ar] 3d⁸ 4s²"),
        ChemicalElement(29, "Cu", "Cobre", 63.546, ElementCategory.TRANSITION_METAL, 11, 4, 1.90, "[Ar] 3d¹⁰ 4s¹"),
        ChemicalElement(30, "Zn", "Cinc", 65.38, ElementCategory.TRANSITION_METAL, 12, 4, 1.65, "[Ar] 3d¹⁰ 4s²"),
        ChemicalElement(31, "Ga", "Galio", 69.723, ElementCategory.POST_TRANSITION, 13, 4, 1.81, "[Ar] 3d¹⁰ 4s² 4p¹"),
        ChemicalElement(32, "Ge", "Germanio", 72.630, ElementCategory.METALLOID, 14, 4, 2.01, "[Ar] 3d¹⁰ 4s² 4p²"),
        ChemicalElement(33, "As", "Arsénico", 74.922, ElementCategory.METALLOID, 15, 4, 2.18, "[Ar] 3d¹⁰ 4s² 4p³"),
        ChemicalElement(34, "Se", "Selenio", 78.971, ElementCategory.NON_METAL, 16, 4, 2.55, "[Ar] 3d¹⁰ 4s² 4p⁴"),
        ChemicalElement(35, "Br", "Bromo", 79.904, ElementCategory.HALOGEN, 17, 4, 2.96, "[Ar] 3d¹⁰ 4s² 4p⁵"),
        ChemicalElement(36, "Kr", "Kriptón", 83.798, ElementCategory.NOBLE_GAS, 18, 4, 3.00, "[Ar] 3d¹⁰ 4s² 4p⁶"),
        ChemicalElement(47, "Ag", "Plata", 107.87, ElementCategory.TRANSITION_METAL, 11, 5, 1.93, "[Kr] 4d¹⁰ 5s¹"),
        ChemicalElement(79, "Au", "Oro", 196.97, ElementCategory.TRANSITION_METAL, 11, 6, 2.54, "[Xe] 4f¹⁴ 5d¹⁰ 6s¹"),
        ChemicalElement(80, "Hg", "Mercurio", 200.59, ElementCategory.TRANSITION_METAL, 12, 6, 2.00, "[Xe] 4f¹⁴ 5d¹⁰ 6s²"),
        ChemicalElement(82, "Pb", "Plomo", 207.2, ElementCategory.POST_TRANSITION, 14, 6, 2.33, "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p²"),
        ChemicalElement(92, "U", "Uranio", 238.03, ElementCategory.ACTINIDE, 3, 7, 1.38, "[Rn] 5f³ 6d¹ 7s²")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicTableDialog(
    onDismiss: () -> Unit,
    onInsertText: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ElementCategory?>(null) }
    var selectedElement by remember { mutableStateOf<ChemicalElement?>(PeriodicTableRegistry.elements.first()) }

    val filteredElements = remember(searchQuery, selectedCategory) {
        PeriodicTableRegistry.elements.filter { el ->
            val matchesQuery = searchQuery.isBlank() ||
                    el.name.contains(searchQuery, ignoreCase = true) ||
                    el.symbol.contains(searchQuery, ignoreCase = true) ||
                    el.atomicNumber.toString() == searchQuery.trim()
            val matchesCat = selectedCategory == null || el.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Tabla Periódica Interactiva", fontWeight = FontWeight.Bold)
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
                                selectedElement?.let { el ->
                                    val summary = "${el.name} (${el.symbol}) - Z: ${el.atomicNumber}, Masa: ${el.atomicMass} u, Config: ${el.electronConfig}"
                                    onInsertText(summary)
                                    onDismiss()
                                }
                            },
                            enabled = selectedElement != null
                        ) {
                            Icon(Icons.Filled.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Insertar en Nota")
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
                            .height(44.dp)
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
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.colorHex))
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Main Layout: Elements Grid + Detail Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Elements Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 72.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        items(filteredElements, key = { it.atomicNumber }) { el ->
                            val isSelected = selectedElement?.atomicNumber == el.atomicNumber
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(el.category.colorHex).copy(alpha = if (isSelected) 0.35f else 0.12f),
                                tonalElevation = if (isSelected) 4.dp else 1.dp,
                                modifier = Modifier
                                    .size(72.dp)
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, Color(el.category.colorHex), RoundedCornerShape(10.dp))
                                        else Modifier.border(1.dp, Color(el.category.colorHex).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    )
                                    .clickable { selectedElement = el }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${el.atomicNumber}",
                                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            el.symbol,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Text(
                                        el.name,
                                        style = TextStyle(fontSize = 9.sp),
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Detail Panel
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .width(280.dp)
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
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Big Element Badge
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(el.category.colorHex).copy(alpha = 0.18f),
                                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(el.category.colorHex)),
                                        modifier = Modifier.fillMaxWidth().height(110.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(10.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Z = ${el.atomicNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("${el.atomicMass} u", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Text(el.symbol, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                                            }
                                            Text(el.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Categoría: ${el.category.label}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text("Grupo: ${el.group}  •  Periodo: ${el.period}", style = MaterialTheme.typography.bodySmall)
                                        if (el.electronegativity != null) {
                                            Text("Electronegatividad (Pauling): ${el.electronegativity}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("Configuración electrónica: ${el.electronConfig}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                    }
                                }

                                Button(
                                    onClick = {
                                        val summary = "${el.name} (${el.symbol}) - Z: ${el.atomicNumber}, Masa: ${el.atomicMass} u, Config: ${el.electronConfig}"
                                        onInsertText(summary)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Insertar en Nota")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
