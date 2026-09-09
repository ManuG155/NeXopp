package com.nexopp.repository

import com.nexopp.sync.SyncConfig
import com.nexopp.sync.SyncEngine
import com.nexopp.sync.SyncProvider
import com.nexopp.sync.SyncResult
import com.nexopp.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_REMOTE,
    KEEP_BOTH_FORK
}

/**
 * Interface defining sync operations for multi-device collaboration (Tablet, PC, Cloud).
 */
interface SyncRepository {
    val syncStatus: Flow<SyncStatus>
    suspend fun synchronize(
        provider: SyncProvider,
        config: SyncConfig,
        onProgress: (String) -> Unit = {}
    ): SyncResult
    
    suspend fun resolveConflict(
        fileName: String,
        resolution: ConflictResolution,
        provider: SyncProvider,
        config: SyncConfig
    ): Boolean
}

/**
 * Default implementation of [SyncRepository] wrapping [SyncEngine] and [DocumentRepository].
 */
class DefaultSyncRepository(
    private val documentRepository: DocumentRepository,
    private val syncEngine: SyncEngine
) : SyncRepository {

    private val _status = MutableStateFlow(SyncStatus.IDLE)
    override val syncStatus: Flow<SyncStatus> = _status.asStateFlow()

    override suspend fun synchronize(
        provider: SyncProvider,
        config: SyncConfig,
        onProgress: (String) -> Unit
    ): SyncResult {
        _status.value = SyncStatus.SYNCING
        val result = try {
            syncEngine.performSync(provider, config, onProgress)
        } catch (e: Exception) {
            SyncResult(
                success = false,
                errorMessage = e.localizedMessage,
                logs = listOf("Excepción durante la sincronización: ${e.localizedMessage}")
            )
        }
        _status.value = when {
            !result.success -> SyncStatus.ERROR
            result.conflictsCount > 0 -> SyncStatus.CONFLICTS_DETECTED
            else -> SyncStatus.SUCCESS
        }
        return result
    }

    override suspend fun resolveConflict(
        fileName: String,
        resolution: ConflictResolution,
        provider: SyncProvider,
        config: SyncConfig
    ): Boolean {
        val file = documentRepository.getDocumentFile(fileName)
        if (!file.exists()) return false

        return when (resolution) {
            ConflictResolution.KEEP_LOCAL -> {
                provider.uploadFile(file, fileName)
            }
            ConflictResolution.KEEP_REMOTE -> {
                provider.downloadFile(fileName, file)
            }
            ConflictResolution.KEEP_BOTH_FORK -> {
                // Duplicate local file with conflict suffix then fetch remote
                val base = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "xopp")
                val forkName = "${base}_copia_local_${System.currentTimeMillis()}.$ext"
                val forkFile = documentRepository.getDocumentFile(forkName)
                file.copyTo(forkFile, overwrite = true)
                provider.downloadFile(fileName, file)
            }
        }
    }
}
