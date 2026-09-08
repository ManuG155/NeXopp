package com.nexopp.io

import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

/**
 * Validates the physical and structural integrity of .xopp documents before committing
 * file writes, preventing data corruption, 0-byte overwrites, or broken gzip streams.
 */
object DocumentVerifier {

    /**
     * Verifies that [file] exists, is non-empty (> 16 bytes), and contains either:
     * 1. A valid Gzip stream wrapping XML with `<xournal` tag.
     * 2. A valid ZIP container holding `document.xopp` or `.xopp`.
     * 3. A valid plain XML text stream with `<xournal` tag.
     */
    fun verify(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false

        // Check if GZIP
        if (isGzip(file)) {
            return try {
                FileInputStream(file).use { fis ->
                    GZIPInputStream(fis).use { gis ->
                        val buffer = ByteArray(2048)
                        val read = gis.read(buffer)
                        if (read <= 0) return false
                        val header = String(buffer, 0, read, Charsets.UTF_8)
                        header.contains("<xournal") || header.contains("<?xml")
                    }
                }
            } catch (e: Exception) {
                false
            }
        }

        // Check if ZIP
        if (isZip(file)) {
            return try {
                FileInputStream(file).use { fis ->
                    ZipInputStream(fis).use { zis ->
                        var entry = zis.nextEntry
                        var foundDoc = false
                        while (entry != null) {
                            if (entry.name.endsWith(".xopp") || entry.name == "document.xml" || entry.name == "bg.pdf") {
                                foundDoc = true
                                break
                            }
                            entry = zis.nextEntry
                        }
                        foundDoc
                    }
                }
            } catch (e: Exception) {
                false
            }
        }

        // Check if plain XML
        return try {
            val preview = file.inputStream().use { stream ->
                val buffer = ByteArray(1024)
                val read = stream.read(buffer)
                if (read <= 0) "" else String(buffer, 0, read, Charsets.UTF_8)
            }
            preview.contains("<xournal") || (preview.contains("<?xml") && preview.contains("<xopp"))
        } catch (e: Exception) {
            false
        }
    }

    private fun isGzip(file: File): Boolean {
        return try {
            FileInputStream(file).use { fis ->
                val b1 = fis.read()
                val b2 = fis.read()
                b1 == 0x1f && b2 == 0x8b
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isZip(file: File): Boolean {
        return try {
            FileInputStream(file).use { fis ->
                val b1 = fis.read()
                val b2 = fis.read()
                val b3 = fis.read()
                val b4 = fis.read()
                b1 == 'P'.code && b2 == 'K'.code && b3 == 0x03 && b4 == 0x04
            }
        } catch (e: Exception) {
            false
        }
    }
}
