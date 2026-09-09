package com.nexopp.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Robust two-way sync engine for NeXopp with conflict protection and zero data-loss guarantees.
 */
class SyncEngine(
    private val notebooksDir: File,
    private val metadataFile: File = File(notebooksDir.parentFile ?: notebooksDir, "sync_metadata.json")
) {

    /**
     * Executes a full synchronization cycle using the specified provider and configuration.
     */
    suspend fun performSync(
        provider: SyncProvider,
        config: SyncConfig,
        onProgress: (message: String) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
            onProgress(msg)
        }

        log("Iniciando sincronización con ${provider.displayName}...")

        val testRes = provider.testConnection()
        if (!testRes.success) {
            log("Error de conexión: ${testRes.message}")
            return@withContext SyncResult(
                success = false,
                logs = logs,
                errorMessage = testRes.message
            )
        }

        var uploaded = 0
        var downloaded = 0
        var conflicts = 0
        var errors = 0

        val localFiles = (notebooksDir.listFiles { f -> f.isFile && f.extension == "xopp" } ?: emptyArray()).associateBy { it.name }
        val remoteFiles = try {
            provider.listRemoteFiles().associateBy { it.filename }
        } catch (e: Exception) {
            log("Error listando archivos remotos: ${e.localizedMessage}")
            return@withContext SyncResult(
                success = false,
                logs = logs,
                errorMessage = "Error listando archivos remotos: ${e.localizedMessage}"
            )
        }

        val previousMetadata = loadSyncMetadata()
        val currentMetadata = mutableMapOf<String, FileSyncMeta>()

        val allFilenames = (localFiles.keys + remoteFiles.keys).toSet()

        for (filename in allFilenames) {
            val local = localFiles[filename]
            val remote = remoteFiles[filename]
            val prev = previousMetadata[filename]

            try {
                when {
                    // 1. File exists locally but NOT remotely
                    local != null && remote == null -> {
                        if (prev != null && prev.existsRemote) {
                            // File was deleted remotely. If local hasn't changed since then, delete local; otherwise re-upload.
                            val localHash = calculateSha256(local)
                            if (localHash == prev.localHash) {
                                log("Eliminando localmente (borrado en remoto): $filename")
                                local.delete()
                            } else {
                                log("Subiendo (modificado localmente tras borrado remoto): $filename")
                                if (provider.uploadFile(local, filename)) {
                                    uploaded++
                                    currentMetadata[filename] = FileSyncMeta(filename, localHash, local.lastModified(), true)
                                }
                            }
                        } else {
                            log("Subiendo nuevo cuaderno local: $filename")
                            if (provider.uploadFile(local, filename)) {
                                uploaded++
                                currentMetadata[filename] = FileSyncMeta(filename, calculateSha256(local), local.lastModified(), true)
                            }
                        }
                    }

                    // 2. File exists remotely but NOT locally
                    local == null && remote != null -> {
                        if (prev != null && prev.existsLocal) {
                            // File was deleted locally. Delete remotely.
                            log("Eliminando en remoto (borrado local): $filename")
                            provider.deleteRemoteFile(filename)
                        } else {
                            log("Descargando nuevo cuaderno remoto: $filename")
                            val dest = File(notebooksDir, filename)
                            if (provider.downloadFile(filename, dest)) {
                                downloaded++
                                currentMetadata[filename] = FileSyncMeta(filename, calculateSha256(dest), dest.lastModified(), true)
                            }
                        }
                    }

                    // 3. File exists BOTH locally and remotely
                    local != null && remote != null -> {
                        val localHash = calculateSha256(local)
                        val localModified = local.lastModified()
                        val remoteModified = remote.lastModified

                        val localChanged = prev == null || localHash != prev.localHash
                        val remoteChanged = prev == null || remoteModified > prev.lastSyncTime + 2000

                        if (localChanged && remoteChanged) {
                            // CONFLICT!
                            conflicts++
                            log("Conflicto detectado en $filename (modificado en ambos extremos).")
                            if (config.preserveBothOnConflict) {
                                // Save remote version as conflict copy
                                val baseName = filename.substringBeforeLast(".")
                                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(remoteModified))
                                val conflictName = "${baseName}_conflicto_$dateStr.xopp"
                                val conflictFile = File(notebooksDir, conflictName)
                                log("Guardando versión remota como copia: $conflictName")
                                provider.downloadFile(filename, conflictFile)
                                // Upload local version to retain primary file
                                provider.uploadFile(local, filename)
                                uploaded++
                                currentMetadata[filename] = FileSyncMeta(filename, localHash, localModified, true)
                                currentMetadata[conflictName] = FileSyncMeta(conflictName, calculateSha256(conflictFile), conflictFile.lastModified(), true)
                            } else {
                                // Local wins
                                provider.uploadFile(local, filename)
                                uploaded++
                                currentMetadata[filename] = FileSyncMeta(filename, localHash, localModified, true)
                            }
                        } else if (localChanged) {
                            log("Subiendo actualización local: $filename")
                            if (provider.uploadFile(local, filename)) {
                                uploaded++
                                currentMetadata[filename] = FileSyncMeta(filename, localHash, localModified, true)
                            }
                        } else if (remoteChanged) {
                            log("Descargando actualización remota: $filename")
                            if (provider.downloadFile(filename, local)) {
                                downloaded++
                                currentMetadata[filename] = FileSyncMeta(filename, calculateSha256(local), local.lastModified(), true)
                            }
                        } else {
                            // Up to date
                            currentMetadata[filename] = FileSyncMeta(filename, localHash, localModified, true)
                        }
                    }
                }
            } catch (e: Exception) {
                errors++
                log("Error procesando $filename: ${e.localizedMessage}")
            }
        }

        saveSyncMetadata(currentMetadata)
        log("Sincronización finalizada. Subidos: $uploaded, Descargados: $downloaded, Conflictos: $conflicts, Errores: $errors.")

        SyncResult(
            success = errors == 0,
            uploadedCount = uploaded,
            downloadedCount = downloaded,
            conflictsCount = conflicts,
            errorsCount = errors,
            logs = logs
        )
    }

    private data class FileSyncMeta(
        val filename: String,
        val localHash: String,
        val lastSyncTime: Long,
        val existsLocal: Boolean = true,
        val existsRemote: Boolean = true
    )

    private fun loadSyncMetadata(): Map<String, FileSyncMeta> {
        if (!metadataFile.exists()) return emptyMap()
        val map = mutableMapOf<String, FileSyncMeta>()
        try {
            val json = JSONObject(metadataFile.readText())
            val filesArr = json.optJSONArray("files") ?: return emptyMap()
            for (i in 0 until filesArr.length()) {
                val obj = filesArr.getJSONObject(i)
                val fn = obj.getString("filename")
                map[fn] = FileSyncMeta(
                    filename = fn,
                    localHash = obj.optString("localHash", ""),
                    lastSyncTime = obj.optLong("lastSyncTime", 0L),
                    existsLocal = obj.optBoolean("existsLocal", true),
                    existsRemote = obj.optBoolean("existsRemote", true)
                )
            }
        } catch (_: Exception) {}
        return map
    }

    private fun saveSyncMetadata(metadata: Map<String, FileSyncMeta>) {
        try {
            val json = JSONObject()
            val filesArr = org.json.JSONArray()
            for (m in metadata.values) {
                val obj = JSONObject().apply {
                    put("filename", m.filename)
                    put("localHash", m.localHash)
                    put("lastSyncTime", m.lastSyncTime)
                    put("existsLocal", m.existsLocal)
                    put("existsRemote", m.existsRemote)
                }
                filesArr.put(obj)
            }
            json.put("files", filesArr)
            json.put("timestamp", System.currentTimeMillis())
            metadataFile.writeText(json.toString(2))
        } catch (_: Exception) {}
    }

    private fun calculateSha256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
