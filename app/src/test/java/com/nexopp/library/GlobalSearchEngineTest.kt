package com.nexopp.library

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GlobalSearchEngineTest {

    private lateinit var searchEngine: GlobalSearchEngine
    private lateinit var subjects: List<Subject>
    private lateinit var tags: List<Tag>
    private lateinit var notebooks: List<Notebook>

    @Before
    fun setup() {
        searchEngine = GlobalSearchEngine()
        subjects = listOf(
            Subject(id = "sub-math", name = "Matemáticas"),
            Subject(id = "sub-phys", name = "Física"),
            Subject(id = "sub-chem", name = "Química")
        )
        tags = listOf(
            Tag(id = "tag-exam", name = "Exámenes"),
            Tag(id = "tag-lab", name = "Prácticas"),
            Tag(id = "tag-theory", name = "Teoría")
        )
        notebooks = listOf(
            Notebook(
                id = "nb-1",
                subjectId = "sub-math",
                name = "Álgebra Lineal - Vectores",
                fileName = "algebra.xopp",
                isFavorite = true,
                lastModified = 1000L,
                pageCount = 10,
                tagIds = listOf("tag-theory", "tag-exam")
            ),
            Notebook(
                id = "nb-2",
                subjectId = "sub-math",
                name = "Cálculo Integral",
                fileName = "calculo.xopp",
                isFavorite = false,
                lastModified = 2000L,
                pageCount = 25,
                tagIds = listOf("tag-theory")
            ),
            Notebook(
                id = "nb-3",
                subjectId = "sub-phys",
                name = "Mecánica Cuántica",
                fileName = "mecanica.xopp",
                isFavorite = true,
                lastModified = 3000L,
                pageCount = 15,
                tagIds = listOf("tag-lab")
            ),
            Notebook(
                id = "nb-4",
                subjectId = "sub-chem",
                name = "Termodinámica Química",
                fileName = "termo.xopp",
                isFavorite = false,
                lastModified = 4000L,
                pageCount = 5,
                tagIds = listOf("tag-exam")
            )
        )
    }

    @Test
    fun `search by notebook name finds matching items`() {
        val results = searchEngine.search(
            query = "Vectores",
            notebooks = notebooks,
            subjects = subjects,
            tags = tags
        )
        assertEquals(1, results.size)
        assertEquals("nb-1", results[0].notebook.id)
        assertEquals(SearchMatchKind.NOTEBOOK_NAME, results[0].matchKind)
    }

    @Test
    fun `search by subject name finds all notebooks in subject`() {
        val results = searchEngine.search(
            query = "Matemáticas",
            notebooks = notebooks,
            subjects = subjects,
            tags = tags
        )
        assertEquals(2, results.size)
        assertTrue(results.any { it.notebook.id == "nb-1" })
        assertTrue(results.any { it.notebook.id == "nb-2" })
    }

    @Test
    fun `search by tag name finds all tagged notebooks`() {
        val results = searchEngine.search(
            query = "Exámenes",
            notebooks = notebooks,
            subjects = subjects,
            tags = tags
        )
        assertEquals(2, results.size)
        assertTrue(results.any { it.notebook.id == "nb-1" })
        assertTrue(results.any { it.notebook.id == "nb-4" })
        assertEquals(SearchMatchKind.TAG_NAME, results[0].matchKind)
    }

    @Test
    fun `filters by favorites only`() {
        val results = searchEngine.search(
            query = "",
            notebooks = notebooks,
            subjects = subjects,
            tags = tags,
            onlyFavorites = true
        )
        assertEquals(2, results.size)
        assertTrue(results.all { it.notebook.isFavorite })
    }

    @Test
    fun `filters by specific subject and tag simultaneously`() {
        val results = searchEngine.search(
            query = "",
            notebooks = notebooks,
            subjects = subjects,
            tags = tags,
            filterSubjectId = "sub-math",
            filterTagIds = setOf("tag-exam")
        )
        assertEquals(1, results.size)
        assertEquals("nb-1", results[0].notebook.id)
    }

    @Test
    fun `sorts notebooks correctly by page count and name`() {
        val sortedByPages = searchEngine.sortNotebooks(notebooks, NotebookSortOption.PAGE_COUNT_DESC)
        assertEquals("nb-2", sortedByPages[0].id) // 25 pages
        assertEquals("nb-4", sortedByPages.last().id) // 5 pages

        val sortedByName = searchEngine.sortNotebooks(notebooks, NotebookSortOption.NAME_ASC)
        assertEquals("nb-1", sortedByName[0].id) // Álgebra Lineal
        assertEquals("nb-4", sortedByName.last().id) // Termodinámica Química
    }
}
