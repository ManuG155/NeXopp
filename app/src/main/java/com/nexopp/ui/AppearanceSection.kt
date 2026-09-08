// --- AppearanceSection.kt ---
package com.nexopp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppearanceSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    OptionGroup(
        title = "Tema",
        subtitle = "Colorea la barra superior, la barra de herramientas y el fondo del lienzo. " +
            "'Sistema' sigue la configuración claro/oscuro del dispositivo.",
        options = ThemeMode.values().toList(),
        selected = settings.themeMode,
        label = { it.label },
        onSelect = { onChange(settings.copy(themeMode = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    SwitchRow(
        title = "Usar colores del sistema",
        subtitle = "En Android 12+ usa los colores del fondo de pantalla. Desactivado usa el morado por defecto de la aplicación.",
        checked = settings.dynamicColor,
        onCheckedChange = { onChange(settings.copy(dynamicColor = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    Text("Posición del contador de páginas", style = MaterialTheme.typography.bodyLarge)
    Text(
        "En qué esquina del lienzo se muestra el indicador \"página X de Y\".",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    DropdownRow(
        label = "Vertical",
        options = PageCounterVertical.values().toList(),
        selected = settings.pageCounterVertical,
        optionLabel = { it.label },
        onSelect = { onChange(settings.copy(pageCounterVertical = it)) },
    )
    DropdownRow(
        label = "Horizontal",
        options = PageCounterHorizontal.values().toList(),
        selected = settings.pageCounterHorizontal,
        optionLabel = { it.label },
        onSelect = { onChange(settings.copy(pageCounterHorizontal = it)) },
    )
}