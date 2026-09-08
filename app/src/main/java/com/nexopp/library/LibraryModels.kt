package com.nexopp.library

import java.util.UUID

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Long = 0xFF1976D2, // Default Primary Blue
    val iconName: String = "folder",
    val order: Int = 0
)

data class Notebook(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val name: String,
    val fileName: String,
    val coverColor: Long = 0xFF1E3A8A, // Indigo/Navy
    val initialTemplate: String = "ruled", // "ruled", "graph", "dotted", "plain", "millimeter", "isometric", "polar"
    val isFavorite: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val pageCount: Int = 1
)

data class TrashItem(
    val id: String = UUID.randomUUID().toString(),
    val notebook: Notebook,
    val deletedTimestamp: Long = System.currentTimeMillis()
)