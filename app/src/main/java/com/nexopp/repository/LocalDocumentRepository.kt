package com.nexopp.repository

import com.nexopp.format.XoppReader
import com.nexopp.format.XoppWriter
import com.nexopp.format.model.Document
import com.nexopp.library.LibraryStore
import com.nexopp.library.Notebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringWriter
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Local file-system and library-store implementation of [DocumentRepository].
 * Features atomic writes, crash safety, and sidecar management.
 */
class LocalDocumentRepository(
    private val notebooksDir: File,
    private val libraryStore: LibraryStore? = null
) : DocumentRepository {

    init {
        notebooksDir.mkdirs()
    }

    override suspend fun listDocuments(includeTrashed: Boolean): List<DocumentMetadata> = withContext(Dispatchers.IO) {
        val files = notebooksDir.listFiles { f -> f.isFile && f.extension == "xopp" } ?: emptyArray()
        val libraryNotebooks = libraryStore?.loadNotebooks()?.associateBy { it.fileName } ?: emptyMap()
        val trashedNotebooks = libraryStore?.loadTrash()?.associateBy { it.notebook.fileName } ?: emptyMap()

        val results = mutableListOf<DocumentMetadata>()

        for (file in files) {
            val fn = file.name
            val libNb = libraryNotebooks[fn]
            val isTrashed = trashedNotebooks.containsKey(fn)

            if (isTrashed && !includeTrashed) continue

            val identity = DocumentIdentity(
                id = libNb?.id ?: fn,
                fileName = fn,
                title = libNb?.name ?: fn.substringBeforeLast("."),
                lastModified = file.lastModified(),
                contentHashSha256 = calculateHash(file)
            )

            results.add(
                DocumentMetadata(
                    identity = identity,
                    subjectId = libNb?.subjectId ?: "general",
                    tagIds = libNb?.tagIds ?: emptyList(),
                    isFavorite = libNb?.isFavorite ?: false,
                    isTrashed = isTrashed,
                    pageCount = libNb?.pageCount ?: 1
                )
            )
        }
        results.sortedByDescending { it.identity.lastModified }
    }

    override suspend fun getDocumentMetadata(fileName: String): DocumentMetadata? = withContext(Dispatchers.IO) {
        val file = File(notebooksDir, fileName)
        if (!file.exists()) return@withContext null
        val libNb = libraryStore?.loadNotebooks()?.find { it.fileName == fileName }
        val isTrashed = libraryStore?.loadTrash()?.any { it.notebook.fileName == fileName } ?: false

        DocumentMetadata(
            identity = DocumentIdentity(
                id = libNb?.id ?: fileName,
                fileName = fileName,
                title = libNb?.name ?: fileName.substringBeforeLast("."),
                lastModified = file.lastModified(),
                contentHashSha256 = calculateHash(file)
            ),
            subjectId = libNb?.subjectId ?: "general",
            tagIds = libNb?.tagIds ?: emptyList(),
            isFavorite = libNb?.isFavorite ?: false,
            isTrashed = isTrashed,
            pageCount = libNb?.pageCount ?: 1
        )
    }

    override suspend fun updateDocumentMetadata(metadata: DocumentMetadata): Boolean = withContext(Dispatchers.IO) {
        if (libraryStore == null) return@withContext true
        val current = libraryStore.loadNotebooks().toMutableList()
        val index = current.indexOfFirst { it.fileName == metadata.identity.fileName }
        if (index >= 0) {
            current[index] = current[index].copy(
                name = metadata.identity.title,
                subjectId = metadata.subjectId,
                tagIds = metadata.tagIds,
                isFavorite = metadata.isFavorite,
                pageCount = metadata.pageCount,
                lastModified = metadata.identity.lastModified
            )
            libraryStore.save(notebooks = current)
            true
        } else {
            val newNb = Notebook(
                id = metadata.identity.id,
                name = metadata.identity.title,
                fileName = metadata.identity.fileName,
                subjectId = metadata.subjectId,
                tagIds = metadata.tagIds,
                isFavorite = metadata.isFavorite,
                pageCount = metadata.pageCount,
                lastModified = metadata.identity.lastModified
            )
            current.add(newNb)
            libraryStore.save(notebooks = current)
            true
        }
    }

    override suspend fun loadDocument(fileName: String): Result<Document> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(notebooksDir, fileName)
            if (!file.exists()) throw IllegalArgumentException("Document not found: $fileName")

            val bytes = file.readBytes()
            val xml = try {
                // Try reading as GZIP (.xopp standard)
                GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                // Fallback to plain XML if uncompressed
                String(bytes, Charsets.UTF_8)
            }
            XoppReader(xml).read()
        }
    }

    override suspend fun saveDocument(fileName: String, document: Document): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = File(notebooksDir, fileName)
            val tempFile = File(notebooksDir, "$fileName.tmp")

            val outWriter = StringWriter()
            XoppWriter(outWriter).write(document)
            val xml = outWriter.toString()

            val outBytes = ByteArrayOutputStream()
            GZIPOutputStream(outBytes).use { gz ->
                gz.write(xml.toByteArray(Charsets.UTF_8))
            }

            FileOutputStream(tempFile).use { it.write(outBytes.toByteArray()) }

            // Atomic rename
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                // Fallback copy if rename fails across partitions
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            // Update library registry
            libraryStore?.let { store ->
                val list = store.loadNotebooks().toMutableList()
                val idx = list.indexOfFirst { it.fileName == fileName }
                if (idx >= 0) {
                    list[idx] = list[idx].copy(
                        pageCount = document.pages.size,
                        lastModified = targetFile.lastModified()
                    )
                    store.save(notebooks = list)
                }
            }

            targetFile
        }
    }

    override suspend fun renameDocument(oldFileName: String, newFileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val oldFile = File(notebooksDir, oldFileName)
            val newFile = File(notebooksDir, newFileName)
            if (!oldFile.exists()) throw IllegalArgumentException("File not found: $oldFileName")

            oldFile.renameTo(newFile)

            // Rename sidecars if present
            val oldBase = oldFileName.substringBeforeLast(".")
            val newBase = newFileName.substringBeforeLast(".")

            val oldStructure = File(notebooksDir, "$oldBase.structure.json")
            if (oldStructure.exists()) oldStructure.renameTo(File(notebooksDir, "$newBase.structure.json"))

            val oldAttachmentsMeta = File(notebooksDir, "$oldBase.attachments.json")
            if (oldAttachmentsMeta.exists()) oldAttachmentsMeta.renameTo(File(notebooksDir, "$newBase.attachments.json"))

            val oldAttachmentsDir = File(notebooksDir, "attachments_$oldBase")
            if (oldAttachmentsDir.exists()) oldAttachmentsDir.renameTo(File(notebooksDir, "attachments_$newBase"))

            // Update library store
            libraryStore?.let { store ->
                val list = store.loadNotebooks().toMutableList()
                val idx = list.indexOfFirst { it.fileName == oldFileName }
                if (idx >= 0) {
                    list[idx] = list[idx].copy(
                        fileName = newFileName,
                        name = newBase,
                        lastModified = System.currentTimeMillis()
                    )
                    store.save(notebooks = list)
                }
            }
            Unit
        }
    }

    override suspend fun moveToTrash(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (libraryStore != null) {
                val nb = libraryStore.loadNotebooks().find { it.fileName == fileName }
                if (nb != null) {
                    libraryStore.moveToTrash(nb)
                }
            }
            Unit
        }
    }

    override suspend fun restoreFromTrash(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (libraryStore != null) {
                val item = libraryStore.loadTrash().find { it.notebook.fileName == fileName }
                if (item != null) {
                    libraryStore.restoreFromTrash(item.id)
                }
            }
            Unit
        }
    }

    override suspend fun permanentlyDelete(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(notebooksDir, fileName)
            if (file.exists()) file.delete()

            val base = fileName.substringBeforeLast(".")
            File(notebooksDir, "$base.structure.json").delete()
            File(notebooksDir, "$base.attachments.json").delete()
            File(notebooksDir, "attachments_$base").deleteRecursively()

            if (libraryStore != null) {
                val item = libraryStore.loadTrash().find { it.notebook.fileName == fileName }
                if (item != null) {
                    libraryStore.permanentlyDelete(item.id)
                }
            }
            Unit
        }
    }

    override suspend fun calculateContentHash(fileName: String): String = withContext(Dispatchers.IO) {
        val file = File(notebooksDir, fileName)
        calculateHash(file)
    }

    override fun getDocumentFile(fileName: String): File = File(notebooksDir, fileName)

    private fun calculateHash(file: File): String {
        if (!file.exists() || !file.canRead()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(8192)
            var r: Int
            while (input.read(buf).also { r = it } != -1) {
                digest.update(buf, 0, r)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
