package com.nexopp.library

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class LibraryZipManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storeDirA: File
    private lateinit var storeDirB: File
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        storeDirA = tempFolder.newFolder("storeA")
        storeDirB = tempFolder.newFolder("storeB")
        cacheDir = tempFolder.newFolder("cache")
    }

    private fun createDummyXoppFile(dir: File, fileName: String, xmlContent: String = "<xournal><page/></xournal>"): File {
        val file = File(dir, fileName)
        GZIPOutputStream(FileOutputStream(file)).use { gzip ->
            gzip.write(xmlContent.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    @Test
    fun exportLibrary_createsStructuredZip_withReadableHierarchyAndManifest() {
        val storeA = LibraryStore(storeDirA)

        val subCalc = Subject(id = "sub-calc", name = "Cálculo", parentId = null)
        val subProb = Subject(id = "sub-prob", name = "Problemas", parentId = "sub-calc")
        val subFisica = Subject(id = "sub-fis", name = "Física", parentId = null)

        val nbFile1 = createDummyXoppFile(storeA.notebooksDir, "nb_1.xopp")
        val nbFile2 = createDummyXoppFile(storeA.notebooksDir, "nb_2.xopp")
        val nbFile3 = createDummyXoppFile(storeA.notebooksDir, "nb_3.xopp")

        val nb1 = Notebook(
            id = "nb-1",
            subjectId = subCalc.id,
            name = "Tema 1",
            fileName = nbFile1.name,
            isFavorite = true
        )
        val nb2 = Notebook(
            id = "nb-2",
            subjectId = subProb.id,
            name = "Integrales",
            fileName = nbFile2.name
        )
        val nb3 = Notebook(
            id = "nb-3",
            subjectId = subFisica.id,
            name = "Mecánica",
            fileName = nbFile3.name
        )

        storeA.save(
            subjects = listOf(subCalc, subProb, subFisica),
            notebooks = listOf(nb1, nb2, nb3),
            tags = emptyList()
        )

        val zipFile = File(tempFolder.root, "export_test.zip")
        val exportResult = zipFile.outputStream().use { out ->
            LibraryZipManager.exportLibrary(storeA, out)
        }

        assertTrue("Export should be successful", exportResult.success)
        assertEquals(3, exportResult.totalNotebooks)
        assertEquals(3, exportResult.totalFolders)

        // Verify physical ZIP entries
        ZipFile(zipFile).use { zip ->
            val entryNames = zip.entries().asSequence().map { it.name }.toList()
            assertTrue("Should contain Cálculo/Tema 1.xopp", entryNames.contains("Cálculo/Tema 1.xopp"))
            assertTrue("Should contain Cálculo/Problemas/Integrales.xopp", entryNames.contains("Cálculo/Problemas/Integrales.xopp"))
            assertTrue("Should contain Física/Mecánica.xopp", entryNames.contains("Física/Mecánica.xopp"))
            assertTrue("Should contain manifest.json", entryNames.contains("manifest.json"))
            assertTrue("Should contain library_registry.json", entryNames.contains("library_registry.json"))

            val manifestEntry = zip.getEntry("manifest.json")
            assertNotNull(manifestEntry)
            val manifestText = zip.getInputStream(manifestEntry).bufferedReader().readText()
            assertTrue(manifestText.contains("FiXmy Notes"))
            assertTrue(manifestText.contains("\"totalNotebooks\": 3"))
        }
    }

    @Test
    fun importLibrary_fromOfficialZip_restoresSubjectsHierarchyAndNotebooks() {
        val storeA = LibraryStore(storeDirA)

        val subUni = Subject(id = "s-uni", name = "Universidad")
        val subAlg = Subject(id = "s-alg", name = "Álgebra Lineal", parentId = "s-uni")
        val tagExam = Tag(id = "t-exam", name = "Examen")

        val file1 = createDummyXoppFile(storeA.notebooksDir, "doc_alg.xopp")
        val nbAlg = Notebook(
            id = "nb-alg",
            subjectId = subAlg.id,
            name = "Matrices y Determinantes",
            fileName = file1.name,
            isFavorite = true,
            tagIds = listOf(tagExam.id)
        )

        storeA.save(
            subjects = listOf(subUni, subAlg),
            notebooks = listOf(nbAlg),
            tags = listOf(tagExam)
        )

        val zipFile = File(tempFolder.root, "roundtrip.zip")
        zipFile.outputStream().use { LibraryZipManager.exportLibrary(storeA, it) }

        // Import into clean storeB
        val storeB = LibraryStore(storeDirB)
        val importResult = zipFile.inputStream().use { input ->
            LibraryZipManager.importLibrary(storeB, input, cacheDir)
        }

        assertTrue("Import should succeed", importResult.success)
        assertEquals(1, importResult.importedNotebooksCount)

        val importedNotebooks = storeB.loadNotebooks()
        assertEquals(1, importedNotebooks.size)
        val importedNb = importedNotebooks[0]
        assertEquals("Matrices y Determinantes", importedNb.name)
        assertTrue(importedNb.isFavorite)

        // Check folder hierarchy restored in storeB
        val importedSubjects = storeB.loadSubjects()
        val restoredAlg = importedSubjects.firstOrNull { it.name == "Álgebra Lineal" }
        assertNotNull(restoredAlg)
        val restoredUni = importedSubjects.firstOrNull { it.id == restoredAlg?.parentId }
        assertNotNull(restoredUni)
        assertEquals("Universidad", restoredUni?.name)

        // Check restored .xopp file is in storeB notebooksDir
        val importedXopp = File(storeB.notebooksDir, importedNb.fileName)
        assertTrue(importedXopp.exists())
        assertTrue(importedXopp.length() > 0)
    }

    @Test
    fun importLibrary_fromCustomUserZip_withoutManifest_buildsHierarchyDynamically() {
        // Build a manual zip without manifest.json or library_registry.json
        val manualZip = File(tempFolder.root, "manual_user.zip")
        ZipOutputStream(FileOutputStream(manualZip)).use { zos ->
            // Entry 1: Ciencias/Biología/Célula.xopp
            zos.putNextEntry(ZipEntry("Ciencias/Biología/Célula.xopp"))
            val dummyGzip = ByteArrayOutputStream().apply {
                GZIPOutputStream(this).use { it.write("<xournal/>".toByteArray()) }
            }.toByteArray()
            zos.write(dummyGzip)
            zos.closeEntry()

            // Entry 2: Historia/Siglo XX.xopp
            zos.putNextEntry(ZipEntry("Historia/Siglo XX.xopp"))
            zos.write(dummyGzip)
            zos.closeEntry()

            // Entry 3: Notas_Libres.xopp (at root)
            zos.putNextEntry(ZipEntry("Notas_Libres.xopp"))
            zos.write(dummyGzip)
            zos.closeEntry()

            // Non-xopp file (should be skipped)
            zos.putNextEntry(ZipEntry("Ciencias/Biología/leeme.txt"))
            zos.write("Instrucciones".toByteArray())
            zos.closeEntry()
        }

        val store = LibraryStore(storeDirB)
        val result = manualZip.inputStream().use { input ->
            LibraryZipManager.importLibrary(store, input, cacheDir)
        }

        assertTrue(result.success)
        assertEquals(3, result.importedNotebooksCount)
        assertEquals(1, result.skippedFilesCount)
        assertTrue(result.skippedFiles.contains("Ciencias/Biología/leeme.txt"))

        val subjects = store.loadSubjects()
        val notebooks = store.loadNotebooks()

        assertEquals(3, notebooks.size)

        // Check nested subjects created: Ciencias -> Biología
        val bio = subjects.firstOrNull { it.name == "Biología" }
        assertNotNull(bio)
        val ciencias = subjects.firstOrNull { it.id == bio?.parentId }
        assertNotNull(ciencias)
        assertEquals("Ciencias", ciencias?.name)

        // Check Historia
        val historia = subjects.firstOrNull { it.name == "Historia" }
        assertNotNull(historia)

        // Check notebooks assignment
        val celula = notebooks.firstOrNull { it.name == "Célula" }
        assertNotNull(celula)
        assertEquals(bio?.id, celula?.subjectId)

        val siglo = notebooks.firstOrNull { it.name == "Siglo XX" }
        assertNotNull(siglo)
        assertEquals(historia?.id, siglo?.subjectId)

        val libres = notebooks.firstOrNull { it.name == "Notas_Libres" }
        assertNotNull(libres)
    }

    @Test
    fun importLibrary_corruptedFile_isSkippedAndReported() {
        val corruptedZip = File(tempFolder.root, "corrupted.zip")
        ZipOutputStream(FileOutputStream(corruptedZip)).use { zos ->
            // Good file
            zos.putNextEntry(ZipEntry("Matemáticas/Valido.xopp"))
            val dummyGzip = ByteArrayOutputStream().apply {
                GZIPOutputStream(this).use { it.write("<xournal/>".toByteArray()) }
            }.toByteArray()
            zos.write(dummyGzip)
            zos.closeEntry()

            // Corrupt file (wrong header, not gzip or xml)
            zos.putNextEntry(ZipEntry("Matemáticas/Corrupto.xopp"))
            zos.write("ESTO NO ES UN ARCHIVO XOPP NI GZIP NI XML VALIDO".toByteArray())
            zos.closeEntry()
        }

        val store = LibraryStore(storeDirB)
        val result = corruptedZip.inputStream().use { input ->
            LibraryZipManager.importLibrary(store, input, cacheDir)
        }

        assertTrue(result.success)
        assertEquals(1, result.importedNotebooksCount)
        assertEquals(1, result.skippedFilesCount)
        assertTrue(result.skippedFiles.any { it.contains("Corrupto.xopp") })

        val notebooks = store.loadNotebooks()
        assertEquals(1, notebooks.size)
        assertEquals("Valido", notebooks[0].name)
    }
}
