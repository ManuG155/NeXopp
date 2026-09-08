// Ruta: app/src/main/java/com/nexopp/ui/StorageSection.kt
package com.nexopp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StorageSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    OptionGroup(
        title = "Límite de importación de texto",
        subtitle = "Abrir un archivo de texto grande toma minutos y genera miles de páginas. Archivos mayores a este tamaño se rechazarán con un mensaje.",
        options = AppSettings.TEXT_IMPORT_LIMIT_CHOICES,
        selected = settings.textImportLimitMb,
        label = { "$it MB" },
        onSelect = { onChange(settings.copy(textImportLimitMb = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    OptionGroup(
        title = "Límite de caché de PDF",
        subtitle = "Los fondos generados e importados se guardan para abrir rápido. Superado el límite, se borran los más antiguos sin uso; se regenerarán al volver a abrir el archivo.",
        options = AppSettings.PDF_CACHE_LIMIT_CHOICES,
        selected = settings.pdfCacheLimitMb,
        label = { "$it MB" },
        onSelect = { onChange(settings.copy(pdfCacheLimitMb = it)) },
    )
}