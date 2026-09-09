package com.nexopp.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Sync provider for local external directories, SD cards, or Syncthing synchronized folders.
 */
class LocalFolderSyncProvider(
    private val syncFolder: File
) : SyncProvider {

    override val id: String = "local_folder"
    override val displayName: String = "Carpeta Local / Syncthing"

    override suspend fun testConnection(): SyncConnectionResult = withContext(Dispatchers.IO) {
        if (!syncFolder.exists()) {
            val created = syncFolder.mkdirs()
            if (!created) {
                return@withContext SyncConnectionResult(false, "No se puede crear o acceder a la carpeta: ${syncFolder.absolutePath}")
            }
        }
        if (!syncFolder.canRead() || !syncFolder.canWrite()) {
            return@withContext SyncConnectionResult(false, "Permisos insuficientes de lectura/escritura en la carpeta.")
        }
        SyncConnectionResult(true, "Acceso a la carpeta verificado correctamente.")
    }

    override suspend fun listRemoteFiles(): List<RemoteFileInfo> = withContext(Dispatchers.IO) {
        if (!syncFolder.exists()) return@withContext emptyList()
        val files = syncFolder.listFiles { f -> f.isFile && f.extension == "xopp" } ?: emptyArray()
        files.map { f ->
            RemoteFileInfo(
                filename = f.name,
                remotePath = f.absolutePath,
                sizeBytes = f.length(),
                lastModified = f.lastModified()
            )
        }
    }

    override suspend fun uploadFile(localFile: File, remoteFileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!syncFolder.exists()) syncFolder.mkdirs()
            val target = File(syncFolder, remoteFileName)
            localFile.copyTo(target, overwrite = true)
            target.setLastModified(localFile.lastModified())
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun downloadFile(remoteFileName: String, targetLocalFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val source = File(syncFolder, remoteFileName)
            if (!source.exists()) return@withContext false
            source.copyTo(targetLocalFile, overwrite = true)
            targetLocalFile.setLastModified(source.lastModified())
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteRemoteFile(remoteFileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = File(syncFolder, remoteFileName)
            if (target.exists()) target.delete() else true
        } catch (_: Exception) {
            false
        }
    }
}
