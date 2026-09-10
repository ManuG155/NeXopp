// Ruta: app/src/test/java/com/nexopp/ui/ToolIndependenceTest.kt
package com.nexopp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolIndependenceTest {

    @Test
    fun `test explicit independence between pen, highlighter, and eraser`() {
        val ui = EditorUiState(
            tool = EditorTool.PEN,
            color = 0xFF000000.toInt(), // Negro
            width = 3.0f,
            penColor = 0xFF000000.toInt(),
            penWidth = 3.0f,
            highlighterColor = 0xFFFFF176.toInt(), // Amarillo
            highlighterWidth = 12.0f,
            eraserWidth = 5.0f
        )

        // 1. Initial verification
        assertEquals(EditorTool.PEN, ui.tool)
        assertEquals(3.0f, ui.width, 0.001f)
        assertEquals(0xFF000000.toInt(), ui.color)
        assertEquals(3.0f, ui.penWidth, 0.001f)
        assertEquals(12.0f, ui.highlighterWidth, 0.001f)
        assertEquals(5.0f, ui.eraserWidth, 0.001f)

        // 2. Si cambio Pluma a grosor 7:
        //    -> Subrayador sigue en 12
        //    -> Borrador sigue en 5
        ui.width = 7.0f
        assertEquals(7.0f, ui.width, 0.001f)
        assertEquals(7.0f, ui.penWidth, 0.001f)
        assertEquals(12.0f, ui.highlighterWidth, 0.001f)
        assertEquals(5.0f, ui.eraserWidth, 0.001f)

        // 3. Cambiar a Subrayador
        ui.tool = EditorTool.HIGHLIGHTER
        assertEquals(EditorTool.HIGHLIGHTER, ui.tool)
        assertEquals(12.0f, ui.width, 0.001f)
        assertEquals(0xFFFFF176.toInt(), ui.color)

        // 4. Si cambio Subrayador a grosor 20:
        //    -> Pluma sigue en 7
        //    -> Borrador sigue en 5
        ui.width = 20.0f
        assertEquals(20.0f, ui.width, 0.001f)
        assertEquals(20.0f, ui.highlighterWidth, 0.001f)
        assertEquals(7.0f, ui.penWidth, 0.001f)
        assertEquals(5.0f, ui.eraserWidth, 0.001f)

        // 5. Cambiar a Borrador
        ui.tool = EditorTool.ERASER
        assertEquals(EditorTool.ERASER, ui.tool)
        assertEquals(5.0f, ui.width, 0.001f)

        // 6. Si cambio Borrador a grosor 10:
        //    -> Pluma sigue en 7
        //    -> Subrayador sigue en 20
        ui.width = 10.0f
        assertEquals(10.0f, ui.width, 0.001f)
        assertEquals(10.0f, ui.eraserWidth, 0.001f)
        assertEquals(7.0f, ui.penWidth, 0.001f)
        assertEquals(20.0f, ui.highlighterWidth, 0.001f)

        // 7. Volver a Pluma y comprobar que recupera su configuración exacta
        ui.tool = EditorTool.PEN
        assertEquals(EditorTool.PEN, ui.tool)
        assertEquals(7.0f, ui.width, 0.001f)
        assertEquals(0xFF000000.toInt(), ui.color)

        // 8. Volver a Subrayador y comprobar que recupera su configuración exacta
        ui.tool = EditorTool.HIGHLIGHTER
        assertEquals(EditorTool.HIGHLIGHTER, ui.tool)
        assertEquals(20.0f, ui.width, 0.001f)
        assertEquals(0xFFFFF176.toInt(), ui.color)

        // 9. Volver a Borrador y comprobar que recupera su grosor exacto
        ui.tool = EditorTool.ERASER
        assertEquals(EditorTool.ERASER, ui.tool)
        assertEquals(10.0f, ui.width, 0.001f)
    }

    @Test
    fun `test color independence between pen and highlighter`() {
        val ui = EditorUiState(
            tool = EditorTool.PEN,
            penColor = 0xFF000000.toInt(),
            highlighterColor = 0xFFFFF176.toInt()
        )

        // Change pen color to blue
        ui.color = 0xFF1E88E5.toInt()
        assertEquals(0xFF1E88E5.toInt(), ui.penColor)
        assertEquals(0xFFFFF176.toInt(), ui.highlighterColor)

        // Switch to highlighter
        ui.tool = EditorTool.HIGHLIGHTER
        assertEquals(0xFFFFF176.toInt(), ui.color)

        // Change highlighter color to pink
        ui.color = 0xFFF06292.toInt()
        assertEquals(0xFFF06292.toInt(), ui.highlighterColor)
        assertEquals(0xFF1E88E5.toInt(), ui.penColor)

        // Switch back to pen
        ui.tool = EditorTool.PEN
        assertEquals(0xFF1E88E5.toInt(), ui.color)
        assertEquals(0xFF1E88E5.toInt(), ui.penColor)
    }
}
