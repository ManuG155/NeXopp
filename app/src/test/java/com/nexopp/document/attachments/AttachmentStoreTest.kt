package com.nexopp.document.attachments

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class AttachmentStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: AttachmentStore
    private lateinit var baseDir: File

    @Before
    fun setup() {
        baseDir = tempFolder.newFolder("attachments_test")
        store = AttachmentStore(baseDir)
    }

    @Test
    fun testEmptyAttachments() {
        val list = store.listAttachments("cuaderno1.xopp")
        assertTrue(list.isEmpty())
    }

    @Test
    fun testAddAndListAttachments() {
        val nb = "fisica_cuantica.xopp"
        val samplePdfData = "PDF_MOCK_DATA_BYTES".toByteArray()
        val sampleImgData = "IMAGE_PNG_BYTES".toByteArray()

        val att1 = store.addAttachment(
            notebookFileName = nb,
            name = "formulario_ondas.pdf",
            mimeType = "application/pdf",
            inputStream = ByteArrayInputStream(samplePdfData),
            pageIndex = 2
        )
        assertNotNull(att1)
        assertEquals("formulario_ondas.pdf", att1?.name)
        assertEquals(samplePdfData.size.toLong(), att1?.sizeBytes)
        assertEquals(2, att1?.pageIndex)

        val att2 = store.addAttachment(
            notebookFileName = nb,
            name = "diagrama_feynman.png",
            mimeType = "image/png",
            inputStream = ByteArrayInputStream(sampleImgData),
            pageIndex = null
        )
        assertNotNull(att2)

        val list = store.listAttachments(nb)
        assertEquals(2, list.size)

        val file1 = store.getAttachmentFile(nb, att1!!)
        assertTrue(file1.exists())
        assertEquals(samplePdfData.size.toLong(), file1.length())
    }

    @Test
    fun testDeleteAttachment() {
        val nb = "quimica.xopp"
        val data = "SAMPLE_DOC".toByteArray()
        val att = store.addAttachment(
            notebookFileName = nb,
            name = "tabla_periodica.pdf",
            mimeType = "application/pdf",
            inputStream = ByteArrayInputStream(data)
        )
        assertNotNull(att)
        assertEquals(1, store.listAttachments(nb).size)

        val file = store.getAttachmentFile(nb, att!!)
        assertTrue(file.exists())

        val deleted = store.deleteAttachment(nb, att.id)
        assertTrue(deleted)
        assertEquals(0, store.listAttachments(nb).size)
        assertFalse(file.exists())
    }
}
