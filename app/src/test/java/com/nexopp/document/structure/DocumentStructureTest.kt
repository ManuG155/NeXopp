package com.nexopp.document.structure

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DocumentStructureTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: DocumentStructureStore
    private lateinit var baseDir: File

    @Before
    fun setup() {
        baseDir = tempFolder.newFolder("struct_test")
        store = DocumentStructureStore(baseDir)
    }

    @Test
    fun testEmptyDocumentStructure() {
        val struct = store.load("non_existent.xopp")
        assertTrue(struct.tocEntries.isEmpty())
        assertTrue(struct.namedBookmarks.isEmpty())
        assertTrue(struct.internalLinks.isEmpty())
    }

    @Test
    fun testSaveAndLoadTocEntries() {
        val nbFile = "algebra_lineal.xopp"
        val toc = listOf(
            TocEntry(title = "Espacios Vectoriales", pageIndex = 0, order = 0, level = 0),
            TocEntry(title = "Bases y Dimensión", pageIndex = 4, order = 1, level = 1),
            TocEntry(title = "Transformaciones Lineales", pageIndex = 12, order = 2, level = 0)
        )
        val bookmarks = listOf(
            NamedBookmark(title = "Fórmulas examen", pageIndex = 4)
        )
        val links = listOf(
            InternalLink(label = "Ver teorema 3.2 → Pág. 5", targetPageIndex = 4, sourcePageIndex = 1)
        )

        val initial = DocumentStructure(
            tocEntries = toc,
            namedBookmarks = bookmarks,
            internalLinks = links
        )

        store.save(nbFile, initial)
        val loaded = store.load(nbFile)

        assertEquals(3, loaded.tocEntries.size)
        assertEquals("Espacios Vectoriales", loaded.tocEntries[0].title)
        assertEquals(0, loaded.tocEntries[0].pageIndex)
        assertEquals("Bases y Dimensión", loaded.tocEntries[1].title)
        assertEquals(4, loaded.tocEntries[1].pageIndex)
        assertEquals(1, loaded.tocEntries[1].level)

        assertEquals(1, loaded.namedBookmarks.size)
        assertEquals("Fórmulas examen", loaded.namedBookmarks[0].title)

        assertEquals(1, loaded.internalLinks.size)
        assertEquals("Ver teorema 3.2 → Pág. 5", loaded.internalLinks[0].label)
        assertEquals(4, loaded.internalLinks[0].targetPageIndex)
    }

    @Test
    fun testTocOrderingAndHierarchy() {
        val nbFile = "calculo.xopp"
        val toc = listOf(
            TocEntry(title = "Tema 2", pageIndex = 10, order = 1),
            TocEntry(title = "Tema 1", pageIndex = 0, order = 0)
        )
        store.save(nbFile, DocumentStructure(tocEntries = toc))
        val loaded = store.load(nbFile)

        assertEquals("Tema 1", loaded.tocEntries[0].title)
        assertEquals("Tema 2", loaded.tocEntries[1].title)
    }
}
