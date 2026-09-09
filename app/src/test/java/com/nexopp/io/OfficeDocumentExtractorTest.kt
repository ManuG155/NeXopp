package com.nexopp.io

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OfficeDocumentExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `detectKind correctly identifies all supported formats`() {
        assertEquals(OfficeDocumentExtractor.OfficeKind.DOCX, OfficeDocumentExtractor.detectKind("apuntes.docx"))
        assertEquals(OfficeDocumentExtractor.OfficeKind.XLSX, OfficeDocumentExtractor.detectKind("datos.xlsx"))
        assertEquals(OfficeDocumentExtractor.OfficeKind.PPTX, OfficeDocumentExtractor.detectKind("presentacion.pptx"))
        assertEquals(OfficeDocumentExtractor.OfficeKind.ODT, OfficeDocumentExtractor.detectKind("texto.odt"))
        assertEquals(OfficeDocumentExtractor.OfficeKind.ODS, OfficeDocumentExtractor.detectKind("hoja.ods"))
        assertEquals(OfficeDocumentExtractor.OfficeKind.ODP, OfficeDocumentExtractor.detectKind("diapositivas.odp"))
        assertEquals(OfficeDocumentExtractor.OfficeKind.HTML, OfficeDocumentExtractor.detectKind("pagina.html"))
        assertEquals(OfficeDocumentExtractor.OfficeKind.EPUB, OfficeDocumentExtractor.detectKind("libro.epub"))
        assertEquals(OfficeDocumentExtractor.OfficeKind.UNKNOWN, OfficeDocumentExtractor.detectKind("apuntes.pdf"))
    }

    @Test
    fun `extracts DOCX paragraphs and headings`() {
        val file = tempFolder.newFile("test.docx")
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("word/document.xml"))
            val xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                    <w:body>
                        <w:p>
                            <w:pPr><w:pStyle w:val="Heading1"/></w:pPr>
                            <w:r><w:t>Título de Física Cuántica</w:t></w:r>
                        </w:p>
                        <w:p>
                            <w:r><w:t>La ecuación de Schrödinger describe la evolución del estado cuántico.</w:t></w:r>
                        </w:p>
                    </w:body>
                </w:document>
            """.trimIndent()
            zos.write(xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val md = OfficeDocumentExtractor.extractDocx(file)
        assertTrue(md.contains("# Título de Física Cuántica"))
        assertTrue(md.contains("La ecuación de Schrödinger"))
    }

    @Test
    fun `extracts HTML into markdown headings and lists`() {
        val html = """
            <html>
                <body>
                    <h1>Termodinámica</h1>
                    <p>Primer principio de la conservación de la energía.</p>
                    <ul>
                        <li>Entalpía</li>
                        <li>Entropía</li>
                    </ul>
                </body>
            </html>
        """.trimIndent()
        val md = OfficeDocumentExtractor.extractHtml(html)
        assertTrue(md.contains("# Termodinámica"))
        assertTrue(md.contains("Primer principio"))
        assertTrue(md.contains("- Entalpía"))
        assertTrue(md.contains("- Entropía"))
    }

    @Test
    fun `extracts ODT text and headings`() {
        val file = tempFolder.newFile("test.odt")
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("content.xml"))
            val xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                    <office:body>
                        <office:text>
                            <text:h text:outline-level="1">Álgebra Lineal</text:h>
                            <text:p>Los vectores propios cumplen Av = lambda v.</text:p>
                        </office:text>
                    </office:body>
                </office:document-content>
            """.trimIndent()
            zos.write(xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val md = OfficeDocumentExtractor.extractOdt(file)
        assertTrue(md.contains("## Álgebra Lineal"))
        assertTrue(md.contains("Los vectores propios"))
    }
}
