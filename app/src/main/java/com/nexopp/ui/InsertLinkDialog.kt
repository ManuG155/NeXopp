// Ruta: app/src/main/java/com/nexopp/ui/InsertLinkDialog.kt
package com.nexopp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun InsertLinkDialog(
    onDismiss: () -> Unit,
    onInsertLink: (url: String, label: String, asCard: Boolean) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var asCard by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Insertar Enlace o Multimedia",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                HorizontalDivider()

                // URL Input
                OutlinedTextField(
                    value = url,
                    onValueChange = { 
                        url = it 
                        if (label.isBlank() && it.isNotBlank()) {
                            val host = runCatching { android.net.Uri.parse(it).host }.getOrNull()
                            if (!host.isNullOrBlank()) label = host
                        }
                    },
                    label = { Text("URL del enlace (https://...)") },
                    placeholder = { Text("https://ejemplo.com o https://youtube.com/...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.OpenInBrowser, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Label Input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Texto visible / Título del enlace") },
                    placeholder = { Text("Ej. Vídeo de explicación / Referencia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Option: Render as rich card
                AppSwitchRow(
                    label = "Mostrar como tarjeta interactiva con vista previa",
                    description = "Incluye icono multimedia y soporte de reproducción / apertura directa",
                    checked = asCard,
                    onCheckedChange = { asCard = it }
                )

                Spacer(Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val cleanUrl = url.trim()
                            if (cleanUrl.isNotBlank()) {
                                val fullUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                                    "https://$cleanUrl"
                                } else cleanUrl
                                val effectiveLabel = label.ifBlank { fullUrl }
                                onInsertLink(fullUrl, effectiveLabel, asCard)
                                onDismiss()
                            }
                        },
                        enabled = url.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Insertar en Página")
                    }
                }
            }
        }
    }
}
