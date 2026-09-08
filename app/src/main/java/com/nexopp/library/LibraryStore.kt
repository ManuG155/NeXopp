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
            save(defaults, emptyList())
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

    fun loadNotebooks(): List<Notebook> {
        if (!file.exists()) return emptyList()
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return emptyList()
        val array = json.optJSONArray("notebooks") ?: JSONArray()
        val list = mutableListOf<Notebook>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
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
                    pageCount = obj.optInt("pageCount", 1)
                )
            )
        }
        return list.sortedByDescending { it.lastModified }
    }

    fun save(subjects: List<Subject>, notebooks: List<Notebook>) {
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
        val notArray = JSONArray()
        notebooks.forEach { n ->
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
            )
        }
        json.put("subjects", subArray)
        json.put("notebooks", notArray)
        file.writeText(json.toString())
    }

    fun addNotebook(notebook: Notebook) {
        val subs = loadSubjects()
        val nbs = loadNotebooks().toMutableList()
        nbs.add(0, notebook)
        save(subs, nbs)
    }

    private val trashFile = File(context.filesDir, "trash_registry.json")
    val trashDir: File = File(context.filesDir, "trash").apply { mkdirs() }

    fun updateNotebook(updated: Notebook) {
        val subs = loadSubjects()
        val nbs = loadNotebooks().map { if (it.id == updated.id) updated else it }
        save(subs, nbs)
    }

    fun toggleFavorite(notebookId: String) {
        val subs = loadSubjects()
        val nbs = loadNotebooks().map { 
            if (it.id == notebookId) it.copy(isFavorite = !it.isFavorite) else it 
        }
        save(subs, nbs)
    }

    fun deleteNotebook(notebook: Notebook) {
        moveToTrash(notebook)
    }

    fun moveToTrash(notebook: Notebook) {
        val subs = loadSubjects()
        val nbs = loadNotebooks().filterNot { it.id == notebook.id }
        save(subs, nbs)

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
            val nb = Notebook(
                id = notObj.getString("id"),
                subjectId = notObj.getString("subjectId"),
                name = notObj.getString("name"),
                fileName = notObj.getString("fileName"),
                coverColor = notObj.optLong("coverColor", 0xFF1E3A8A),
                initialTemplate = notObj.optString("initialTemplate", "ruled"),
                isFavorite = notObj.optBoolean("isFavorite", false),
                lastModified = notObj.optLong("lastModified", System.currentTimeMillis()),
                pageCount = notObj.optInt("pageCount", 1)
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
        save(subs, loadNotebooks())
    }

    fun updateSubject(subject: Subject) {
        val subs = loadSubjects().map { if (it.id == subject.id) subject else it }
        save(subs, loadNotebooks())
    }

    fun deleteSubject(subjectId: String) {
        val subs = loadSubjects().filterNot { it.id == subjectId }
        val nbs = loadNotebooks()
        save(subs, nbs)
    }

    private fun defaultSubjects(): List<Subject> = listOf(
        Subject(name = "Matemáticas", color = 0xFF1E88E5, iconName = "functions", order = 0),
        Subject(name = "Física", color = 0xFF8E24AA, iconName = "science", order = 1),
        Subject(name = "Química", color = 0xFF00897B, iconName = "bubble_chart", order = 2),
        Subject(name = "Informática", color = 0xFF3949AB, iconName = "terminal", order = 3),
        Subject(name = "General", color = 0xFF546E7A, iconName = "folder", order = 4)
    )
}