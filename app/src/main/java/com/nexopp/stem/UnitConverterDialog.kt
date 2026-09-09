package com.nexopp.stem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.util.Locale
import kotlin.math.abs


enum class UnitCategory(val label: String) {

    LENGTH("Longitud"),
    MASS("Masa"),
    PRESSURE("Presión"),
    FORCE("Fuerza"),
    ENERGY("Energía"),
    POWER("Potencia"),
    TEMPERATURE("Temperatura"),
    ANGLE("Ángulo"),
    DATA("Datos")
}

data class UnitDefinition(
    val id: String,
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

object UnitRegistry {
    val categories: Map<UnitCategory, List<UnitDefinition>> = mapOf(
        UnitCategory.LENGTH to listOf(
            linearUnit("m", "Metro", "m", 1.0),
            linearUnit("cm", "Centímetro", "cm", 0.01),
            linearUnit("mm", "Milímetro", "mm", 0.001),
            linearUnit("um", "Micrómetro", "µm", 1e-6),
            linearUnit("nm", "Nanómetro", "nm", 1e-9),
            linearUnit("km", "Kilómetro", "km", 1000.0),
            linearUnit("in", "Pulgada", "in", 0.0254),
            linearUnit("ft", "Pie", "ft", 0.3048),
            linearUnit("yd", "Yarda", "yd", 0.9144),
            linearUnit("mi", "Milla", "mi", 1609.344),
            linearUnit("nmi", "Milla náutica", "nmi", 1852.0)
        ),
        UnitCategory.MASS to listOf(
            linearUnit("kg", "Kilogramo", "kg", 1.0),
            linearUnit("g", "Gramo", "g", 0.001),
            linearUnit("mg", "Miligramo", "mg", 1e-6),
            linearUnit("t", "Tonelada métrica", "t", 1000.0),
            linearUnit("lb", "Libra", "lb", 0.45359237),
            linearUnit("oz", "Onza", "oz", 0.028349523125),
            linearUnit("slug", "Slug", "slug", 14.593903)
        ),
        UnitCategory.PRESSURE to listOf(
            linearUnit("pa", "Pascal", "Pa", 1.0),
            linearUnit("kpa", "Kilopascal", "kPa", 1000.0),
            linearUnit("mpa", "Megapascal", "MPa", 1e6),
            linearUnit("bar", "Bar", "bar", 100000.0),
            linearUnit("mbar", "Milibar", "mbar", 100.0),
            linearUnit("atm", "Atmósfera estándar", "atm", 101325.0),
            linearUnit("psi", "PSI (lbf/in²)", "psi", 6894.757),
            linearUnit("torr", "Torr / mmHg", "Torr", 133.322)
        ),
        UnitCategory.FORCE to listOf(
            linearUnit("n", "Newton", "N", 1.0),
            linearUnit("kn", "Kilonewton", "kN", 1000.0),
            linearUnit("lbf", "Libra-fuerza", "lbf", 4.448222),
            linearUnit("kgf", "Kilogramo-fuerza", "kgf", 9.80665),
            linearUnit("dyn", "Dina", "dyn", 1e-5)
        ),
        UnitCategory.ENERGY to listOf(
            linearUnit("j", "Julio", "J", 1.0),
            linearUnit("kj", "Kilojulio", "kJ", 1000.0),
            linearUnit("mj", "Megajulio", "MJ", 1e6),
            linearUnit("cal", "Caloría", "cal", 4.184),
            linearUnit("kcal", "Kilocaloría", "kcal", 4184.0),
            linearUnit("wh", "Vatio-hora", "Wh", 3600.0),
            linearUnit("kwh", "Kilovatio-hora", "kWh", 3.6e6),
            linearUnit("ev", "Electronvoltio", "eV", 1.602176634e-19),
            linearUnit("btu", "BTU", "BTU", 1055.06)
        ),
        UnitCategory.POWER to listOf(
            linearUnit("w", "Vatio", "W", 1.0),
            linearUnit("kw", "Kilovatio", "kW", 1000.0),
            linearUnit("mw", "Megavatio", "MW", 1e6),
            linearUnit("hp", "Caballo de fuerza (HP)", "hp", 745.69987),
            linearUnit("cv", "Caballo de vapor (CV)", "CV", 735.49875)
        ),
        UnitCategory.TEMPERATURE to listOf(
            UnitDefinition("c", "Celsius", "°C", { it + 273.15 }, { it - 273.15 }),
            UnitDefinition("f", "Fahrenheit", "°F", { (it - 32.0) * 5.0 / 9.0 + 273.15 }, { (it - 273.15) * 9.0 / 5.0 + 32.0 }),
            UnitDefinition("k", "Kelvin", "K", { it }, { it }),
            UnitDefinition("r", "Rankine", "°R", { it * 5.0 / 9.0 }, { it * 9.0 / 5.0 })
        ),
        UnitCategory.ANGLE to listOf(
            linearUnit("rad", "Radián", "rad", 1.0),
            linearUnit("deg", "Grado sexagesimal", "°", Math.PI / 180.0),
            linearUnit("grad", "Gradian / Grado centesimal", "grad", Math.PI / 200.0),
            linearUnit("arcmin", "Minuto de arco", "'", Math.PI / 10800.0),
            linearUnit("arcsec", "Segundo de arco", "''", Math.PI / 648000.0)
        ),
        UnitCategory.DATA to listOf(
            linearUnit("b", "Byte", "B", 1.0),
            linearUnit("kb", "Kilobyte (decimal)", "KB", 1000.0),
            linearUnit("mb", "Megabyte (decimal)", "MB", 1e6),
            linearUnit("gb", "Gigabyte (decimal)", "GB", 1e9),
            linearUnit("tb", "Terabyte (decimal)", "TB", 1e12),
            linearUnit("kib", "Kibibyte (binario)", "KiB", 1024.0),
            linearUnit("mib", "Mebibyte (binario)", "MiB", 1048576.0),
            linearUnit("gib", "Gibibyte (binario)", "GiB", 1073741824.0),
            linearUnit("tib", "Tebibyte (binario)", "TiB", 1099511627776.0)
        )
    )

    private fun linearUnit(id: String, name: String, symbol: String, factorToBase: Double): UnitDefinition {
        return UnitDefinition(id, name, symbol, { it * factorToBase }, { it / factorToBase })
    }

    fun convert(value: Double, from: UnitDefinition, to: UnitDefinition): Double {
        val baseVal = from.toBase(value)
        return to.fromBase(baseVal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterDialog(
    onDismiss: () -> Unit,
    onInsertText: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(UnitCategory.LENGTH) }
    val unitsForCategory = remember(selectedCategory) { UnitRegistry.categories[selectedCategory] ?: emptyList() }

    var fromUnit by remember(selectedCategory) { mutableStateOf(unitsForCategory.firstOrNull() ?: UnitRegistry.categories[UnitCategory.LENGTH]!!.first()) }
    var toUnit by remember(selectedCategory) { mutableStateOf(unitsForCategory.getOrNull(1) ?: unitsForCategory.firstOrNull() ?: UnitRegistry.categories[UnitCategory.LENGTH]!!.first()) }

    var inputValueStr by remember { mutableStateOf("1") }

    val convertedValue = remember(inputValueStr, fromUnit, toUnit) {
        val input = inputValueStr.toDoubleOrNull()
        if (input != null) {
            UnitRegistry.convert(input, fromUnit, toUnit)
        } else {
            Double.NaN
        }
    }

    val formattedOutput = remember(convertedValue) {
        if (convertedValue.isNaN() || convertedValue.isInfinite()) {
            "---"
        } else {
            if (abs(convertedValue) < 1e-4 || abs(convertedValue) >= 1e7) {
                String.format(Locale.US, "%.6e", convertedValue)
            } else {
                String.format(Locale.US, "%.6f", convertedValue).trimEnd('0').trimEnd('.')
            }
        }
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
                            Icon(Icons.Filled.Transform, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Conversor de Unidades para Ingeniería", fontWeight = FontWeight.Bold)
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
                                if (formattedOutput != "---") {
                                    val text = "$inputValueStr ${fromUnit.symbol} = $formattedOutput ${toUnit.symbol}"
                                    onInsertText(text)
                                    onDismiss()
                                }
                            },
                            enabled = formattedOutput != "---"
                        ) {
                            Icon(Icons.Filled.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Insertar en Nota")
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                )

                // Category selection row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(UnitCategory.values()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label) }
                        )
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Interactive Conversion Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // From row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = inputValueStr,
                                    onValueChange = { inputValueStr = it },
                                    label = { Text("Valor") },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.weight(1f)
                                )

                                var fromExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { fromExpanded = true },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("${fromUnit.name} (${fromUnit.symbol})", maxLines = 1)
                                    }
                                    DropdownMenu(
                                        expanded = fromExpanded,
                                        onDismissRequest = { fromExpanded = false }
                                    ) {
                                        unitsForCategory.forEach { u ->
                                            DropdownMenuItem(
                                                text = { Text("${u.name} (${u.symbol})") },
                                                onClick = {
                                                    fromUnit = u
                                                    fromExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Swap button
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = {
                                        val temp = fromUnit
                                        fromUnit = toUnit
                                        toUnit = temp
                                    }
                                ) {
                                    Icon(Icons.Filled.SwapVert, contentDescription = "Intercambiar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                            }

                            // To row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = formattedOutput,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                var toExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { toExpanded = true },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("${toUnit.name} (${toUnit.symbol})", maxLines = 1)
                                    }
                                    DropdownMenu(
                                        expanded = toExpanded,
                                        onDismissRequest = { toExpanded = false }
                                    ) {
                                        unitsForCategory.forEach { u ->
                                            DropdownMenuItem(
                                                text = { Text("${u.name} (${u.symbol})") },
                                                onClick = {
                                                    toUnit = u
                                                    toExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick conversion equivalence table
                    Text("Equivalencias en ${selectedCategory.label}:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val inputNum = inputValueStr.toDoubleOrNull() ?: 1.0
                        unitsForCategory.forEach { u ->
                            val equiv = UnitRegistry.convert(inputNum, fromUnit, u)
                            val eqStr = if (abs(equiv) < 1e-4 || abs(equiv) >= 1e7) {
                                String.format(Locale.US, "%.4e", equiv)
                            } else {
                                String.format(Locale.US, "%.4f", equiv).trimEnd('0').trimEnd('.')
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { toUnit = u }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(u.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "$eqStr ${u.symbol}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
