// Ruta: app/src/main/java/com/nexopp/ui/InsertLinkDialog.kt
package com.nexopp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable
fun InsertLinkDialog(
    onDismiss: () -> Unit,
    onInsertLink: (url: String, label: String, asCard: Boolean) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var asCard by remember { mutableStateOf(true) }

    var preview by remember { mutableStateOf<LinkMetadata?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        val trimmed = url.trim()
        if (trimmed.length > 7 && (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("."))) {
            delay(400)
            isLoadingPreview = true
            val meta = LinkPreviewFetcher.fetchPreview(trimmed)
            preview = meta
            isLoadingPreview = false
            if (label.isBlank() && !meta?.title.isNullOrBlank()) {
                label = meta?.title ?: ""
            }
        } else {
            preview = null
            isLoadingPreview = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(TabletDimensions.DialogCornerRadius),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(TabletDimensions.DialogMaxWidthFraction)
                .wrapContentHeight()
                .padding(TabletDimensions.DialogPadding)
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
                    Column {
                        Text(
                            "Insertar Enlace o Multimedia",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Compatible con vídeos de YouTube, enlaces web y referencias",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    placeholder = { Text("https://ejemplo.com o https://youtube.com/watch?v=...") },
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

                // Live Preview Card
                if (isLoadingPreview) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Obteniendo vista previa del enlace…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (preview != null) {
                    val p = preview!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (p.mediaType == LinkMediaType.VIDEO) Color(0xFFFFEBEE)
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (p.mediaType == LinkMediaType.VIDEO) Icons.Filled.PlayCircle
                                    else Icons.Filled.Article,
                                    contentDescription = null,
                                    tint = if (p.mediaType == LinkMediaType.VIDEO) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        p.siteName ?: "Web",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("Vista previa lista", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Text(
                                    p.title ?: p.url,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!p.description.isNullOrBlank()) {
                                    Text(
                                        p.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

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
                                val effectiveLabel = label.ifBlank { preview?.title ?: fullUrl }
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
