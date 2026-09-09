package com.nexopp.sync

import java.io.File

/**
 * Pluggable sync backend contract for local-first, privacy-respecting sync.
 */
interface SyncProvider {
    val id: String
    val displayName: String

    /**
     * Test connection and authentication credentials.
     */
    suspend fun testConnection(): SyncConnectionResult

    /**
     * Lists all files available in remote sync directory.
     */
    suspend fun listRemoteFiles(): List<RemoteFileInfo>

    /**
     * Uploads local file to remote sync destination.
     */
    suspend fun uploadFile(localFile: File, remoteFileName: String): Boolean

    /**
     * Downloads remote file to local target.
     */
    suspend fun downloadFile(remoteFileName: String, targetLocalFile: File): Boolean

    /**
     * Deletes a remote file.
     */
    suspend fun deleteRemoteFile(remoteFileName: String): Boolean
}
