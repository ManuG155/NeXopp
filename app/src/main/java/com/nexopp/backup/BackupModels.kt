package com.nexopp.backup

import java.io.File

/**
 * Strategy to resolve collisions when restoring a backup.
 */
enum class RestoreConflictPolicy {
    /** Overwrite existing local files with backup copies */
    OVERWRITE,
    /** Only restore files that do not exist or are newer in the backup */
    KEEP_NEWER,
    /** Keep existing local files and restore collisions with a " (Restaurado)" suffix */
    RENAME_DUPLICATES
}

/**
 * Metadata recorded inside a backup archive manifest.
 */
data class BackupManifest(
    val formatVersion: Int = 1,
    val appVersion: String = "1.0",
    val timestamp: Long = System.currentTimeMillis(),
    val totalNotebooks: Int = 0,
    val totalSubjects: Int = 0,
    val totalTags: Int = 0,
    val deviceName: String = "Dispositivo Android"
)

/**
 * Information about an existing local backup archive file.
 */
data class BackupInfo(
    val file: File,
    val timestamp: Long,
    val sizeBytes: Long,
    val manifest: BackupManifest? = null
)

/**
 * Result of a backup restore operation.
 */
data class RestoreResult(
    val success: Boolean,
    val restoredNotebooksCount: Int,
    val skippedCount: Int,
    val renamedCount: Int,
    val errorMessage: String? = null
)
