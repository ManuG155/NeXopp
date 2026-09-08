package com.nexopp.io

import com.nexopp.format.SaveFormat
import com.nexopp.format.Xopp
import com.nexopp.format.model.Document
import java.io.File
import java.util.concurrent.Executors

/**
 * Manages crash recovery snapshots in a dedicated recovery directory.
 * If the app is killed by the OS or crashes unexpectedly, unsaved work can be restored.
 */
class CrashRecoveryManager(
    private val recoveryDir: File,
    private val io: DocumentIo? = null
) {
    init {
        if (!recoveryDir.exists()) {
            recoveryDir.mkdirs()
        }
    }

    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Checkpoints a document snapshot for [tabId] in background.
     */
    fun checkpoint(tabId: String, doc: Document, pdf: File?, images: Map<String, File>) {
        executor.execute {
            try {
                val recoveryFile = getRecoveryFile(tabId)
                val staged = if (io != null) {
                    io.encode(
                        document = doc,
                        pdf = pdf,
                        format = SaveFormat.ORIGINAL,
                        images = images
                    )
                } else {
                    val tmp = File(recoveryDir, "$tabId.stage")
                    tmp.outputStream().use { Xopp.save(doc, it) }
                    tmp
                }
                if (DocumentVerifier.verify(staged)) {
                    val tmp = File(recoveryDir, "$tabId.tmp")
                    staged.copyTo(tmp, overwrite = true)
                    staged.delete()
                    if (!tmp.renameTo(recoveryFile)) {
                        tmp.copyTo(recoveryFile, overwrite = true)
                        tmp.delete()
                    }
                } else {
                    staged.delete()
                }
            } catch (e: Exception) {
                // Best effort recovery checkpoint
            }
        }
    }

    /**
     * Discards the recovery checkpoint for [tabId] once cleanly saved or closed.
     */
    fun discard(tabId: String) {
        executor.execute {
            try {
                val f = getRecoveryFile(tabId)
                if (f.exists()) f.delete()
                val tmp = File(recoveryDir, "$tabId.tmp")
                if (tmp.exists()) tmp.delete()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Returns a valid recovery snapshot file if one exists for [tabId] and is newer than [savedFile].
     */
    fun findPendingRecovery(tabId: String, savedFile: File?): File? {
        val f = getRecoveryFile(tabId)
        if (!f.exists() || f.length() < 16) return null
        if (!DocumentVerifier.verify(f)) return null

        if (savedFile != null && savedFile.exists()) {
            if (f.lastModified() <= savedFile.lastModified()) {
                return null
            }
        }
        return f
    }

    private fun getRecoveryFile(tabId: String): File = File(recoveryDir, "$tabId.recovery.xopp")

    /**
     * Prunes old recovery files older than [maxAgeMs] (default 7 days).
     */
    fun prune(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        executor.execute {
            try {
                val now = System.currentTimeMillis()
                recoveryDir.listFiles()?.forEach { file ->
                    if (now - file.lastModified() > maxAgeMs) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
