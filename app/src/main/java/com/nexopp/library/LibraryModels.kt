package com.nexopp.library

import java.util.UUID

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val order: Int = 0
)

data class Notebook(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val name: String,
    val fileName: String, 
    val lastModified: Long = System.currentTimeMillis()
)