package com.nexopp.repository

import java.util.UUID

/**
 * Stable, device-independent identity for a notebook document across tablets, PCs, and Cloud sync.
 */
data class DocumentIdentity(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val title: String,
    val version: Long = 1L,
    val deviceOrigin: String = "tablet-android",
    val lastModified: Long = System.currentTimeMillis(),
    val contentHashSha256: String = ""
)

/**
 * Sync status of an individual document.
 */
sealed class DocumentSyncState {
    object Synced : DocumentSyncState()
    object PendingUpload : DocumentSyncState()
    object PendingDownload : DocumentSyncState()
    object Syncing : DocumentSyncState()
    object Offline : DocumentSyncState()
    data class Conflict(val originalFileName: String, val conflictForkFileName: String) : DocumentSyncState()
    data class Error(val message: String) : DocumentSyncState()
}

/**
 * Rich domain metadata for a notebook document, decoupled from the core XML .xopp representation.
 */
data class DocumentMetadata(
    val identity: DocumentIdentity,
    val subjectId: String = "general",
    val tagIds: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val pageCount: Int = 1,
    val syncState: DocumentSyncState = DocumentSyncState.Synced,
    val customAttributes: Map<String, String> = emptyMap()
)
