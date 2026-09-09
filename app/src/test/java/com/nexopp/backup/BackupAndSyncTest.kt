package com.nexopp.backup

import com.nexopp.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupAndSyncTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var notebooksDir: File
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        baseDir = tempFolder.newFolder("nexopp_data")
        notebooksDir = File(baseDir, "notebooks").apply { mkdirs() }
        backupManager = BackupManager(baseDir, maxLocalBackups = 3)
    }

    @Test
    fun testCreateAndListBackup() {
        // Create sample notebook
        val nbFile = File(notebooksDir, "calculo.xopp")
        nbFile.writeText("<xopp>sample content</xopp>")

        val backupInfo = backupManager.createBackup("test_backup")
        assertTrue(backupInfo.file.exists())
        assertTrue(backupInfo.sizeBytes > 0)
        assertEquals(1, backupInfo.manifest?.totalNotebooks)

        val list = backupManager.listBackups()
        assertEquals(1, list.size)
        assertEquals(backupInfo.file.name, list[0].file.name)
    }

    @Test
    fun testBackupRetentionLimit() {
        val nbFile = File(notebooksDir, "algebra.xopp")
        nbFile.writeText("<xopp>sample</xopp>")

        for (i in 1..5) {
            backupManager.createBackup("backup_$i")
            Thread.sleep(20) // Ensure distinct timestamps
        }

        val list = backupManager.listBackups()
        // Maximum local backups configured is 3
        assertEquals(3, list.size)
    }

    @Test
    fun testRestoreBackupWithPolicies() {
        val nbFile = File(notebooksDir, "fisica.xopp")
        nbFile.writeText("versión original 1")

        val backup = backupManager.createBackup("fisica_v1")

        // Modify local file
        nbFile.writeText("versión modificada local 2")

        // 1. Restore with RENAME_DUPLICATES policy
        val renameResult = backupManager.restoreBackup(backup.file, RestoreConflictPolicy.RENAME_DUPLICATES)
        assertTrue(renameResult.success)
        assertEquals(1, renameResult.renamedCount)

        // Verify both files exist
        val xoppFiles = notebooksDir.listFiles { f -> f.extension == "xopp" } ?: emptyArray()
        assertEquals(2, xoppFiles.size)

        // 2. Restore with OVERWRITE policy
        val overwriteResult = backupManager.restoreBackup(backup.file, RestoreConflictPolicy.OVERWRITE)
        assertTrue(overwriteResult.success)
        assertEquals("versión original 1", nbFile.readText())
    }

    @Test
    fun testLocalFolderTwoWaySync() = runBlocking {
        val syncFolder = tempFolder.newFolder("sync_target")
        val provider = LocalFolderSyncProvider(syncFolder)
        val engine = SyncEngine(notebooksDir)

        // Create local notebook
        val localNb = File(notebooksDir, "quimica.xopp")
        localNb.writeText("Contenido química")

        val config = SyncConfig(providerType = SyncProviderType.LOCAL_FOLDER, preserveBothOnConflict = true)

        // 1st Sync: should upload localNb to syncFolder
        val res1 = engine.performSync(provider, config)
        assertTrue(res1.success)
        assertEquals(1, res1.uploadedCount)
        assertTrue(File(syncFolder, "quimica.xopp").exists())

        // Create a remote notebook in syncFolder
        val remoteNb = File(syncFolder, "termodinamica.xopp")
        remoteNb.writeText("Contenido termo")

        // 2nd Sync: should download remoteNb into notebooksDir
        val res2 = engine.performSync(provider, config)
        assertTrue(res2.success)
        assertEquals(1, res2.downloadedCount)
        assertTrue(File(notebooksDir, "termodinamica.xopp").exists())
    }

    @Test
    fun testSyncConflictCreatesConflictCopy() = runBlocking {
        val syncFolder = tempFolder.newFolder("sync_target_conflict")
        val provider = LocalFolderSyncProvider(syncFolder)
        val engine = SyncEngine(notebooksDir)

        val localNb = File(notebooksDir, "apuntes.xopp")
        localNb.writeText("Versión base")

        val config = SyncConfig(providerType = SyncProviderType.LOCAL_FOLDER, preserveBothOnConflict = true)

        // Initial sync
        engine.performSync(provider, config)

        // Modify both ends independently
        Thread.sleep(100)
        localNb.writeText("Versión editada en tablet A")
        val remoteNb = File(syncFolder, "apuntes.xopp")
        remoteNb.writeText("Versión editada en tablet B")
        remoteNb.setLastModified(System.currentTimeMillis() + 10000)

        // Sync with conflict protection
        val syncRes = engine.performSync(provider, config)
        assertEquals(1, syncRes.conflictsCount)

        // Verify conflict copy was created and zero data was lost
        val allFiles = notebooksDir.listFiles { f -> f.extension == "xopp" } ?: emptyArray()
        assertTrue(allFiles.any { it.name.contains("conflicto") })
    }
}
