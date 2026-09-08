package com.nexopp.io

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentVerifierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `rejects non-existent and empty files`() {
        val nonExistent = File(tempFolder.root, "does_not_exist.xopp")
        assertFalse(DocumentVerifier.verify(nonExistent))

        val empty = tempFolder.newFile("empty.xopp")
        assertFalse(DocumentVerifier.verify(empty))

        val tiny = tempFolder.newFile("tiny.xopp").apply { writeText("too short") }
        assertFalse(DocumentVerifier.verify(tiny))
    }

    @Test
    fun `accepts valid gzipped xopp`() {
        val file = tempFolder.newFile("valid.xopp")
        FileOutputStream(file).use { fos ->
            GZIPOutputStream(fos).use { gos ->
                gos.write("<xournal creator=\"NeXopp\"><page width=\"595\" height=\"842\"/></xournal>".toByteArray(Charsets.UTF_8))
            }
        }
        assertTrue(DocumentVerifier.verify(file))
    }

    @Test
    fun `rejects corrupted gzip stream`() {
        val file = tempFolder.newFile("corrupt.xopp")
        file.writeBytes(byteArrayOf(0x1f.toByte(), 0x8b.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07))
        assertFalse(DocumentVerifier.verify(file))
    }

    @Test
    fun `accepts valid zipped xopp`() {
        val file = tempFolder.newFile("valid_zipped.xopp")
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("document.xopp"))
            zos.write("<xournal creator=\"NeXopp\"/>".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        assertTrue(DocumentVerifier.verify(file))
    }

    @Test
    fun `accepts plain xml xopp`() {
        val file = tempFolder.newFile("plain.xopp").apply {
            writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xournal version=\"0.4.8\">\n<page width=\"612\" height=\"792\"/></xournal>")
        }
        assertTrue(DocumentVerifier.verify(file))
    }
}
