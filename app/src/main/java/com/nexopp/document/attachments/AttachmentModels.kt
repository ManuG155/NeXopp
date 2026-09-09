package com.nexopp.document.attachments

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * An attachment or reference document associated with a notebook or specific page.
 */
data class DocumentAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val pageIndex: Int? = null,
    val localFileName: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages attachments storage and metadata for notebooks.
 */
class AttachmentStore(private val storageDir: File) {

    fun attachmentsDirFor(notebookFileName: String): File {
        val baseName = notebookFileName.substringBeforeLast(".")
        return File(storageDir, "attachments_$baseName").apply { mkdirs() }
    }

    private fun metadataFileFor(notebookFileName: String): File {
        val baseName = notebookFileName.substringBeforeLast(".")
        return File(storageDir, "$baseName.attachments.json")
    }

    fun listAttachments(notebookFileName: String): List<DocumentAttachment> {
        val file = metadataFileFor(notebookFileName)
        if (!file.exists() || !file.canRead()) return emptyList()

        return try {
            val json = JSONObject(file.readText())
            val arr = json.optJSONArray("attachments") ?: JSONArray()
            val list = mutableListOf<DocumentAttachment>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DocumentAttachment(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        sizeBytes = obj.getLong("sizeBytes"),
                        mimeType = obj.optString("mimeType", "application/octet-stream"),
                        pageIndex = if (obj.has("pageIndex") && !obj.isNull("pageIndex")) obj.getInt("pageIndex") else null,
                        localFileName = obj.getString("localFileName"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addAttachment(
        notebookFileName: String,
        name: String,
        mimeType: String,
        inputStream: InputStream,
        pageIndex: Int? = null
    ): DocumentAttachment? {
        val dir = attachmentsDirFor(notebookFileName)
        val ext = name.substringAfterLast(".", "")
        val localName = "${UUID.randomUUID()}${if (ext.isNotBlank()) ".$ext" else ""}"
        val destFile = File(dir, localName)

        return try {
            destFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            val attachment = DocumentAttachment(
                name = name,
                sizeBytes = destFile.length(),
                mimeType = mimeType,
                pageIndex = pageIndex,
                localFileName = localName,
                timestamp = System.currentTimeMillis()
            )

            val current = listAttachments(notebookFileName).toMutableList()
            current.add(attachment)
            saveList(notebookFileName, current)
            attachment
        } catch (_: Exception) {
            null
        }
    }

    fun deleteAttachment(notebookFileName: String, attachmentId: String): Boolean {
        val current = listAttachments(notebookFileName)
        val toDelete = current.firstOrNull { it.id == attachmentId } ?: return false
        val dir = attachmentsDirFor(notebookFileName)
        File(dir, toDelete.localFileName).delete()

        val updated = current.filter { it.id != attachmentId }
        saveList(notebookFileName, updated)
        return true
    }

    fun getAttachmentFile(notebookFileName: String, attachment: DocumentAttachment): File {
        val dir = attachmentsDirFor(notebookFileName)
        return File(dir, attachment.localFileName)
    }

    private fun saveList(notebookFileName: String, list: List<DocumentAttachment>) {
        val file = metadataFileFor(notebookFileName)
        try {
            val json = JSONObject()
            val arr = JSONArray()
            for (att in list) {
                arr.put(JSONObject().apply {
                    put("id", att.id)
                    put("name", att.name)
                    put("sizeBytes", att.sizeBytes)
                    put("mimeType", att.mimeType)
                    if (att.pageIndex != null) put("pageIndex", att.pageIndex)
                    put("localFileName", att.localFileName)
                    put("timestamp", att.timestamp)
                })
            }
            json.put("attachments", arr)
            file.writeText(json.toString(2))
        } catch (_: Exception) {}
    }
}
