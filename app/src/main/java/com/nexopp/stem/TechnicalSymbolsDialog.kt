package com.nexopp.stem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class SymbolCategory(val label: String) {
    GREEK_LOWER("Griegas (min)"),
    GREEK_UPPER("Griegas (MAY)"),
    CALCULUS("Cálculo y Álgebra"),
    PHYSICS_EE("Física e Ing."),
    LOGIC_SETS("Lógica y Conjuntos"),
    ARROWS_REL("Flechas y Rel.")
}

data class TechnicalSymbol(
    val char: String,
    val latex: String,
    val name: String
)

object TechnicalSymbolsRegistry {
    val categories: Map<SymbolCategory, List<TechnicalSymbol>> = mapOf(
        SymbolCategory.GREEK_LOWER to listOf(
            TechnicalSymbol("α", "\\alpha", "Alfa"),
            TechnicalSymbol("β", "\\beta", "Beta"),
            TechnicalSymbol("γ", "\\gamma", "Gamma"),
            TechnicalSymbol("δ", "\\delta", "Delta"),
            TechnicalSymbol("ε", "\\epsilon", "Épsilon"),
            TechnicalSymbol("ζ", "\\zeta", "Zeta"),
            TechnicalSymbol("η", "\\eta", "Eta"),
            TechnicalSymbol("θ", "\\theta", "Theta"),
            TechnicalSymbol("ι", "\\iota", "Iota"),
            TechnicalSymbol("κ", "\\kappa", "Kappa"),
            TechnicalSymbol("λ", "\\lambda", "Lambda"),
            TechnicalSymbol("μ", "\\mu", "Mu (micro)"),
            TechnicalSymbol("ν", "\\nu", "Nu"),
            TechnicalSymbol("ξ", "\\xi", "Xi"),
            TechnicalSymbol("π", "\\pi", "Pi"),
            TechnicalSymbol("ρ", "\\rho", "Rho"),
            TechnicalSymbol("σ", "\\sigma", "Sigma"),
            TechnicalSymbol("τ", "\\tau", "Tau"),
            TechnicalSymbol("υ", "\\upsilon", "Ípsilon"),
            TechnicalSymbol("φ", "\\phi", "Phi"),
            TechnicalSymbol("χ", "\\chi", "Chi"),
            TechnicalSymbol("ψ", "\\psi", "Psi"),
            TechnicalSymbol("ω", "\\omega", "Omega")
        ),
        SymbolCategory.GREEK_UPPER to listOf(
            TechnicalSymbol("Γ", "\\Gamma", "Gamma"),
            TechnicalSymbol("Δ", "\\Delta", "Delta"),
            TechnicalSymbol("Θ", "\\Theta", "Theta"),
            TechnicalSymbol("Λ", "\\Lambda", "Lambda"),
            TechnicalSymbol("Ξ", "\\Xi", "Xi"),
            TechnicalSymbol("Π", "\\Pi", "Pi"),
            TechnicalSymbol("Σ", "\\Sigma", "Sigma"),
            TechnicalSymbol("Φ", "\\Phi", "Phi"),
            TechnicalSymbol("Ψ", "\\Psi", "Psi"),
            TechnicalSymbol("Ω", "\\Omega", "Omega (Ohm)")
        ),
        SymbolCategory.CALCULUS to listOf(
            TechnicalSymbol("∫", "\\int", "Integral simple"),
            TechnicalSymbol("∬", "\\iint", "Integral doble"),
            TechnicalSymbol("∭", "\\iiint", "Integral triple"),
            TechnicalSymbol("∮", "\\oint", "Integral de línea cerrada"),
            TechnicalSymbol("∂", "\\partial", "Derivada parcial"),
            TechnicalSymbol("∇", "\\nabla", "Nabla / Gradiente"),
            TechnicalSymbol("∑", "\\sum", "Sumatorio"),
            TechnicalSymbol("∏", "\\prod", "Productorio"),
            TechnicalSymbol("lim", "\\lim", "Límite"),
            TechnicalSymbol("√", "\\sqrt{}", "Raíz cuadrada"),
            TechnicalSymbol("∛", "\\sqrt[3]{}", "Raíz cúbica"),
            TechnicalSymbol("∞", "\\infty", "Infinito"),
            TechnicalSymbol("±", "\\pm", "Más / Menos"),
            TechnicalSymbol("∓", "\\mp", "Menos / Más"),
            TechnicalSymbol("×", "\\times", "Multiplicación / Producto cruz"),
            TechnicalSymbol("·", "\\cdot", "Punto / Producto escalar"),
            TechnicalSymbol("÷", "\\div", "División"),
            TechnicalSymbol("≈", "\\approx", "Aproximadamente igual"),
            TechnicalSymbol("≠", "\\neq", "Distinto de"),
            TechnicalSymbol("≤", "\\leq", "Menor o igual que"),
            TechnicalSymbol("≥", "\\geq", "Mayor o igual que"),
            TechnicalSymbol("≡", "\\equiv", "Idéntico / Congruente"),
            TechnicalSymbol("∝", "\\propto", "Proporcional a")
        ),
        SymbolCategory.PHYSICS_EE to listOf(
            TechnicalSymbol("Ω", "\\Omega", "Ohm (Resistencia)"),
            TechnicalSymbol("µ", "\\mu", "Micro (10⁻⁶)"),
            TechnicalSymbol("∠", "\\angle", "Fasor / Ángulo"),
            TechnicalSymbol("⊥", "\\perp", "Perpendicular / Ortogonal"),
            TechnicalSymbol("∥", "\\parallel", "Paralelo"),
            TechnicalSymbol("ℏ", "\\hbar", "Constante de Planck reducida"),
            TechnicalSymbol("Å", "\\AA", "Angstrom"),
            TechnicalSymbol("℃", "^{\\circ}C", "Grados Celsius"),
            TechnicalSymbol("℉", "^{\\circ}F", "Grados Fahrenheit"),
            TechnicalSymbol("∅", "\\emptyset", "Diámetro / Vacío"),
            TechnicalSymbol("∢", "\\measuredangle", "Ángulo medido")
        ),
        SymbolCategory.LOGIC_SETS to listOf(
            TechnicalSymbol("∀", "\\forall", "Para todo"),
            TechnicalSymbol("∃", "\\exists", "Existe"),
            TechnicalSymbol("∄", "\\nexists", "No existe"),
            TechnicalSymbol("∈", "\\in", "Pertenece a"),
            TechnicalSymbol("∉", "\\notin", "No pertenece a"),
            TechnicalSymbol("⊂", "\\subset", "Subconjunto propio"),
            TechnicalSymbol("⊆", "\\subseteq", "Subconjunto"),
            TechnicalSymbol("∪", "\\cup", "Unión"),
            TechnicalSymbol("∩", "\\cap", "Intersección"),
            TechnicalSymbol("∖", "\\setminus", "Diferencia de conjuntos"),
            TechnicalSymbol("∧", "\\land", "Conjunción lógica (AND)"),
            TechnicalSymbol("∨", "\\lor", "Disyunción lógica (OR)"),
            TechnicalSymbol("¬", "\\neg", "Negación (NOT)"),
            TechnicalSymbol("⇒", "\\implies", "Implica"),
            TechnicalSymbol("⇔", "\\iff", "Si y sólo si"),
            TechnicalSymbol("∴", "\\therefore", "Por lo tanto"),
            TechnicalSymbol("∵", "\\because", "Porque / Dado que"),
            TechnicalSymbol("ℝ", "\\mathbb{R}", "Números Reales"),
            TechnicalSymbol("ℂ", "\\mathbb{C}", "Números Complejos"),
            TechnicalSymbol("ℕ", "\\mathbb{N}", "Números Naturales"),
            TechnicalSymbol("ℤ", "\\mathbb{Z}", "Números Enteros")
        ),
        SymbolCategory.ARROWS_REL to listOf(
            TechnicalSymbol("→", "\\to", "Flecha derecha"),
            TechnicalSymbol("←", "\\gets", "Flecha izquierda"),
            TechnicalSymbol("↔", "\\leftrightarrow", "Flecha bidireccional"),
            TechnicalSymbol("⇒", "\\Rightarrow", "Doble flecha derecha"),
            TechnicalSymbol("⇐", "\\Leftarrow", "Doble flecha izquierda"),
            TechnicalSymbol("⇔", "\\Leftrightarrow", "Doble flecha bidireccional"),
            TechnicalSymbol("↑", "\\uparrow", "Flecha arriba"),
            TechnicalSymbol("↓", "\\downarrow", "Flecha abajo"),
            TechnicalSymbol("↦", "\\mapsto", "Mapea a"),
            TechnicalSymbol("↗", "\\nearrow", "Noreste"),
            TechnicalSymbol("↘", "\\searrow", "Sureste")
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalSymbolsDialog(
    onDismiss: () -> Unit,
    onInsertSymbol: (symbol: TechnicalSymbol, asLatex: Boolean) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(SymbolCategory.GREEK_LOWER) }
    var selectedSymbol by remember { mutableStateOf<TechnicalSymbol?>(null) }
    var insertAsLatex by remember { mutableStateOf(false) }

    val currentSymbols = remember(selectedCategory) {
        TechnicalSymbolsRegistry.categories[selectedCategory] ?: emptyList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Functions, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Catálogo de Símbolos Técnicos y Matemáticos", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = insertAsLatex,
                                onClick = { insertAsLatex = !insertAsLatex },
                                label = { Text(if (insertAsLatex) "Modo: LaTeX" else "Modo: Carácter") }
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    selectedSymbol?.let {
                                        onInsertSymbol(it, insertAsLatex)
                                        onDismiss()
                                    }
                                },
                                enabled = selectedSymbol != null
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Insertar")
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                    }
                )

                // Category chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SymbolCategory.values()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label) }
                        )
                    }
                }

                HorizontalDivider()

                // Main Symbols Grid & Preview Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Grid of Symbols
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 64.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        items(currentSymbols) { sym ->
                            val isSelected = selectedSymbol == sym
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = if (isSelected) 4.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable { selectedSymbol = sym }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = sym.char,
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Detail / Preview Panel
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (selectedSymbol != null) {
                                val sym = selectedSymbol!!
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Detalles del Símbolo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            sym.char,
                                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column {
                                        Text("Nombre:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(sym.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }

                                    Column {
                                        Text("Comando LaTeX:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(sym.latex, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        onInsertSymbol(sym, insertAsLatex)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Insertar en Nota")
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Selecciona un símbolo para ver sus detalles e insertarlo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
