package com.nexopp.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LibraryStore(context: Context) {
    private val file = File(context.filesDir, "library_registry.json")

    fun loadSubjects(): List<Subject> {
        if (!file.exists()) return emptyList()
        val json = JSONObject(file.readText())
        val array = json.optJSONArray("subjects") ?: JSONArray()
        val list = mutableListOf<Subject>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(Subject(obj.getString("id"), obj.getString("name"), obj.optInt("order", 0)))
        }
        return list.sortedBy { it.order }
    }

    fun loadNotebooks(): List<Notebook> {
        if (!file.exists()) return emptyList()
        val json = JSONObject(file.readText())
        val array = json.optJSONArray("notebooks") ?: JSONArray()
        val list = mutableListOf<Notebook>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(Notebook(
                obj.getString("id"),
                obj.getString("subjectId"),
                obj.getString("name"),
                obj.getString("fileName"),
                obj.optLong("lastModified", 0L)
            ))
        }
        return list.sortedByDescending { it.lastModified }
    }

    fun save(subjects: List<Subject>, notebooks: List<Notebook>) {
        val json = JSONObject()
        val subArray = JSONArray()
        subjects.forEach { s ->
            subArray.put(JSONObject().put("id", s.id).put("name", s.name).put("order", s.order))
        }
        val notArray = JSONArray()
        notebooks.forEach { n ->
            notArray.put(JSONObject().put("id", n.id).put("subjectId", n.subjectId).put("name", n.name).put("fileName", n.fileName).put("lastModified", n.lastModified))
        }
        json.put("subjects", subArray)
        json.put("notebooks", notArray)
        file.writeText(json.toString())
    }
}