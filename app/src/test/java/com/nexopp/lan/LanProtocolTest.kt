package com.nexopp.lan

import com.nexopp.format.model.*
import com.nexopp.library.Notebook
import com.nexopp.library.Subject
import com.nexopp.library.Tag
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LanProtocolTest {

    @Test
    fun libraryToJson_serializesCorrectly() {
        val subjects = listOf(
            Subject(id = "sub1", name = "Matemáticas", color = 0xFF1976D2, parentId = null),
            Subject(id = "sub2", name = "Álgebra", color = 0xFF38BDF8, parentId = "sub1")
        )
        val notebooks = listOf(
            Notebook(
                id = "nb1",
                subjectId = "sub2",
                name = "Álgebra Lineal",
                fileName = "algebra.xopp",
                coverColor = 0xFF1E3A8A,
                initialTemplate = "ruled",
                pageCount = 3
            )
        )
        val tags = listOf(
            Tag(id = "tag1", name = "Examen", color = 0xFFDC2626)
        )

        val json = LanProtocol.libraryToJson(subjects, notebooks, tags)

        assertEquals(LanProtocol.TYPE_LIBRARY_DATA, json.getString("type"))
        val subs = json.getJSONArray("subjects")
        assertEquals(2, subs.length())
        assertEquals("Matemáticas", subs.getJSONObject(0).getString("name"))
        assertEquals("sub1", subs.getJSONObject(1).getString("parentId"))

        val nbs = json.getJSONArray("notebooks")
        assertEquals(1, nbs.length())
        assertEquals("Álgebra Lineal", nbs.getJSONObject(0).getString("name"))
        assertEquals(3, nbs.getJSONObject(0).getInt("pageCount"))
    }

    @Test
    fun strokeRoundtrip_preservesAllProperties() {
        val stroke = Stroke(
            tool = Tool.HIGHLIGHTER,
            color = 0x55F59E0B,
            capStyle = "round",
            points = listOf(
                StrokePoint(x = 10.0, y = 20.0, width = 5.0),
                StrokePoint(x = 30.0, y = 40.0, width = 8.0)
            ),
            uniformWidth = false,
            lineStyle = LineStyle.PLAIN
        )

        val json = LanProtocol.strokeToJson(stroke)
        val roundtrip = LanProtocol.strokeFromJson(json)

        assertEquals(Tool.HIGHLIGHTER, roundtrip.tool)
        assertEquals(0x55F59E0B, roundtrip.color)
        assertEquals("round", roundtrip.capStyle)
        assertEquals(2, roundtrip.points.size)
        assertEquals(10.0, roundtrip.points[0].x, 0.001)
        assertEquals(20.0, roundtrip.points[0].y, 0.001)
        assertEquals(5.0, roundtrip.points[0].width, 0.001)
        assertEquals(30.0, roundtrip.points[1].x, 0.001)
        assertEquals(40.0, roundtrip.points[1].y, 0.001)
        assertEquals(8.0, roundtrip.points[1].width, 0.001)
    }

    @Test
    fun documentToJson_serializesPagesAndStrokes() {
        val stroke = Stroke(
            tool = Tool.PEN,
            color = 0xFF000000.toInt(),
            capStyle = "round",
            points = listOf(StrokePoint(0.0, 0.0, 1.5), StrokePoint(50.0, 50.0, 1.5)),
            uniformWidth = true
        )
        val page = Page(
            width = 595.276,
            height = 841.890,
            background = Background.Solid(0xFFFFFFFF.toInt(), "ruled"),
            layers = listOf(Layer(elements = listOf(stroke)))
        )
        val doc = Document(pages = listOf(page))

        val json = LanProtocol.documentToJson(
            notebookId = "nb-123",
            fileName = "notes.xopp",
            title = "Mis Notas",
            document = doc,
            version = 42L
        )

        assertEquals(LanProtocol.TYPE_DOCUMENT_DATA, json.getString("type"))
        assertEquals("nb-123", json.getString("notebookId"))
        assertEquals("Mis Notas", json.getString("title"))
        assertEquals(42L, json.getLong("version"))

        val pages = json.getJSONArray("pages")
        assertEquals(1, pages.length())
        val p0 = pages.getJSONObject(0)
        assertEquals(595.276, p0.getDouble("width"), 0.001)
        assertEquals("ruled", p0.getJSONObject("background").getString("style"))

        val layers = p0.getJSONArray("layers")
        assertEquals(1, layers.length())
        val elements = layers.getJSONObject(0).getJSONArray("elements")
        assertEquals(1, elements.length())
        assertEquals("stroke", elements.getJSONObject(0).getString("type"))
        assertEquals("pen", elements.getJSONObject(0).getString("tool"))
    }

    @Test
    fun textRoundtrip_preservesAllProperties() {
        val text = TextElement(
            font = "Liberation Sans",
            size = 16.0,
            x = 120.0,
            y = 250.0,
            color = 0xFF2563EB.toInt(),
            content = "Nota de texto desde PC\nSegunda línea"
        )

        val json = LanProtocol.textToJson(text)
        val roundtrip = LanProtocol.textFromJson(json)

        assertEquals("Liberation Sans", roundtrip.font)
        assertEquals(16.0, roundtrip.size, 0.001)
        assertEquals(120.0, roundtrip.x, 0.001)
        assertEquals(250.0, roundtrip.y, 0.001)
        assertEquals(0xFF2563EB.toInt(), roundtrip.color)
        assertEquals("Nota de texto desde PC\nSegunda línea", roundtrip.content)
    }

    @Test
    fun documentToJson_serializesTextElements() {
        val text = TextElement(
            font = "Liberation Sans",
            size = 14.0,
            x = 50.0,
            y = 100.0,
            color = 0xFF000000.toInt(),
            content = "Texto en documento"
        )
        val page = Page(
            width = 595.276,
            height = 841.890,
            background = Background.Solid(0xFFFFFFFF.toInt(), "ruled"),
            layers = listOf(Layer(elements = listOf(text)))
        )
        val doc = Document(pages = listOf(page))

        val json = LanProtocol.documentToJson(
            notebookId = "nb-456",
            fileName = "text_doc.xopp",
            title = "Documento con Texto",
            document = doc,
            version = 1L
        )

        val pages = json.getJSONArray("pages")
        val elements = pages.getJSONObject(0).getJSONArray("layers").getJSONObject(0).getJSONArray("elements")
        assertEquals(1, elements.length())
        val textObj = elements.getJSONObject(0)
        assertEquals("text", textObj.getString("type"))
        assertEquals("Texto en documento", textObj.getString("content"))
        assertEquals(14.0, textObj.getDouble("size"), 0.001)
    }

    @Test
    fun pointPayloadRoundtrip_preservesCoordinatesAndRadius() {
        val points = listOf(
            LanProtocol.PointPayload(10.5, 20.7, 12.0),
            LanProtocol.PointPayload(35.2, 88.9, 15.0)
        )
        val jsonArray = LanProtocol.pointPayloadsToJson(points)
        val roundtrip = LanProtocol.pointPayloadsFromJson(jsonArray)

        assertEquals(2, roundtrip.size)
        assertEquals(10.5, roundtrip[0].x, 0.001)
        assertEquals(20.7, roundtrip[0].y, 0.001)
        assertEquals(12.0, roundtrip[0].radius, 0.001)
        assertEquals(35.2, roundtrip[1].x, 0.001)
        assertEquals(88.9, roundtrip[1].y, 0.001)
        assertEquals(15.0, roundtrip[1].radius, 0.001)
    }

    @Test
    fun pageToJson_serializesSinglePageCorrectly() {
        val stroke = Stroke(
            tool = Tool.PEN,
            color = 0xFF000000.toInt(),
            capStyle = "round",
            points = listOf(StrokePoint(10.0, 10.0, 2.0)),
            uniformWidth = true
        )
        val page = Page(
            width = 500.0,
            height = 700.0,
            background = Background.Solid(0xFFFFFFFF.toInt(), "graph"),
            layers = listOf(Layer(elements = listOf(stroke)))
        )
        val pageJson = LanProtocol.pageToJson(page, pageIdx = 2)

        assertEquals(2, pageJson.getInt("index"))
        assertEquals(500.0, pageJson.getDouble("width"), 0.001)
        assertEquals(700.0, pageJson.getDouble("height"), 0.001)
        assertEquals("graph", pageJson.getJSONObject("background").getString("style"))
        assertEquals(1, pageJson.getJSONArray("layers").length())
    }
}
