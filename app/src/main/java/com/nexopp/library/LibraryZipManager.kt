package com.nexopp.library

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Result of a library export operation.
 */
data class LibraryExportResult(
    val success: Boolean,
    val totalNotebooks: Int,
    val totalFolders: Int,
    val zipSizeBytes: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Result of a library import operation.
 */
data class LibraryImportResult(
    val success: Boolean,
    val importedNotebooksCount: Int,
    val importedFoldersCount: Int,
    val skippedFilesCount: Int = 0,
    val skippedFiles: List<String> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Manages full library export to human-readable ZIP files and importing from ZIP files.
 * Preserves complete subject folder hierarchy, tags, metadata, and .xopp documents.
 * Also supports importing user-created ZIP archives containing folders and .xopp files.
 */
object LibraryZipManager {

    private const val MANIFEST_FILENAME = "manifest.json"
    private const val REGISTRY_FILENAME = "library_registry.json"
    private const val EXPORT_FORMAT_VERSION = 1
    private const val APP_NAME = "FiXmy Notes"

    /**
     * Sanitizes a folder or file name segment by replacing invalid filesystem characters.
     */
    fun sanitizeSegment(rawName: String): String {
        val cleaned = rawName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return if (cleaned.isBlank()) "Sin_nombre" else cleaned
    }

    /**
     * Exports the entire library to an output stream (e.g. from SAF Uri).
     */
    fun exportLibrary(
        store: LibraryStore,
        outputStream: OutputStream
    ): LibraryExportResult {
        val subjects = store.loadSubjects()
        val notebooks = store.loadNotebooks()
        val tags = store.loadTags()

        // Map each subject to its sanitized relative folder path
        val subjectPathMap = mutableMapOf<String, String>()
        for (subject in subjects) {
            val chain = subject.getParentChain(subjects)
            val path = chain.joinToString("/") { sanitizeSegment(it.name) }
            subjectPathMap[subject.id] = path
        }

        val timestamp = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        var exportedCount = 0
        val usedZipPaths = mutableSetOf<String>()
        val notebookEntriesJson = JSONArray()

        try {
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                // Write each notebook file
                for (nb in notebooks) {
                    val origFile = File(store.notebooksDir, nb.fileName)
                    if (!origFile.exists() || origFile.length() == 0L) {
                        continue
                    }

                    val folderPath = subjectPathMap[nb.subjectId] ?: ""
                    val baseName = sanitizeSegment(nb.name.ifBlank { origFile.nameWithoutExtension })
                    
                    // Deduplicate file name in ZIP if multiple notebooks have the same name in the same folder
                    var candidatePath = if (folderPath.isBlank()) "$baseName.xopp" else "$folderPath/$baseName.xopp"
                    var counter = 2
                    while (usedZipPaths.contains(candidatePath)) {
                        candidatePath = if (folderPath.isBlank()) {
                            "$baseName ($counter).xopp"
                        } else {
                            "$folderPath/$baseName ($counter).xopp"
                        }
                        counter++
                    }
                    usedZipPaths.add(candidatePath)

                    // Write zip entry
                    val entry = ZipEntry(candidatePath)
                    entry.time = nb.lastModified
                    zos.putNextEntry(entry)
                    origFile.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                    exportedCount++

                    // Record entry in manifest
                    val nbJson = JSONObject().apply {
                        put("id", nb.id)
                        put("name", nb.name)
                        put("subjectId", nb.subjectId)
                        put("zipPath", candidatePath)
                        put("coverColor", nb.coverColor)
                        put("initialTemplate", nb.initialTemplate)
                        put("isFavorite", nb.isFavorite)
                        put("lastModified", nb.lastModified)
                        put("pageCount", nb.pageCount)
                        put("tagIds", JSONArray(nb.tagIds))
                        put("bookmarkedPages", JSONArray(nb.bookmarkedPages.toList()))
                    }
                    notebookEntriesJson.put(nbJson)
                }

                // 2. Write manifest.json
                val manifestJson = JSONObject().apply {
                    put("formatVersion", EXPORT_FORMAT_VERSION)
                    put("appName", APP_NAME)
                    put("timestamp", timestamp)
                    put("formattedDate", formattedDate)
                    put("totalNotebooks", exportedCount)
                    put("totalSubjects", subjects.size)
                    put("totalTags", tags.size)
                    put("notebooks", notebookEntriesJson)
                }
                zos.putNextEntry(ZipEntry(MANIFEST_FILENAME))
                zos.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 3. Write library_registry.json for 100% full fidelity
                val registryJson = JSONObject().apply {
                    val subArray = JSONArray()
                    subjects.forEach { s ->
                        val sObj = JSONObject()
                            .put("id", s.id)
                            .put("name", s.name)
                            .put("color", s.color)
                            .put("iconName", s.iconName)
                            .put("order", s.order)
                        s.parentId?.let { sObj.put("parentId", it) }
                        subArray.put(sObj)
                    }
                    val tagArray = JSONArray()
                    tags.forEach { t ->
                        tagArray.put(
                            JSONObject()
                                .put("id", t.id)
                                .put("name", t.name)
                                .put("color", t.color)
                        )
                    }
                    put("subjects", subArray)
                    put("tags", tagArray)
                    put("notebooks", notebookEntriesJson)
                }
                zos.putNextEntry(ZipEntry(REGISTRY_FILENAME))
                zos.write(registryJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            return LibraryExportResult(
                success = true,
                totalNotebooks = exportedCount,
                totalFolders = subjects.size
            )
        } catch (e: Exception) {
            return LibraryExportResult(
                success = false,
                totalNotebooks = exportedCount,
                totalFolders = subjects.size,
                errorMessage = e.localizedMessage ?: "Error al exportar la biblioteca."
            )
        }
    }

    /**
     * Helper to export to a temporary file then stream to Uri.
     */
    fun exportLibraryToUri(
        context: Context,
        store: LibraryStore,
        targetUri: Uri
    ): LibraryExportResult {
        return try {
            val outputStream = context.contentResolver.openOutputStream(targetUri)
                ?: return LibraryExportResult(false, 0, 0, errorMessage = "No se pudo abrir el archivo de destino para escritura.")
            outputStream.use { stream ->
                exportLibrary(store, stream)
            }
        } catch (e: Exception) {
            LibraryExportResult(false, 0, 0, errorMessage = e.localizedMessage ?: "Error al escribir el archivo ZIP.")
        }
    }

    /**
     * Imports a library from an input stream (e.g. from SAF Uri).
     */
    fun importLibrary(
        store: LibraryStore,
        inputStream: InputStream,
        cacheDir: File
    ): LibraryImportResult {
        // Save stream to a temp file to allow reading ZipFile entries
        val tempZip = File(cacheDir, "import_${UUID.randomUUID()}.zip")
        try {
            tempZip.outputStream().use { out ->
                inputStream.copyTo(out)
            }

            if (!tempZip.exists() || tempZip.length() < 22) {
                return LibraryImportResult(
                    success = false,
                    importedNotebooksCount = 0,
                    importedFoldersCount = 0,
                    errorMessage = "El archivo seleccionado no es un archivo ZIP válido o está vacío."
                )
            }

            return importLibraryFromZipFile(store, tempZip)
        } catch (e: Exception) {
            return LibraryImportResult(
                success = false,
                importedNotebooksCount = 0,
                importedFoldersCount = 0,
                errorMessage = e.localizedMessage ?: "Error al procesar el archivo ZIP."
            )
        } finally {
            tempZip.delete()
        }
    }

    /**
     * Imports a library from an existing ZIP file.
     */
    fun importLibraryFromZipFile(
        store: LibraryStore,
        zipFile: File
    ): LibraryImportResult {
        val existingSubjects = store.loadSubjects().toMutableList()
        val existingTags = store.loadTags().toMutableList()
        val existingNotebooks = store.loadNotebooks().toMutableList()

        var importedNotebooksCount = 0
        var importedFoldersCount = 0
        val skippedFiles = mutableListOf<String>()

        try {
            ZipFile(zipFile).use { zip ->
                val manifestEntry = zip.getEntry(MANIFEST_FILENAME)
                val registryEntry = zip.getEntry(REGISTRY_FILENAME)

                val manifestJson = manifestEntry?.let { entry ->
                    runCatching {
                        JSONObject(zip.getInputStream(entry).bufferedReader().readText())
                    }.getOrNull()
                }

                val registryJson = registryEntry?.let { entry ->
                    runCatching {
                        JSONObject(zip.getInputStream(entry).bufferedReader().readText())
                    }.getOrNull()
                }

                // 1. Process Tags if available
                val tagMap = mutableMapOf<String, String>() // oldTagId -> localTagId
                val tagsArray = registryJson?.optJSONArray("tags")
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) {
                        val tObj = tagsArray.getJSONObject(i)
                        val oldId = tObj.getString("id")
                        val tagName = tObj.getString("name")
                        val tagColor = tObj.optLong("color", 0xFF0288D1)

                        val match = existingTags.firstOrNull { it.name.equals(tagName, ignoreCase = true) }
                        if (match != null) {
                            tagMap[oldId] = match.id
                        } else {
                            val newTag = Tag(name = tagName, color = tagColor)
                            existingTags.add(newTag)
                            tagMap[oldId] = newTag.id
                        }
                    }
                }

                // 2. Map of Subject path to local Subject
                // Helper to find or create subject hierarchy by path segments (e.g. ["Cálculo", "Problemas"])
                fun getOrCreateSubjectByPath(segments: List<String>): Subject {
                    var currentParentId: String? = null
                    var currentSubject: Subject? = null

                    for (seg in segments) {
                        val cleanSeg = seg.trim()
                        if (cleanSeg.isEmpty()) continue

                        val existing = existingSubjects.firstOrNull { s ->
                            s.name.equals(cleanSeg, ignoreCase = true) && s.parentId == currentParentId
                        }

                        if (existing != null) {
                            currentSubject = existing
                            currentParentId = existing.id
                        } else {
                            val newSubject = Subject(
                                id = UUID.randomUUID().toString(),
                                name = cleanSeg,
                                parentId = currentParentId,
                                order = existingSubjects.count { it.parentId == currentParentId }
                            )
                            existingSubjects.add(newSubject)
                            importedFoldersCount++
                            currentSubject = newSubject
                            currentParentId = newSubject.id
                        }
                    }

                    return currentSubject ?: run {
                        // Fallback root folder if no segments
                        val fallback = existingSubjects.firstOrNull { it.name == "General" && it.parentId == null }
                            ?: Subject(name = "General").also {
                                existingSubjects.add(it)
                                importedFoldersCount++
                            }
                        fallback
                    }
                }

                // Pre-populate subjects from registry/manifest if present
                val subjectIdMap = mutableMapOf<String, String>() // oldSubjectId -> localSubjectId
                val subjectsArray = registryJson?.optJSONArray("subjects")
                if (subjectsArray != null) {
                    // Two passes: root subjects first, then children
                    val pendingObjs = mutableListOf<JSONObject>()
                    for (i in 0 until subjectsArray.length()) {
                        pendingObjs.add(subjectsArray.getJSONObject(i))
                    }

                    // Sort by hierarchy (null parentId first)
                    pendingObjs.sortBy { if (it.has("parentId") && !it.isNull("parentId")) 1 else 0 }

                    for (sObj in pendingObjs) {
                        val oldId = sObj.getString("id")
                        val name = sObj.getString("name")
                        val color = sObj.optLong("color", 0xFF1976D2)
                        val iconName = sObj.optString("iconName", "folder")
                        val oldParentId = if (sObj.has("parentId") && !sObj.isNull("parentId")) sObj.getString("parentId") else null
                        val newParentId = oldParentId?.let { subjectIdMap[it] }

                        val match = existingSubjects.firstOrNull { s ->
                            s.name.equals(name, ignoreCase = true) && s.parentId == newParentId
                        }

                        if (match != null) {
                            subjectIdMap[oldId] = match.id
                        } else {
                            val newSub = Subject(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                color = color,
                                iconName = iconName,
                                order = existingSubjects.count { it.parentId == newParentId },
                                parentId = newParentId
                            )
                            existingSubjects.add(newSub)
                            subjectIdMap[oldId] = newSub.id
                            importedFoldersCount++
                        }
                    }
                }

                // 3. Build manifest lookup map by zipPath
                val manifestNotebooksMap = mutableMapOf<String, JSONObject>()
                val nbArray = (manifestJson?.optJSONArray("notebooks") ?: registryJson?.optJSONArray("notebooks"))
                if (nbArray != null) {
                    for (i in 0 until nbArray.length()) {
                        val obj = nbArray.getJSONObject(i)
                        val zipPath = obj.optString("zipPath", "")
                        if (zipPath.isNotEmpty()) {
                            manifestNotebooksMap[zipPath] = obj
                        }
                    }
                }

                // 4. Iterate all entries in ZIP
                val entries = zip.entries()
                var hasAnyXopp = false

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue

                    val entryName = entry.name.replace('\\', '/')
                    if (entryName == MANIFEST_FILENAME || entryName == REGISTRY_FILENAME) {
                        continue
                    }

                    // Check if entry is a .xopp file
                    if (!entryName.endsWith(".xopp", ignoreCase = true)) {
                        skippedFiles.add(entryName)
                        continue
                    }

                    hasAnyXopp = true

                    // Validate .xopp magic header before accepting
                    val isValidXopp = zip.getInputStream(entry).use { isXoppStreamValid(it) }
                    if (!isValidXopp) {
                        skippedFiles.add("$entryName (formato no válido o corrupto)")
                        continue
                    }

                    // Generate a unique destination file name
                    val newFileName = "notebook_${UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis() % 100000}.xopp"
                    val destFile = File(store.notebooksDir, newFileName)

                    zip.getInputStream(entry).use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (entry.time > 0) {
                        destFile.setLastModified(entry.time)
                    }

                    // Determine notebook metadata
                    val manifestObj = manifestNotebooksMap[entryName]
                    val pathSegments = entryName.split("/").filter { it.isNotEmpty() }
                    val fileBaseName = File(entryName).nameWithoutExtension

                    val targetSubjectId: String
                    val notebookName: String
                    var coverColor: Long = 0xFF1E3A8A
                    var initialTemplate: String = "ruled"
                    var isFavorite: Boolean = false
                    var lastModified: Long = if (entry.time > 0) entry.time else System.currentTimeMillis()
                    var pageCount: Int = 1
                    var assignedTags: List<String> = emptyList()
                    var bookmarkedPages: Set<Int> = emptySet()

                    if (manifestObj != null) {
                        notebookName = manifestObj.optString("name", fileBaseName)
                        val oldSubId = manifestObj.optString("subjectId", "")
                        targetSubjectId = subjectIdMap[oldSubId] ?: run {
                            if (pathSegments.size > 1) {
                                getOrCreateSubjectByPath(pathSegments.dropLast(1)).id
                            } else {
                                getOrCreateSubjectByPath(emptyList()).id
                            }
                        }
                        coverColor = manifestObj.optLong("coverColor", coverColor)
                        initialTemplate = manifestObj.optString("initialTemplate", initialTemplate)
                        isFavorite = manifestObj.optBoolean("isFavorite", false)
                        lastModified = manifestObj.optLong("lastModified", lastModified)
                        pageCount = manifestObj.optInt("pageCount", 1)

                        val tArray = manifestObj.optJSONArray("tagIds")
                        if (tArray != null) {
                            val mappedT = mutableListOf<String>()
                            for (t in 0 until tArray.length()) {
                                val oldT = tArray.getString(t)
                                val newT = tagMap[oldT]
                                if (newT != null) mappedT.add(newT)
                            }
                            assignedTags = mappedT
                        }

                        val bArray = manifestObj.optJSONArray("bookmarkedPages")
                        if (bArray != null) {
                            val bSet = mutableSetOf<Int>()
                            for (b in 0 until bArray.length()) {
                                bSet.add(bArray.getInt(b))
                            }
                            bookmarkedPages = bSet
                        }
                    } else {
                        // User-created custom ZIP: deduce folder hierarchy from path segments
                        notebookName = fileBaseName
                        targetSubjectId = if (pathSegments.size > 1) {
                            getOrCreateSubjectByPath(pathSegments.dropLast(1)).id
                        } else {
                            getOrCreateSubjectByPath(emptyList()).id
                        }
                    }

                    val newNotebook = Notebook(
                        id = UUID.randomUUID().toString(),
                        subjectId = targetSubjectId,
                        name = notebookName,
                        fileName = newFileName,
                        coverColor = coverColor,
                        initialTemplate = initialTemplate,
                        isFavorite = isFavorite,
                        lastModified = lastModified,
                        pageCount = pageCount,
                        tagIds = assignedTags,
                        bookmarkedPages = bookmarkedPages
                    )

                    existingNotebooks.add(0, newNotebook)
                    importedNotebooksCount++
                }

                if (!hasAnyXopp && manifestJson == null) {
                    return LibraryImportResult(
                        success = false,
                        importedNotebooksCount = 0,
                        importedFoldersCount = 0,
                        skippedFilesCount = skippedFiles.size,
                        skippedFiles = skippedFiles,
                        errorMessage = "No se encontraron cuadernos .xopp válidos en el archivo ZIP."
                    )
                }
            }

            // Save merged store
            store.save(
                subjects = existingSubjects,
                notebooks = existingNotebooks,
                tags = existingTags
            )

            return LibraryImportResult(
                success = true,
                importedNotebooksCount = importedNotebooksCount,
                importedFoldersCount = importedFoldersCount,
                skippedFilesCount = skippedFiles.size,
                skippedFiles = skippedFiles
            )
        } catch (e: Exception) {
            return LibraryImportResult(
                success = false,
                importedNotebooksCount = importedNotebooksCount,
                importedFoldersCount = importedFoldersCount,
                skippedFilesCount = skippedFiles.size,
                skippedFiles = skippedFiles,
                errorMessage = e.localizedMessage ?: "Error al importar los archivos de la biblioteca."
            )
        }
    }

    /**
     * Checks whether an input stream begins with valid .xopp header bytes:
     * - GZIP magic bytes: 0x1F, 0x8B
     * - XML magic characters: <?xml or <xournal
     */
    fun isXoppStreamValid(stream: InputStream): Boolean {
        val buffer = ByteArray(16)
        val read = stream.read(buffer)
        if (read < 2) return false

        // 1. Check GZIP magic (0x1F, 0x8B)
        if (buffer[0] == 0x1F.toByte() && buffer[1] == 0x8B.toByte()) {
            return true
        }

        // 2. Check XML header
        val headerText = String(buffer, 0, read, Charsets.UTF_8).trimStart()
        return headerText.startsWith("<?xml") || headerText.startsWith("<xournal")
    }
}
