// Ruta: app/src/main/java/com/nexopp/ui/TabOverviewPopup.kt
package com.nexopp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexopp.render.PageThumbnail

private const val THUMB_WIDTH_PX = 240

@Composable
internal fun TabOverviewButton(tabs: TabsUiState) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { tabs.onOverview(); open = true }) {
        Icon(Icons.Filled.GridView, contentDescription = "Vista general de pestañas")
    }
    if (!open) return
    val thumbs = remember(tabs.titles.size) { mutableStateListOf<Bitmap?>().apply { repeat(tabs.titles.size) { add(null) } } }
    LaunchedEffect(thumbs) {
        tabs.titles.indices.forEach { i ->
            tabs.preview(i, THUMB_WIDTH_PX) { bitmap -> if (i < thumbs.size) thumbs[i] = bitmap }
        }
    }
    AlertDialog(
        onDismissRequest = { open = false },
        confirmButton = { TextButton(onClick = { open = false }) { Text("Cerrar") } },
        title = { Text("Pestañas abiertas") },
        text = {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 120.dp)) {
                itemsIndexed(tabs.titles) { index, title ->
                    TabOverviewCell(
                        title = title,
                        thumbnail = thumbs.getOrNull(index),
                        active = index == tabs.activeIndex,
                        onClick = { open = false; tabs.onSelect(index) },
                    )
                }
            }
        },
    )
}

@Composable
private fun TabOverviewCell(
    title: String,
    thumbnail: Bitmap?,
    active: Boolean,
    onClick: () -> Unit,
) {
    val outline =
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier.padding(4.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.77f) 
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(if (active) 2.dp else 1.dp, outline, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}