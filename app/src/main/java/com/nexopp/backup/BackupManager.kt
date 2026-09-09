package com.nexopp.backup

import com.nexopp.library.LibraryStore
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Robust local backup and archive manager for NeXopp notebooks, tags, and metadata.
 * Produces clean portable zip packages (.nxbackup) and restores with full conflict protection.
 */
class BackupManager(
    private val baseDir: File,
    private val maxLocalBackups: Int = 10
) {
    private val backupsDir = File(baseDir, "backups").apply { mkdirs() }
    private val notebooksDir = File(baseDir, "notebooks").apply { mkdirs() }
    private val registryFile = File(baseDir, "library_registry.json")

    /**
     * Creates a complete local backup of all notebooks and registry.
     * @param backupName Optional custom name; defaults to timestamp-based filename.
     */
    fun createBackup(backupName: String? = null): BackupInfo {
        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
        val filename = if (!backupName.isNullOrBlank()) {
            "backup_${backupName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}_$dateStr.nxbackup"
        } else {
            "nexopp_backup_$dateStr.nxbackup"
        }

        val targetFile = File(backupsDir, filename)
        val notebookFiles = notebooksDir.listFiles { f -> f.isFile && f.extension == "xopp" } ?: emptyArray()

        // Read registry for counts
        var subjectCount = 0
        var tagCount = 0
        var notebookCount = notebookFiles.size
        if (registryFile.exists()) {
            try {
                val json = JSONObject(registryFile.readText())
                subjectCount = json.optJSONArray("subjects")?.length() ?: 0
                tagCount = json.optJSONArray("tags")?.length() ?: 0
            } catch (_: Exception) {}
        }

        val manifest = BackupManifest(
            formatVersion = 1,
            timestamp = timestamp,
            totalNotebooks = notebookCount,
            totalSubjects = subjectCount,
            totalTags = tagCount
        )

        ZipOutputStream(BufferedOutputStream(FileOutputStream(targetFile))).use { zos ->
            // 1. Write manifest.json
            val manifestJson = JSONObject().apply {
                put("formatVersion", manifest.formatVersion)
                put("appVersion", manifest.appVersion)
                put("timestamp", manifest.timestamp)
                put("totalNotebooks", manifest.totalNotebooks)
                put("totalSubjects", manifest.totalSubjects)
                put("totalTags", manifest.totalTags)
                put("deviceName", manifest.deviceName)
            }
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Write library_registry.json
            if (registryFile.exists()) {
                zos.putNextEntry(ZipEntry("library_registry.json"))
                registryFile.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }

            // 3. Write all .xopp notebooks
            for (nbFile in notebookFiles) {
                zos.putNextEntry(ZipEntry("notebooks/${nbFile.name}"))
                nbFile.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        // Apply retention pruning
        pruneOldBackups()

        return BackupInfo(
            file = targetFile,
            timestamp = timestamp,
            sizeBytes = targetFile.length(),
            manifest = manifest
        )
    }

    /**
     * Lists all available local backup archives ordered newest first.
     */
    fun listBackups(): List<BackupInfo> {
        val files = backupsDir.listFiles { f -> f.isFile && (f.extension == "nxbackup" || f.extension == "zip") } ?: return emptyList()
        return files.map { file ->
            val manifest = readManifest(file)
            BackupInfo(
                file = file,
                timestamp = manifest?.timestamp ?: file.lastModified(),
                sizeBytes = file.length(),
                manifest = manifest
            )
        }.sortedByDescending { it.timestamp }
    }

    /**
     * Reads manifest from a backup zip archive.
     */
    fun readManifest(backupFile: File): BackupManifest? {
        if (!backupFile.exists() || !backupFile.canRead()) return null
        return try {
            ZipFile(backupFile).use { zip ->
                val entry = zip.getEntry("manifest.json") ?: return null
                val text = zip.getInputStream(entry).bufferedReader().readText()
                val json = JSONObject(text)
                BackupManifest(
                    formatVersion = json.optInt("formatVersion", 1),
                    appVersion = json.optString("appVersion", "1.0"),
                    timestamp = json.optLong("timestamp", backupFile.lastModified()),
                    totalNotebooks = json.optInt("totalNotebooks", 0),
                    totalSubjects = json.optInt("totalSubjects", 0),
                    totalTags = json.optInt("totalTags", 0),
                    deviceName = json.optString("deviceName", "Dispositivo")
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Restores notes and metadata from a backup archive according to the selected policy.
     */
    fun restoreBackup(backupFile: File, policy: RestoreConflictPolicy = RestoreConflictPolicy.KEEP_NEWER): RestoreResult {
        if (!backupFile.exists()) {
            return RestoreResult(false, 0, 0, 0, "El archivo de copia de seguridad no existe.")
        }

        var restoredCount = 0
        var skippedCount = 0
        var renamedCount = 0

        try {
            ZipFile(backupFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue

                    when {
                        entry.name == "library_registry.json" -> {
                            // Extract to temp then merge / replace registry
                            val tempRegistry = File(baseDir, "library_registry_restored.json")
                            zip.getInputStream(entry).use { input ->
                                tempRegistry.outputStream().use { output -> input.copyTo(output) }
                            }
                            if (policy == RestoreConflictPolicy.OVERWRITE || !registryFile.exists()) {
                                tempRegistry.renameTo(registryFile)
                            } else {
                                mergeRegistries(registryFile, tempRegistry)
                                tempRegistry.delete()
                            }
                        }

                        entry.name.startsWith("notebooks/") -> {
                            val nbName = File(entry.name).name
                            val destFile = File(notebooksDir, nbName)
                            var targetFile = destFile

                            if (destFile.exists()) {
                                when (policy) {
                                    RestoreConflictPolicy.OVERWRITE -> {
                                        // Overwrite existing
                                        restoredCount++
                                    }
                                    RestoreConflictPolicy.KEEP_NEWER -> {
                                        if (entry.time > destFile.lastModified()) {
                                            restoredCount++
                                        } else {
                                            skippedCount++
                                            continue
                                        }
                                    }
                                    RestoreConflictPolicy.RENAME_DUPLICATES -> {
                                        val baseName = destFile.nameWithoutExtension
                                        targetFile = File(notebooksDir, "${baseName}_restaurado_${System.currentTimeMillis() % 10000}.xopp")
                                        renamedCount++
                                    }
                                }
                            } else {
                                restoredCount++
                            }

                            zip.getInputStream(entry).use { input ->
                                targetFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            if (entry.time > 0) targetFile.setLastModified(entry.time)
                        }
                    }
                }
            }

            return RestoreResult(
                success = true,
                restoredNotebooksCount = restoredCount,
                skippedCount = skippedCount,
                renamedCount = renamedCount
            )
        } catch (e: Exception) {
            return RestoreResult(false, restoredCount, skippedCount, renamedCount, e.localizedMessage)
        }
    }

    /**
     * Deletes a local backup file.
     */
    fun deleteBackup(backupInfo: BackupInfo): Boolean {
        return backupInfo.file.delete()
    }

    /**
     * Prunes oldest backups beyond the retention limit.
     */
    private fun pruneOldBackups() {
        val backups = listBackups()
        if (backups.size > maxLocalBackups) {
            val toDelete = backups.drop(maxLocalBackups)
            for (b in toDelete) {
                b.file.delete()
            }
        }
    }

    private fun mergeRegistries(currentRegistry: File, restoredRegistry: File) {
        try {
            val currentJson = if (currentRegistry.exists()) JSONObject(currentRegistry.readText()) else JSONObject()
            val restoredJson = JSONObject(restoredRegistry.readText())

            // Merge tags
            val currentTags = currentJson.optJSONArray("tags") ?: org.json.JSONArray()
            val restoredTags = restoredJson.optJSONArray("tags") ?: org.json.JSONArray()
            val existingTagIds = mutableSetOf<String>()
            for (i in 0 until currentTags.length()) {
                existingTagIds.add(currentTags.getJSONObject(i).getString("id"))
            }
            for (i in 0 until restoredTags.length()) {
                val tagObj = restoredTags.getJSONObject(i)
                if (!existingTagIds.contains(tagObj.getString("id"))) {
                    currentTags.put(tagObj)
                }
            }
            currentJson.put("tags", currentTags)

            // Merge subjects
            val currentSubjects = currentJson.optJSONArray("subjects") ?: org.json.JSONArray()
            val restoredSubjects = restoredJson.optJSONArray("subjects") ?: org.json.JSONArray()
            val existingSubjectIds = mutableSetOf<String>()
            for (i in 0 until currentSubjects.length()) {
                existingSubjectIds.add(currentSubjects.getJSONObject(i).getString("id"))
            }
            for (i in 0 until restoredSubjects.length()) {
                val subObj = restoredSubjects.getJSONObject(i)
                if (!existingSubjectIds.contains(subObj.getString("id"))) {
                    currentSubjects.put(subObj)
                }
            }
            currentJson.put("subjects", currentSubjects)

            currentRegistry.writeText(currentJson.toString(2))
        } catch (_: Exception) {}
    }
}
