package com.nexopp.stem

import org.junit.Assert.*
import org.junit.Test

class PeriodicTableTest {

    @Test
    fun `verifies essential chemical elements data`() {
        val elements = PeriodicTableRegistry.elements
        assertTrue(elements.isNotEmpty())

        val hydrogen = elements.find { it.symbol == "H" }!!
        assertEquals(1, hydrogen.atomicNumber)
        assertEquals("Hidrógeno", hydrogen.name)
        assertEquals(ElementCategory.NON_METAL, hydrogen.category)

        val carbon = elements.find { it.symbol == "C" }!!
        assertEquals(6, carbon.atomicNumber)
        assertEquals(12.011, carbon.atomicMass, 0.001)

        val gold = elements.find { it.symbol == "Au" }!!
        assertEquals(79, gold.atomicNumber)
        assertEquals(ElementCategory.TRANSITION_METAL, gold.category)
    }

    @Test
    fun `verifies elements categorized properly`() {
        val alkalis = PeriodicTableRegistry.elements.filter { it.category == ElementCategory.ALKALI_METAL }
        assertTrue(alkalis.any { it.symbol == "Li" })
        assertTrue(alkalis.any { it.symbol == "Na" })
        assertTrue(alkalis.any { it.symbol == "K" })

        val halogens = PeriodicTableRegistry.elements.filter { it.category == ElementCategory.HALOGEN }
        assertTrue(halogens.any { it.symbol == "F" })
        assertTrue(halogens.any { it.symbol == "Cl" })
        assertTrue(halogens.any { it.symbol == "Br" })
    }
}
