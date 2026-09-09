package com.nexopp.stem

import org.junit.Assert.*
import org.junit.Test

class UnitConverterTest {

    private val eps = 1e-4

    @Test
    fun `converts length units accurately`() {
        val lengths = UnitRegistry.categories[UnitCategory.LENGTH]!!
        val m = lengths.find { it.id == "m" }!!
        val cm = lengths.find { it.id == "cm" }!!
        val inch = lengths.find { it.id == "in" }!!
        val km = lengths.find { it.id == "km" }!!

        assertEquals(100.0, UnitRegistry.convert(1.0, m, cm), eps)
        assertEquals(2.54, UnitRegistry.convert(1.0, inch, cm), eps)
        assertEquals(1000.0, UnitRegistry.convert(1.0, km, m), eps)
    }

    @Test
    fun `converts pressure units accurately`() {
        val pressures = UnitRegistry.categories[UnitCategory.PRESSURE]!!
        val bar = pressures.find { it.id == "bar" }!!
        val kpa = pressures.find { it.id == "kpa" }!!
        val psi = pressures.find { it.id == "psi" }!!

        assertEquals(100.0, UnitRegistry.convert(1.0, bar, kpa), eps)
        // 1 bar approx 14.5038 psi
        assertEquals(14.5038, UnitRegistry.convert(1.0, bar, psi), 1e-2)
    }

    @Test
    fun `converts temperature units accurately`() {
        val temps = UnitRegistry.categories[UnitCategory.TEMPERATURE]!!
        val c = temps.find { it.id == "c" }!!
        val f = temps.find { it.id == "f" }!!
        val k = temps.find { it.id == "k" }!!

        assertEquals(32.0, UnitRegistry.convert(0.0, c, f), eps)
        assertEquals(212.0, UnitRegistry.convert(100.0, c, f), eps)
        assertEquals(373.15, UnitRegistry.convert(100.0, c, k), eps)
        assertEquals(0.0, UnitRegistry.convert(32.0, f, c), eps)
    }

    @Test
    fun `converts digital data units accurately`() {
        val data = UnitRegistry.categories[UnitCategory.DATA]!!
        val kb = data.find { it.id == "kb" }!!
        val b = data.find { it.id == "b" }!!
        val mib = data.find { it.id == "mib" }!!
        val kib = data.find { it.id == "kib" }!!

        assertEquals(1000.0, UnitRegistry.convert(1.0, kb, b), eps)
        assertEquals(1024.0, UnitRegistry.convert(1.0, mib, kib), eps)
    }
}
