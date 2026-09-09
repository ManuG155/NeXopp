package com.nexopp.library

import org.junit.Assert.*
import org.junit.Test

class HierarchicalFoldersTest {

    @Test
    fun `buildPath generates correct breadcrumb path`() {
        val root = Subject(id = "sub-uni", name = "Universidad", parentId = null)
        val child = Subject(id = "sub-elec", name = "Electrónica", parentId = "sub-uni")
        val grandChild = Subject(id = "sub-analog", name = "Analógica", parentId = "sub-elec")

        val all = listOf(root, child, grandChild)

        assertEquals("Universidad", root.buildPath(all))
        assertEquals("Universidad / Electrónica", child.buildPath(all))
        assertEquals("Universidad / Electrónica / Analógica", grandChild.buildPath(all))
    }

    @Test
    fun `getParentChain returns full chain from root to leaf`() {
        val root = Subject(id = "sub-uni", name = "Universidad", parentId = null)
        val child = Subject(id = "sub-elec", name = "Electrónica", parentId = "sub-uni")
        val grandChild = Subject(id = "sub-analog", name = "Analógica", parentId = "sub-elec")

        val all = listOf(root, child, grandChild)

        val chain = grandChild.getParentChain(all)
        assertEquals(3, chain.size)
        assertEquals("sub-uni", chain[0].id)
        assertEquals("sub-elec", chain[1].id)
        assertEquals("sub-analog", chain[2].id)
    }

    @Test
    fun `getAllDescendantIds includes self and all descendants recursively`() {
        val root = Subject(id = "root", name = "Root")
        val branch1 = Subject(id = "b1", name = "Branch 1", parentId = "root")
        val branch2 = Subject(id = "b2", name = "Branch 2", parentId = "root")
        val leaf11 = Subject(id = "l11", name = "Leaf 1.1", parentId = "b1")
        val leaf12 = Subject(id = "l12", name = "Leaf 1.2", parentId = "b1")
        val deepLeaf = Subject(id = "dl", name = "Deep Leaf", parentId = "l11")

        val all = listOf(root, branch1, branch2, leaf11, leaf12, deepLeaf)

        val rootDescendants = root.getAllDescendantIds(all)
        assertEquals(setOf("root", "b1", "b2", "l11", "l12", "dl"), rootDescendants)

        val b1Descendants = branch1.getAllDescendantIds(all)
        assertEquals(setOf("b1", "l11", "l12", "dl"), b1Descendants)

        val leaf12Descendants = leaf12.getAllDescendantIds(all)
        assertEquals(setOf("l12"), leaf12Descendants)
    }

    @Test
    fun `search with filterSubjectId includes notebooks in subfolders`() {
        val searchEngine = GlobalSearchEngine()

        val uni = Subject(id = "uni", name = "Universidad")
        val elec = Subject(id = "elec", name = "Electrónica", parentId = "uni")
        val math = Subject(id = "math", name = "Matemáticas", parentId = "uni")
        val other = Subject(id = "other", name = "Personal")
        val allSubjects = listOf(uni, elec, math, other)

        val notebooks = listOf(
            Notebook(id = "nb-1", subjectId = "uni", name = "Horarios y Matrícula", fileName = "nb1.xopp"),
            Notebook(id = "nb-2", subjectId = "elec", name = "Circuitos RLC", fileName = "nb2.xopp"),
            Notebook(id = "nb-3", subjectId = "math", name = "Cálculo Multivariable", fileName = "nb3.xopp"),
            Notebook(id = "nb-4", subjectId = "other", name = "Lista de la compra", fileName = "nb4.xopp")
        )

        // Searching within "Universidad" should match notes in Universidad, Electrónica, and Matemáticas
        val uniResults = searchEngine.search(
            query = "",
            notebooks = notebooks,
            subjects = allSubjects,
            tags = emptyList(),
            filterSubjectId = "uni"
        )
        assertEquals(3, uniResults.size)
        assertTrue(uniResults.any { it.notebook.id == "nb-1" })
        assertTrue(uniResults.any { it.notebook.id == "nb-2" })
        assertTrue(uniResults.any { it.notebook.id == "nb-3" })
        assertFalse(uniResults.any { it.notebook.id == "nb-4" })

        // Searching within "Electrónica" should only match notes in Electrónica
        val elecResults = searchEngine.search(
            query = "",
            notebooks = notebooks,
            subjects = allSubjects,
            tags = emptyList(),
            filterSubjectId = "elec"
        )
        assertEquals(1, elecResults.size)
        assertEquals("nb-2", elecResults[0].notebook.id)
    }
}
