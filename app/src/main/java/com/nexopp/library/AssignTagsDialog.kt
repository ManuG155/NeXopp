package com.nexopp.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AssignTagsDialog(
    notebook: Notebook,
    availableTags: List<Tag>,
    onDismiss: () -> Unit,
    onSaveTags: (List<String>) -> Unit,
    onCreateNewTagRequest: () -> Unit
) {
    var selectedTagIds by remember(notebook) { mutableStateOf(notebook.tagIds.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Etiquetas del Cuaderno", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onCreateNewTagRequest) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva etiqueta", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                Text(
                    "Selecciona las etiquetas para '${notebook.name}':",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (availableTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay etiquetas creadas.", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableTags, key = { it.id }) { tag ->
                            val isSelected = selectedTagIds.contains(tag.id)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                tonalElevation = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTagIds = if (isSelected) {
                                            selectedTagIds - tag.id
                                        } else {
                                            selectedTagIds + tag.id
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(tag.color))
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(tag.name, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                    }

                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedTagIds = if (checked) selectedTagIds + tag.id else selectedTagIds - tag.id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveTags(selectedTagIds.toList())
                onDismiss()
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun MoveNotebookDialog(
    notebook: Notebook,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onMove: (newSubjectId: String) -> Unit
) {
    var selectedSubjectId by remember(notebook) { mutableStateOf(notebook.subjectId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mover a Asignatura", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Selecciona la nueva asignatura para '${notebook.name}':",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        val isSelected = selectedSubjectId == subject.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubjectId = subject.id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(subject.color))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    val path = buildSubjectPath(subject, subjects)
                                    Text(path, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }

                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onMove(selectedSubjectId)
                    onDismiss()
                },
                enabled = selectedSubjectId != notebook.subjectId
            ) {
                Text("Mover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

fun buildSubjectPath(subject: Subject, allSubjects: List<Subject>): String {
    val names = mutableListOf<String>()
    var cur: Subject? = subject
    val visited = mutableSetOf<String>()
    while (cur != null && cur.id !in visited) {
        visited.add(cur.id)
        names.add(0, cur.name)
        val pId = cur.parentId
        cur = if (pId != null) allSubjects.firstOrNull { it.id == pId } else null
    }
    return names.joinToString(" / ")
}
