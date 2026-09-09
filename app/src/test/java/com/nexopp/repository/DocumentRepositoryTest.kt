package com.nexopp.repository

import com.nexopp.format.model.Background
import com.nexopp.format.model.Document
import com.nexopp.format.model.Layer
import com.nexopp.format.model.Page
import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import com.nexopp.format.model.Tool
import com.nexopp.sync.RemoteFileInfo
import com.nexopp.sync.SyncConfig
import com.nexopp.sync.SyncConnectionResult
import com.nexopp.sync.SyncEngine
import com.nexopp.sync.SyncProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DocumentRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var repo: LocalDocumentRepository

    @Before
    fun setup() {
        baseDir = tempFolder.newFolder("repo_test")
        repo = LocalDocumentRepository(baseDir, libraryStore = null)
    }

    private fun createSampleDoc(title: String = "Test Doc"): Document {
        val stroke = Stroke(
            tool = Tool.PEN,
            color = 0xFF000000.toInt(),
            capStyle = null,
            points = listOf(
                StrokePoint(10.0, 10.0, 2.0),
                StrokePoint(20.0, 20.0, 2.0),
                StrokePoint(30.0, 30.0, 2.0)
            ),
            uniformWidth = true
        )
        val layer = Layer(elements = listOf(stroke))
        val page = Page(
            width = 595.0,
            height = 842.0,
            background = Background.Solid(color = 0xFFFFFFFF.toInt(), style = "plain"),
            layers = listOf(layer)
        )
        return Document(
            title = title,
            pages = listOf(page)
        )
    }

    @Test
    fun testSaveAndLoadDocument() = runBlocking {
        val doc = createSampleDoc("Algebra Lineal")
        val saveResult = repo.saveDocument("algebra.xopp", doc)
        assertTrue(saveResult.isSuccess)
        val file = saveResult.getOrThrow()
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        val loadResult = repo.loadDocument("algebra.xopp")
        assertTrue(loadResult.isSuccess)
        val loaded = loadResult.getOrThrow()
        assertEquals(1, loaded.pages.size)
        assertEquals(1, loaded.pages[0].layers[0].elements.size)
    }

    @Test
    fun testListDocumentsAndMetadata() = runBlocking {
        val doc1 = createSampleDoc("Calculo I")
        val doc2 = createSampleDoc("Fisica I")

        repo.saveDocument("calculo.xopp", doc1)
        repo.saveDocument("fisica.xopp", doc2)

        val meta1 = repo.getDocumentMetadata("calculo.xopp")
        assertNotNull(meta1)
        assertEquals("calculo.xopp", meta1?.identity?.fileName)

        val list = repo.listDocuments(includeTrashed = false)
        assertEquals(2, list.size)
        assertTrue(list.any { it.identity.fileName == "calculo.xopp" })
        assertTrue(list.any { it.identity.fileName == "fisica.xopp" })
    }

    @Test
    fun testRenameDocumentWithSidecars() = runBlocking {
        val doc = createSampleDoc("Quimica")
        repo.saveDocument("quimica.xopp", doc)

        // Create mock sidecars
        val structureFile = File(baseDir, "quimica.structure.json")
        structureFile.writeText("{\"sections\":[]}")
        val attachmentsMeta = File(baseDir, "quimica.attachments.json")
        attachmentsMeta.writeText("[]")
        val attachmentsDir = File(baseDir, "attachments_quimica")
        attachmentsDir.mkdirs()
        File(attachmentsDir, "sample.pdf").writeText("pdf data")

        val renameResult = repo.renameDocument("quimica.xopp", "quimica_avanzada.xopp")
        assertTrue(renameResult.isSuccess)

        assertFalse(File(baseDir, "quimica.xopp").exists())
        assertTrue(File(baseDir, "quimica_avanzada.xopp").exists())

        assertFalse(structureFile.exists())
        assertTrue(File(baseDir, "quimica_avanzada.structure.json").exists())

        assertFalse(attachmentsMeta.exists())
        assertTrue(File(baseDir, "quimica_avanzada.attachments.json").exists())

        assertFalse(attachmentsDir.exists())
        assertTrue(File(baseDir, "attachments_quimica_avanzada").exists())
        assertTrue(File(baseDir, "attachments_quimica_avanzada/sample.pdf").exists())
    }

    @Test
    fun testContentHash() = runBlocking {
        val doc = createSampleDoc("Hash Test")
        repo.saveDocument("hash_test.xopp", doc)

        val hash1 = repo.calculateContentHash("hash_test.xopp")
        assertTrue(hash1.isNotEmpty())
        assertEquals(64, hash1.length) // SHA-256 hex string length

        val hash2 = repo.calculateContentHash("hash_test.xopp")
        assertEquals(hash1, hash2)
    }

    @Test
    fun testSyncRepositoryConflictFork() = runBlocking {
        val doc = createSampleDoc("Sync Local")
        repo.saveDocument("sync_test.xopp", doc)

        val syncEngine = SyncEngine(baseDir)
        val syncRepo = DefaultSyncRepository(repo, syncEngine)

        // Mock sync provider
        val mockProvider = object : SyncProvider {
            override val id: String = "mock"
            override val displayName: String = "Mock Provider"
            override suspend fun testConnection(): SyncConnectionResult = SyncConnectionResult(true, "OK")
            override suspend fun listRemoteFiles(): List<RemoteFileInfo> = listOf(
                RemoteFileInfo(
                    filename = "sync_test.xopp",
                    remotePath = "sync_test.xopp",
                    sizeBytes = 100,
                    lastModified = System.currentTimeMillis()
                )
            )
            override suspend fun uploadFile(localFile: File, remoteFileName: String): Boolean = true
            override suspend fun downloadFile(remoteFileName: String, targetLocalFile: File): Boolean {
                targetLocalFile.writeText("REMOTE_CONTENT_MOCK")
                return true
            }
            override suspend fun deleteRemoteFile(remoteFileName: String): Boolean = true
        }

        val config = SyncConfig(autoSyncEnabled = false)

        val resolved = syncRepo.resolveConflict(
            fileName = "sync_test.xopp",
            resolution = ConflictResolution.KEEP_BOTH_FORK,
            provider = mockProvider,
            config = config
        )

        assertTrue(resolved)

        // Check that a fork copy was created and original was updated with remote
        val files = baseDir.listFiles() ?: emptyArray()
        val forkFiles = files.filter { it.name.startsWith("sync_test_copia_local_") }
        assertEquals(1, forkFiles.size)
        assertTrue(forkFiles[0].length() > 0)

        val originalFile = File(baseDir, "sync_test.xopp")
        assertEquals("REMOTE_CONTENT_MOCK", originalFile.readText())
    }
}
