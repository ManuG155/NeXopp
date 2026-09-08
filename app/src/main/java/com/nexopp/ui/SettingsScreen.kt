// Ruta: app/src/main/java/com/nexopp/ui/SettingsScreen.kt
package com.nexopp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class SettingsSection(val title: String, val summary: String) {
    STYLUS("Stylus", "Dibujo con dedo, previsualización, botón del stylus, sensibilidad de presión."),
    EDITOR("Editor", "Herramienta por defecto y ajuste a cuadrícula o rotaciones de 15°."),
    TOOLBAR("Barra de herramientas", "Qué botones aparecen y en qué orden."),
    PALETTE("Paleta", "Los anillos de la paleta radial — qué hace cada ranura al tocar con el stylus."),
    NAVIGATION("Navegación", "Desplazamiento con inercia y sensibilidad panorámica."),
    APPEARANCE("Apariencia", "Tema y posición del contador de páginas en el lienzo."),
    STORAGE("Almacenamiento", "Límite de importación de texto y caché de PDF."),
    ABOUT("Acerca de", "Versión, licencia, código fuente y formas de apoyar el desarrollo."),
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var section by remember { mutableStateOf<SettingsSection?>(null) }
    val open = section

    BackHandler(enabled = true) { if (section != null) section = null else onBack() }

    if (open == null) {
        SettingsPage(title = "Ajustes", onBack = onBack) {
            SettingsSection.values().forEach { entry ->
                SectionRow(entry) { section = entry }
                HorizontalDivider()
            }
        }
    } else {
        SettingsPage(title = open.title, onBack = { section = null }) {
            when (open) {
                SettingsSection.STYLUS -> StylusSection(settings, onChange)
                SettingsSection.EDITOR -> EditorSection(settings, onChange)
                SettingsSection.TOOLBAR -> ToolbarSection(settings, onChange)
                SettingsSection.PALETTE -> PaletteSection(settings, onChange)
                SettingsSection.NAVIGATION -> NavigationSection(settings, onChange)
                SettingsSection.APPEARANCE -> AppearanceSection(settings, onChange)
                SettingsSection.STORAGE -> StorageSection(settings, onChange)
                SettingsSection.ABOUT -> AboutSection()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(title: String, onBack: () -> Unit, body: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            body()
        }
    }
}

@Composable
private fun SectionRow(section: SettingsSection, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(section.title, style = MaterialTheme.typography.bodyLarge)
            Text(section.summary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}