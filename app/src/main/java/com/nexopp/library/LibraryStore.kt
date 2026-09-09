package com.nexopp.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LibraryStore(private val context: Context) {
    private val file = File(context.filesDir, "library_registry.json")
    val notebooksDir: File = File(context.filesDir, "notebooks").apply { mkdirs() }

    fun loadSubjects(): List<Subject> {
        if (!file.exists()) {
            val defaults = defaultSubjects()
            save(defaults, emptyList(), defaultTags())
            return defaults
        }
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return defaultSubjects()
        val array = json.optJSONArray("subjects") ?: JSONArray()
        val list = mutableListOf<Subject>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Subject(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    color = obj.optLong("color", 0xFF1976D2),
                    iconName = obj.optString("iconName", "folder"),
                    order = obj.optInt("order", i)
                )
            )
        }
        return if (list.isEmpty()) defaultSubjects() else list.sortedBy { it.order }
    }

    fun loadTags(): List<Tag> {
        if (!file.exists()) return defaultTags()
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return defaultTags()
        val array = json.optJSONArray("tags") ?: return defaultTags()
        val list = mutableListOf<Tag>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Tag(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    color = obj.optLong("color", 0xFF0288D1)
                )
            )
        }
        return if (list.isEmpty()) defaultTags() else list
    }

    fun loadNotebooks(): List<Notebook> {
        if (!file.exists()) return emptyList()
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return emptyList()
        val array = json.optJSONArray("notebooks") ?: JSONArray()
        val list = mutableListOf<Notebook>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            
            val tagArray = obj.optJSONArray("tagIds")
            val tags = mutableListOf<String>()
            if (tagArray != null) {
                for (t in 0 until tagArray.length()) {
                    tags.add(tagArray.getString(t))
                }
            }

            val bookmarkArray = obj.optJSONArray("bookmarkedPages")
            val bookmarks = mutableSetOf<Int>()
            if (bookmarkArray != null) {
                for (b in 0 until bookmarkArray.length()) {
                    bookmarks.add(bookmarkArray.getInt(b))
                }
            }

            list.add(
                Notebook(
                    id = obj.getString("id"),
                    subjectId = obj.getString("subjectId"),
                    name = obj.getString("name"),
                    fileName = obj.getString("fileName"),
                    coverColor = obj.optLong("coverColor", 0xFF1E3A8A),
                    initialTemplate = obj.optString("initialTemplate", "ruled"),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    lastModified = obj.optLong("lastModified", System.currentTimeMillis()),
                    pageCount = obj.optInt("pageCount", 1),
                    tagIds = tags,
                    bookmarkedPages = bookmarks
                )
            )
        }
        return list.sortedByDescending { it.lastModified }
    }

    fun save(
        subjects: List<Subject> = loadSubjects(),
        notebooks: List<Notebook> = loadNotebooks(),
        tags: List<Tag> = loadTags()
    ) {
        val json = JSONObject()
        
        val subArray = JSONArray()
        subjects.forEach { s ->
            subArray.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("color", s.color)
                    .put("iconName", s.iconName)
                    .put("order", s.order)
            )
        }

        val tagArray = JSONArray()
        tags.forEach { t ->
            tagArray.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("color", t.color)
            )
        }

        val notArray = JSONArray()
        notebooks.forEach { n ->
            val nbTagArray = JSONArray()
            n.tagIds.forEach { nbTagArray.put(it) }

            val nbBookmarkArray = JSONArray()
            n.bookmarkedPages.forEach { nbBookmarkArray.put(it) }

            notArray.put(
                JSONObject()
                    .put("id", n.id)
                    .put("subjectId", n.subjectId)
                    .put("name", n.name)
                    .put("fileName", n.fileName)
                    .put("coverColor", n.coverColor)
                    .put("initialTemplate", n.initialTemplate)
                    .put("isFavorite", n.isFavorite)
                    .put("lastModified", n.lastModified)
                    .put("pageCount", n.pageCount)
                    .put("tagIds", nbTagArray)
                    .put("bookmarkedPages", nbBookmarkArray)
            )
        }
        
        json.put("subjects", subArray)
        json.put("tags", tagArray)
        json.put("notebooks", notArray)
        file.writeText(json.toString())
    }

    fun addNotebook(notebook: Notebook) {
        val subs = loadSubjects()
        val tags = loadTags()
        val nbs = loadNotebooks().toMutableList()
        nbs.add(0, notebook)
        save(subs, nbs, tags)
    }

    private val trashFile = File(context.filesDir, "trash_registry.json")
    val trashDir: File = File(context.filesDir, "trash").apply { mkdirs() }

    fun updateNotebook(updated: Notebook) {
        val subs = loadSubjects()
        val tags = loadTags()
        val nbs = loadNotebooks().map { if (it.id == updated.id) updated else it }
        save(subs, nbs, tags)
    }

    fun setNotebookTags(notebookId: String, tagIds: List<String>) {
        val subs = loadSubjects()
        val tags = loadTags()
        val nbs = loadNotebooks().map { 
            if (it.id == notebookId) it.copy(tagIds = tagIds) else it 
        }
        save(subs, nbs, tags)
    }

    fun setNotebookSubject(notebookId: String, newSubjectId: String) {
        val subs = loadSubjects()
        val tags = loadTags()
        val nbs = loadNotebooks().map { 
            if (it.id == notebookId) it.copy(subjectId = newSubjectId) else it 
        }
        save(subs, nbs, tags)
    }

    fun togglePageBookmark(notebookId: String, pageIndex: Int) {
        val subs = loadSubjects()
        val tags = loadTags()
        val nbs = loadNotebooks().map { nb ->
            if (nb.id == notebookId) {
                val newBookmarks = if (nb.bookmarkedPages.contains(pageIndex)) {
                    nb.bookmarkedPages - pageIndex
                } else {
                    nb.bookmarkedPages + pageIndex
                }
                nb.copy(bookmarkedPages = newBookmarks)
            } else {
                nb
            }
        }
        save(subs, nbs, tags)
    }

    fun toggleFavorite(notebookId: String) {
        val subs = loadSubjects()
        val tags = loadTags()
        val nbs = loadNotebooks().map { 
            if (it.id == notebookId) it.copy(isFavorite = !it.isFavorite) else it 
        }
        save(subs, nbs, tags)
    }

    fun deleteNotebook(notebook: Notebook) {
        moveToTrash(notebook)
    }

    fun moveToTrash(notebook: Notebook) {
        val subs = loadSubjects()
        val tags = loadTags()
        val nbs = loadNotebooks().filterNot { it.id == notebook.id }
        save(subs, nbs, tags)

        val origFile = File(notebooksDir, notebook.fileName)
        if (origFile.exists()) {
            val destFile = File(trashDir, notebook.fileName)
            origFile.copyTo(destFile, overwrite = true)
            origFile.delete()
        }

        val trash = loadTrash().toMutableList()
        trash.add(0, TrashItem(notebook = notebook, deletedTimestamp = System.currentTimeMillis()))
        saveTrash(trash)
    }

    fun loadTrash(): List<TrashItem> {
        if (!trashFile.exists()) return emptyList()
        val json = runCatching { JSONObject(trashFile.readText()) }.getOrNull() ?: return emptyList()
        val array = json.optJSONArray("trash") ?: JSONArray()
        val list = mutableListOf<TrashItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val notObj = obj.getJSONObject("notebook")
            
            val tagArray = notObj.optJSONArray("tagIds")
            val tags = mutableListOf<String>()
            if (tagArray != null) {
                for (t in 0 until tagArray.length()) {
                    tags.add(tagArray.getString(t))
                }
            }

            val nb = Notebook(
                id = notObj.getString("id"),
                subjectId = notObj.getString("subjectId"),
                name = notObj.getString("name"),
                fileName = notObj.getString("fileName"),
                coverColor = notObj.optLong("coverColor", 0xFF1E3A8A),
                initialTemplate = notObj.optString("initialTemplate", "ruled"),
                isFavorite = notObj.optBoolean("isFavorite", false),
                lastModified = notObj.optLong("lastModified", System.currentTimeMillis()),
                pageCount = notObj.optInt("pageCount", 1),
                tagIds = tags
            )
            list.add(
                TrashItem(
                    id = obj.getString("id"),
                    notebook = nb,
                    deletedTimestamp = obj.optLong("deletedTimestamp", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    private fun saveTrash(trash: List<TrashItem>) {
        val json = JSONObject()
        val array = JSONArray()
        trash.forEach { item ->
            val nbTagArray = JSONArray()
            item.notebook.tagIds.forEach { nbTagArray.put(it) }

            val notObj = JSONObject()
                .put("id", item.notebook.id)
                .put("subjectId", item.notebook.subjectId)
                .put("name", item.notebook.name)
                .put("fileName", item.notebook.fileName)
                .put("coverColor", item.notebook.coverColor)
                .put("initialTemplate", item.notebook.initialTemplate)
                .put("isFavorite", item.notebook.isFavorite)
                .put("lastModified", item.notebook.lastModified)
                .put("pageCount", item.notebook.pageCount)
                .put("tagIds", nbTagArray)

            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("notebook", notObj)
                    .put("deletedTimestamp", item.deletedTimestamp)
            )
        }
        json.put("trash", array)
        trashFile.writeText(json.toString())
    }

    fun restoreFromTrash(trashId: String): Notebook? {
        val trash = loadTrash().toMutableList()
        val item = trash.firstOrNull { it.id == trashId } ?: return null
        trash.removeAll { it.id == trashId }
        saveTrash(trash)

        val srcFile = File(trashDir, item.notebook.fileName)
        if (srcFile.exists()) {
            val destFile = File(notebooksDir, item.notebook.fileName)
            srcFile.copyTo(destFile, overwrite = true)
            srcFile.delete()
        }

        addNotebook(item.notebook)
        return item.notebook
    }

    fun permanentlyDelete(trashId: String) {
        val trash = loadTrash().toMutableList()
        val item = trash.firstOrNull { it.id == trashId } ?: return
        trash.removeAll { it.id == trashId }
        saveTrash(trash)

        val srcFile = File(trashDir, item.notebook.fileName)
        if (srcFile.exists()) {
            srcFile.delete()
        }
    }

    fun emptyTrash() {
        saveTrash(emptyList())
        runCatching {
            trashDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun duplicateNotebook(notebook: Notebook): Notebook {
        val newFileName = "${java.util.UUID.randomUUID()}.xopp"
        val origFile = File(notebooksDir, notebook.fileName)
        if (origFile.exists()) {
            origFile.copyTo(File(notebooksDir, newFileName), overwrite = true)
        }
        val dup = notebook.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${notebook.name} (Copia)",
            fileName = newFileName,
            lastModified = System.currentTimeMillis()
        )
        addNotebook(dup)
        return dup
    }

    fun addSubject(subject: Subject) {
        val subs = loadSubjects().toMutableList()
        subs.add(subject)
        save(subs, loadNotebooks(), loadTags())
    }

    fun updateSubject(subject: Subject) {
        val subs = loadSubjects().map { if (it.id == subject.id) subject else it }
        save(subs, loadNotebooks(), loadTags())
    }

    fun deleteSubject(subjectId: String) {
        val subs = loadSubjects().filterNot { it.id == subjectId }
        val nbs = loadNotebooks()
        save(subs, nbs, loadTags())
    }

    fun addTag(tag: Tag) {
        val tags = loadTags().toMutableList()
        tags.add(tag)
        save(loadSubjects(), loadNotebooks(), tags)
    }

    fun updateTag(tag: Tag) {
        val tags = loadTags().map { if (it.id == tag.id) tag else it }
        save(loadSubjects(), loadNotebooks(), tags)
    }

    fun deleteTag(tagId: String) {
        val tags = loadTags().filterNot { it.id == tagId }
        val nbs = loadNotebooks().map { nb ->
            if (nb.tagIds.contains(tagId)) nb.copy(tagIds = nb.tagIds - tagId) else nb
        }
        save(loadSubjects(), nbs, tags)
    }

    private fun defaultSubjects(): List<Subject> = listOf(
        Subject(name = "Matemáticas", color = 0xFF1E88E5, iconName = "functions", order = 0),
        Subject(name = "Física", color = 0xFF8E24AA, iconName = "science", order = 1),
        Subject(name = "Química", color = 0xFF00897B, iconName = "bubble_chart", order = 2),
        Subject(name = "Informática", color = 0xFF3949AB, iconName = "terminal", order = 3),
        Subject(name = "General", color = 0xFF546E7A, iconName = "folder", order = 4)
    )

    private fun defaultTags(): List<Tag> = listOf(
        Tag(name = "Exámenes", color = 0xFFE53935),
        Tag(name = "Prácticas", color = 0xFF43A047),
        Tag(name = "Teoría", color = 0xFF1E88E5),
        Tag(name = "Fórmulas", color = 0xFFFB8C00),
        Tag(name = "Importante", color = 0xFF8E24AA)
    )
}