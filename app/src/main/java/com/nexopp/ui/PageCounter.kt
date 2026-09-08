// --- PageCounter.kt ---
package com.nexopp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun pageLabel(currentPage: Int, pageCount: Int): String =
    "${(currentPage + 1).coerceAtMost(pageCount)} / $pageCount"

fun pageCounterAlignment(
    vertical: PageCounterVertical,
    horizontal: PageCounterHorizontal,
): Alignment = when (vertical) {
    PageCounterVertical.TOP -> when (horizontal) {
        PageCounterHorizontal.LEFT -> Alignment.TopStart
        PageCounterHorizontal.CENTER -> Alignment.TopCenter
        PageCounterHorizontal.RIGHT -> Alignment.TopEnd
    }
    PageCounterVertical.CENTER -> when (horizontal) {
        PageCounterHorizontal.LEFT -> Alignment.CenterStart
        PageCounterHorizontal.CENTER -> Alignment.Center
        PageCounterHorizontal.RIGHT -> Alignment.CenterEnd
    }
    PageCounterVertical.BOTTOM -> when (horizontal) {
        PageCounterHorizontal.LEFT -> Alignment.BottomStart
        PageCounterHorizontal.CENTER -> Alignment.BottomCenter
        PageCounterHorizontal.RIGHT -> Alignment.BottomEnd
    }
}

@Composable
fun PageCounter(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        tonalElevation = 3.dp,
    ) {
        Text(
            text = pageLabel(currentPage, pageCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}