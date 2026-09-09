package com.nexopp.sync

import java.io.File

/**
 * Status of the synchronization engine.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    CONFLICTS_DETECTED,
    ERROR
}

/**
 * Action determined for an individual file during two-way sync.
 */
enum class FileSyncAction {
    UPLOAD,
    DOWNLOAD,
    CONFLICT_FORK,
    DELETE_LOCAL,
    DELETE_REMOTE,
    UP_TO_DATE
}

/**
 * Provider type for sync backend.
 */
enum class SyncProviderType {
    LOCAL_FOLDER,
    WEBDAV_NEXTCLOUD
}

/**
 * Information regarding a file on the remote sync target.
 */
data class RemoteFileInfo(
    val filename: String,
    val remotePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val etag: String? = null
)

/**
 * Persistent sync configuration.
 */
data class SyncConfig(
    val providerType: SyncProviderType = SyncProviderType.LOCAL_FOLDER,
    val serverUrl: String = "",
    val username: String = "",
    val passwordOrToken: String = "",
    val remoteDirectory: String = "NeXopp",
    val localFolderPath: String = "",
    val autoSyncEnabled: Boolean = false,
    val preserveBothOnConflict: Boolean = true,
    val lastSyncTimestamp: Long = 0L
)

/**
 * Outcome of a sync run.
 */
data class SyncResult(
    val success: Boolean,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val conflictsCount: Int = 0,
    val errorsCount: Int = 0,
    val logs: List<String> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Result of testing connectivity with remote provider.
 */
data class SyncConnectionResult(
    val success: Boolean,
    val message: String
)
