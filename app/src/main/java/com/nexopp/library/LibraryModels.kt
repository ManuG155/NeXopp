package com.nexopp.library

import java.util.UUID

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Long = 0xFF1976D2, // Default Primary Blue
    val iconName: String = "folder",
    val order: Int = 0,
    val parentId: String? = null // Parent folder ID for nested hierarchies
)

data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Long = 0xFF0288D1 // Light Blue / Teal default
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
    val pageCount: Int = 1,
    val tagIds: List<String> = emptyList(),
    val bookmarkedPages: Set<Int> = emptySet()
)

data class TrashItem(
    val id: String = UUID.randomUUID().toString(),
    val notebook: Notebook,
    val deletedTimestamp: Long = System.currentTimeMillis()
)

enum class NotebookSortOption(val label: String) {
    RECENT_DESC("Modificados recientemente"),
    RECENT_ASC("Más antiguos"),
    NAME_ASC("Nombre (A - Z)"),
    NAME_DESC("Nombre (Z - A)"),
    PAGE_COUNT_DESC("Más páginas"),
    PAGE_COUNT_ASC("Menos páginas")
}

/**
 * Match origin for search results. Extensible for OCR, handwritten text, PDF text, and LaTeX formula matching.
 */
enum class SearchMatchKind(val label: String) {
    NOTEBOOK_NAME("Título"),
    SUBJECT_NAME("Asignatura"),
    TAG_NAME("Etiqueta"),
    CONTENT_TEXT("Texto"),
    HANDWRITING_OCR("Escritura manuscrita"),
    PDF_TEXT("Contenido PDF"),
    FORMULA_LATEX("Fórmula / LaTeX")
}

data class SearchResultItem(
    val notebook: Notebook,
    val subject: Subject?,
    val matchedTags: List<Tag>,
    val matchKind: SearchMatchKind,
    val matchSnippet: String,
    val pageIndex: Int? = null
)

fun Subject.buildPath(allSubjects: List<Subject>): String {
    val chain = mutableListOf(name)
    var curr = this
    while (curr.parentId != null) {
        val parent = allSubjects.firstOrNull { it.id == curr.parentId } ?: break
        chain.add(0, parent.name)
        curr = parent
    }
    return chain.joinToString(" / ")
}

fun Subject.getParentChain(allSubjects: List<Subject>): List<Subject> {
    val chain = mutableListOf(this)
    var curr = this
    val visited = mutableSetOf(id)
    while (curr.parentId != null) {
        val parent = allSubjects.firstOrNull { it.id == curr.parentId } ?: break
        if (!visited.add(parent.id)) break // Guard against cycles
        chain.add(0, parent)
        curr = parent
    }
    return chain
}

fun Subject.getAllDescendantIds(allSubjects: List<Subject>): Set<String> {
    val result = mutableSetOf(id)
    val children = allSubjects.filter { it.parentId == id }
    for (child in children) {
        result.addAll(child.getAllDescendantIds(allSubjects))
    }
    return result
}