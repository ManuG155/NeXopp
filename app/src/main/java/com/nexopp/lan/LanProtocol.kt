package com.nexopp.lan

import com.nexopp.format.model.*
import com.nexopp.library.Notebook
import com.nexopp.library.Subject
import com.nexopp.library.Tag
import org.json.JSONArray
import org.json.JSONObject

/**
 * Clean, structured JSON protocol for NeXopp LAN Web Client and bidirectional synchronization.
 */
object LanProtocol {

    // --- Message Types ---
    const val TYPE_GET_LIBRARY = "GET_LIBRARY"
    const val TYPE_LIBRARY_DATA = "LIBRARY_DATA"
    const val TYPE_CREATE_SUBJECT = "CREATE_SUBJECT"
    const val TYPE_CREATE_NOTEBOOK = "CREATE_NOTEBOOK"
    const val TYPE_RENAME_NOTEBOOK = "RENAME_NOTEBOOK"
    const val TYPE_DELETE_NOTEBOOK = "DELETE_NOTEBOOK"

    const val TYPE_OPEN_DOCUMENT = "OPEN_DOCUMENT"
    const val TYPE_DOCUMENT_DATA = "DOCUMENT_DATA"
    const val TYPE_ADD_STROKE = "ADD_STROKE"
    const val TYPE_STROKE_ADDED = "STROKE_ADDED"
    const val TYPE_ADD_PAGE = "ADD_PAGE"
    const val TYPE_PAGE_ADDED = "PAGE_ADDED"
    const val TYPE_TABLET_DOCUMENT_CHANGED = "TABLET_DOCUMENT_CHANGED"
    const val TYPE_SAVE_DOCUMENT = "SAVE_DOCUMENT"
    const val TYPE_DOCUMENT_SAVED = "DOCUMENT_SAVED"

    const val TYPE_STATUS = "STATUS"
    const val TYPE_ERROR = "ERROR"

    // --- Serialization Helpers ---

    fun libraryToJson(subjects: List<Subject>, notebooks: List<Notebook>, tags: List<Tag>): JSONObject {
        val root = JSONObject()
        root.put("type", TYPE_LIBRARY_DATA)

        val subArray = JSONArray()
        subjects.forEach { s ->
            subArray.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("color", s.color)
                put("iconName", s.iconName)
                put("order", s.order)
                if (s.parentId != null) put("parentId", s.parentId)
            })
        }
        root.put("subjects", subArray)

        val nbArray = JSONArray()
        notebooks.forEach { nb ->
            nbArray.put(JSONObject().apply {
                put("id", nb.id)
                put("subjectId", nb.subjectId)
                put("name", nb.name)
                put("fileName", nb.fileName)
                put("coverColor", nb.coverColor)
                put("initialTemplate", nb.initialTemplate)
                put("isFavorite", nb.isFavorite)
                put("lastModified", nb.lastModified)
                put("pageCount", nb.pageCount)
                val tagIdsArray = JSONArray()
                nb.tagIds.forEach { tagIdsArray.put(it) }
                put("tagIds", tagIdsArray)
            })
        }
        root.put("notebooks", nbArray)

        val tagArray = JSONArray()
        tags.forEach { t ->
            tagArray.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("color", t.color)
            })
        }
        root.put("tags", tagArray)

        return root
    }

    fun documentToJson(
        notebookId: String,
        fileName: String,
        title: String,
        document: Document,
        version: Long
    ): JSONObject {
        val root = JSONObject()
        root.put("type", TYPE_DOCUMENT_DATA)
        root.put("notebookId", notebookId)
        root.put("fileName", fileName)
        root.put("title", title)
        root.put("version", version)

        val pagesArray = JSONArray()
        document.pages.forEachIndexed { pageIdx, page ->
            val pageObj = JSONObject()
            pageObj.put("index", pageIdx)
            pageObj.put("width", page.width)
            pageObj.put("height", page.height)

            // Background
            val bgObj = JSONObject()
            when (val bg = page.background) {
                is Background.Solid -> {
                    bgObj.put("type", "solid")
                    bgObj.put("color", bg.color)
                    bgObj.put("style", bg.style)
                }
                is Background.Pdf -> {
                    bgObj.put("type", "pdf")
                    bgObj.put("page", bg.pageNo)
                }
                is Background.Pixmap -> {
                    bgObj.put("type", "pixmap")
                    bgObj.put("file", bg.filename)
                }
            }
            pageObj.put("background", bgObj)

            // Layers and Elements
            val layersArray = JSONArray()
            page.layers.forEach { layer ->
                val layerObj = JSONObject()
                val elementsArray = JSONArray()
                layer.elements.forEach { elem ->
                    if (elem is Stroke) {
                        elementsArray.put(strokeToJson(elem))
                    } else if (elem is TextElement) {
                        elementsArray.put(JSONObject().apply {
                            put("type", "text")
                            put("x", elem.x)
                            put("y", elem.y)
                            put("font", elem.font)
                            put("size", elem.size)
                            put("color", elem.color)
                            put("content", elem.content)
                        })
                    }
                }
                layerObj.put("elements", elementsArray)
                layersArray.put(layerObj)
            }
            pageObj.put("layers", layersArray)

            pagesArray.put(pageObj)
        }
        root.put("pages", pagesArray)

        return root
    }

    fun strokeToJson(stroke: Stroke): JSONObject {
        val obj = JSONObject()
        obj.put("type", "stroke")
        obj.put("tool", stroke.tool.xml)
        obj.put("color", stroke.color)
        obj.put("capStyle", stroke.capStyle ?: "round")
        obj.put("uniformWidth", stroke.uniformWidth)
        obj.put("lineStyle", stroke.lineStyle.xml)

        val ptsArray = JSONArray()
        stroke.points.forEach { pt ->
            val ptObj = JSONObject()
            ptObj.put("x", pt.x)
            ptObj.put("y", pt.y)
            ptObj.put("w", pt.width)
            ptsArray.put(ptObj)
        }
        obj.put("points", ptsArray)
        return obj
    }

    fun strokeFromJson(json: JSONObject): Stroke {
        val toolStr = json.optString("tool", "pen")
        val tool = Tool.fromXml(toolStr)
        val color = json.optInt("color", 0xFF000000.toInt())
        val capStyle = json.optString("capStyle", "round")
        val uniformWidth = json.optBoolean("uniformWidth", true)
        val lineStyleStr = json.optString("lineStyle", "plain")
        val lineStyle = LineStyle.fromXml(lineStyleStr)

        val ptsJson = json.optJSONArray("points") ?: JSONArray()
        val points = mutableListOf<StrokePoint>()
        for (i in 0 until ptsJson.length()) {
            val p = ptsJson.getJSONObject(i)
            points.add(
                StrokePoint(
                    x = p.getDouble("x"),
                    y = p.getDouble("y"),
                    width = p.optDouble("w", 1.5)
                )
            )
        }

        return Stroke(
            tool = tool,
            color = color,
            capStyle = capStyle,
            points = points,
            uniformWidth = uniformWidth,
            lineStyle = lineStyle
        )
    }

    fun pageFromJson(json: JSONObject): Page {
        val width = json.optDouble("width", 595.276)
        val height = json.optDouble("height", 841.890)

        val bgObj = json.optJSONObject("background")
        val bgStyle = bgObj?.optString("style", "ruled") ?: "ruled"
        val bgColor = bgObj?.optInt("color", 0xFFFFFFFF.toInt()) ?: 0xFFFFFFFF.toInt()
        val background = Background.Solid(color = bgColor, style = bgStyle)

        val layers = mutableListOf<Layer>()
        val layersArray = json.optJSONArray("layers")
        if (layersArray != null) {
            for (l in 0 until layersArray.length()) {
                val layerObj = layersArray.getJSONObject(l)
                val elemsArray = layerObj.optJSONArray("elements") ?: JSONArray()
                val elements = mutableListOf<Element>()
                for (e in 0 until elemsArray.length()) {
                    val elemObj = elemsArray.getJSONObject(e)
                    val elemType = elemObj.optString("type")
                    if (elemType == "stroke") {
                        elements.add(strokeFromJson(elemObj))
                    } else if (elemType == "text") {
                        elements.add(
                            TextElement(
                                font = elemObj.optString("font", "Sans"),
                                size = elemObj.optDouble("size", 12.0),
                                x = elemObj.optDouble("x", 0.0),
                                y = elemObj.optDouble("y", 0.0),
                                color = elemObj.optInt("color", 0xFF000000.toInt()),
                                content = elemObj.optString("content", "")
                            )
                        )
                    }
                }
                layers.add(Layer(elements = elements))
            }
        }

        if (layers.isEmpty()) {
            layers.add(Layer(elements = emptyList()))
        }

        return Page(
            width = width,
            height = height,
            background = background,
            layers = layers
        )
    }
}
