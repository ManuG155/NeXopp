package com.nexopp.lan

import com.nexopp.format.FontDescription
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
    const val TYPE_ERASE_STROKES = "ERASE_STROKES"
    const val TYPE_STROKES_ERASED = "STROKES_ERASED"
    const val TYPE_ADD_TEXT = "ADD_TEXT"
    const val TYPE_TEXT_ADDED = "TEXT_ADDED"
    const val TYPE_ADD_PAGE = "ADD_PAGE"
    const val TYPE_PAGE_ADDED = "PAGE_ADDED"
    const val TYPE_TABLET_DOCUMENT_CHANGED = "TABLET_DOCUMENT_CHANGED"
    const val TYPE_SAVE_DOCUMENT = "SAVE_DOCUMENT"
    const val TYPE_DOCUMENT_SAVED = "DOCUMENT_SAVED"
    const val TYPE_CHECK_DOCUMENT_VERSION = "CHECK_DOCUMENT_VERSION"
    const val TYPE_DOCUMENT_UP_TO_DATE = "DOCUMENT_UP_TO_DATE"
    const val TYPE_STROKE_ACK = "STROKE_ACK"
    const val TYPE_TEXT_ACK = "TEXT_ACK"

    const val TYPE_STATUS = "STATUS"
    const val TYPE_ERROR = "ERROR"

    data class PointPayload(val x: Double, val y: Double, val radius: Double = 12.0)

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
            pagesArray.put(pageToJson(page, pageIdx))
        }
        root.put("pages", pagesArray)

        return root
    }

    fun pageToJson(page: Page, pageIdx: Int = 0): JSONObject {
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
                    elementsArray.put(textToJson(elem))
                }
            }
            layerObj.put("elements", elementsArray)
            layersArray.put(layerObj)
        }
        pageObj.put("layers", layersArray)
        return pageObj
    }

    fun pointPayloadsFromJson(array: JSONArray): List<PointPayload> {
        val list = mutableListOf<PointPayload>()
        for (i in 0 until array.length()) {
            val p = array.getJSONObject(i)
            list.add(
                PointPayload(
                    x = p.getDouble("x"),
                    y = p.getDouble("y"),
                    radius = p.optDouble("radius", p.optDouble("r", 12.0))
                )
            )
        }
        return list
    }

    fun pointPayloadsToJson(points: List<PointPayload>): JSONArray {
        val array = JSONArray()
        points.forEach { pt ->
            val obj = JSONObject()
            obj.put("x", pt.x)
            obj.put("y", pt.y)
            obj.put("radius", pt.radius)
            array.put(obj)
        }
        return array
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

    fun textToJson(text: TextElement): JSONObject {
        val obj = JSONObject()
        obj.put("type", "text")
        obj.put("x", text.x)
        obj.put("y", text.y)
        obj.put("font", text.font)
        obj.put("size", text.size)
        obj.put("color", text.color)
        obj.put("content", text.content)

        val fd = FontDescription.parse(text.font)
        obj.put("bold", fd.bold)
        obj.put("italic", fd.italic)
        obj.put("underline", text.extraAttrs["underline"] == "true")

        if (text.extraAttrs.isNotEmpty()) {
            val extraObj = JSONObject()
            text.extraAttrs.forEach { (k, v) -> extraObj.put(k, v) }
            obj.put("extraAttrs", extraObj)
        }
        return obj
    }

    fun textFromJson(json: JSONObject): TextElement {
        val rawFont = json.optString("font", "Liberation Sans")
        val bold = json.optBoolean("bold", false)
        val italic = json.optBoolean("italic", false)
        val underline = json.optBoolean("underline", false)

        val fd = FontDescription.parse(rawFont)
        val finalFont = if (bold || italic || fd.bold || fd.italic) {
            FontDescription(
                family = fd.family.ifBlank { "Liberation Sans" },
                bold = bold || fd.bold,
                italic = italic || fd.italic
            ).compose()
        } else {
            rawFont.ifBlank { "Liberation Sans" }
        }

        val extraMap = mutableMapOf<String, String>()
        val extraObj = json.optJSONObject("extraAttrs")
        if (extraObj != null) {
            val keys = extraObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                extraMap[k] = extraObj.optString(k, "")
            }
        }
        if (underline) {
            extraMap["underline"] = "true"
        }

        return TextElement(
            font = finalFont,
            size = json.optDouble("size", 14.0),
            x = json.optDouble("x", 100.0),
            y = json.optDouble("y", 100.0),
            color = json.optInt("color", 0xFF000000.toInt()),
            content = json.optString("content", ""),
            extraAttrs = extraMap
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
                        elements.add(textFromJson(elemObj))
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
