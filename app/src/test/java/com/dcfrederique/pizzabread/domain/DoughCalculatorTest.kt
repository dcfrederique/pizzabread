package com.dcfrederique.pizzabread.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DoughCalculatorTest {
    @Test
    fun `napolitaans 4 pizza's`() {
        val r = DoughCalculator.calculate(DoughInput(PizzaStyle.NEAPOLITAN, 4))
        assertEquals(626.0, r.flour, 0.5)
        assertEquals(394.0, r.water, 0.5)
        assertEquals(18.8, r.salt, 0.05)
        assertEquals(0.9, r.yeast, 0.05)
        assertEquals(0.0, r.oil, 0.0)
        assertEquals(1040.0, r.flour + r.water + r.salt + r.yeast + r.oil, 0.001)
    }

    @Test
    fun `romeins 4 pizza's`() {
        val r = DoughCalculator.calculate(DoughInput(PizzaStyle.ROMAN, 4))
        assertEquals(461.0, r.flour, 0.5)
        assertEquals(267.0, r.water, 0.5)
        assertEquals(11.5, r.salt, 0.05)
        assertEquals(2.3, r.yeast, 0.05)
        assertEquals(18.4, r.oil, 0.05)
    }

    @Test
    fun `droge gist is 40 procent van verse`() {
        val fresh = DoughCalculator.calculate(DoughInput(PizzaStyle.ROMAN, 4))
        val dry = DoughCalculator.calculate(DoughInput(PizzaStyle.ROMAN, 4, dryYeast = true))
        assertEquals(fresh.flour * PizzaStyle.ROMAN.freshYeast * 0.4, dry.yeast, 0.01)
    }

    @Test
    fun `grammen afronden`() {
        assertEquals("626 g", DoughCalculator.grams(625.9))
        assertEquals("0,9 g", DoughCalculator.grams(0.94))
    }

    @Test
    fun `duur tonen`() {
        assertEquals("45 min", TimeFormat.duration(45))
        assertEquals("4 u 30", TimeFormat.duration(270))
        assertEquals("18 u", TimeFormat.duration(1080))
    }
}
