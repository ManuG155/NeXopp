package com.nexopp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexopp.format.model.Page
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.PageThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerDialog(
    visible: Boolean,
    surface: DrawingSurfaceView?,
    currentPage: Int,
    onDismiss: () -> Unit,
    onGoToPage: (Int) -> Unit,
) {
    if (!visible || surface == null) return

    val pages = surface.doc.pages
    var thumbnails by remember(pages) { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var showTemplateMenuForPage by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pages) {
        withContext(Dispatchers.Default) {
            val map = mutableMapOf<Int, Bitmap>()
            pages.forEachIndexed { i, page ->
                PageThumbnail.render(page, 280)?.let { map[i] = it }
            }
            withContext(Dispatchers.Main) {
                thumbnails = map
            }
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
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cabecera del Gestor de Páginas
                TopAppBar(
                    title = {
                        Column {
                            Text("Gestor de Páginas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("${pages.size} ${if (pages.size == 1) "página" else "páginas"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                surface.addPage()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Añadir página al final")
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Cuadrícula de Páginas
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    itemsIndexed(pages) { index, page ->
                        val isCurrent = index == currentPage
                        val bitmap = thumbnails[index]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isCurrent) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                )
                                .clickable {
                                    onGoToPage(index)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Encabezado de la tarjeta con número de página
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(
                                            "Pág. ${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Menú de opciones de página
                                        Box {
                                            IconButton(
                                                onClick = { showTemplateMenuForPage = index },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Filled.GridOn, contentDescription = "Plantilla", modifier = Modifier.size(18.dp))
                                            }
                                            DropdownMenu(
                                                expanded = showTemplateMenuForPage == index,
                                                onDismissRequest = { showTemplateMenuForPage = null }
                                            ) {
                                                Text(
                                                    "Cambiar plantilla",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                )
                                                val templates = listOf(
                                                    "plain" to "Liso",
                                                    "ruled" to "Pautado con margen",
                                                    "lined" to "Rayado",
                                                    "graph" to "Cuadrícula 5mm",
                                                    "millimeter" to "Papel Milimetrado",
                                                    "isometric" to "Malla Isométrica",
                                                    "polar" to "Gráfico Polar",
                                                    "dotted" to "Punteado"
                                                )
                                                templates.forEach { (style, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        onClick = {
                                                            surface.setPageBackgroundStyle(style)
                                                            showTemplateMenuForPage = null
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                surface.duplicatePage()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicar", modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                surface.removePage()
                                            },
                                            enabled = pages.size > 1,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Eliminar",
                                                tint = if (pages.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Vista previa de la miniatura
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Página ${index + 1}",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Controles de movimiento
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) surface.movePage(index, index - 1)
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowBack, contentDescription = "Mover antes", modifier = Modifier.size(16.dp))
                                    }

                                    TextButton(
                                        onClick = {
                                            surface.addPageBefore()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("Insertar antes", style = MaterialTheme.typography.labelSmall)
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < pages.lastIndex) surface.movePage(index, index + 1)
                                        },
                                        enabled = index < pages.lastIndex,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowForward, contentDescription = "Mover después", modifier = Modifier.size(16.dp))
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
