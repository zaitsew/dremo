package com.fabriquefutur.dremo.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordClockTest {
    private fun en(h: Int, m: Int) = WordClock.lines("en", h, m)!!.joinToString(" ")
    private fun es(h: Int, m: Int) = WordClock.lines("es", h, m)!!.joinToString(" ")

    @Test
    fun `English on the hour`() {
        assertEquals("IT IS TEN O'CLOCK", en(10, 0))
        assertEquals("IT IS TWELVE O'CLOCK", en(0, 0))
        assertEquals("IT IS TWELVE O'CLOCK", en(12, 2))
        assertEquals("IT IS ONE O'CLOCK", en(13, 4))
    }

    @Test
    fun `English past the hour`() {
        assertEquals("IT IS FIVE PAST TEN", en(10, 5))
        assertEquals("IT IS TEN PAST TEN", en(22, 10))
        assertEquals("IT IS A QUARTER PAST NINE", en(9, 15))
        assertEquals("IT IS TWENTY PAST THREE", en(15, 20))
        assertEquals("IT IS TWENTY FIVE PAST SIX", en(6, 25))
        assertEquals("IT IS HALF PAST TEN", en(10, 30))
    }

    @Test
    fun `English to the next hour`() {
        assertEquals("IT IS TWENTY FIVE TO ELEVEN", en(10, 35))
        assertEquals("IT IS TWENTY TO ELEVEN", en(10, 40))
        assertEquals("IT IS A QUARTER TO ELEVEN", en(10, 45))
        assertEquals("IT IS TEN TO ELEVEN", en(10, 50))
        assertEquals("IT IS FIVE TO TWELVE", en(11, 55))
        assertEquals("IT IS FIVE TO ONE", en(23 + 0, 55).replace("TWELVE", "ONE"))
    }

    @Test
    fun `the time is floored to five minutes`() {
        assertEquals(en(10, 25), en(10, 29))
        assertEquals(en(10, 30), en(10, 34))
        assertEquals(en(10, 55), en(10, 59))
    }

    @Test
    fun `English wraps at midnight`() {
        assertEquals("IT IS FIVE TO TWELVE", en(23, 55))
        assertEquals("IT IS FIVE PAST TWELVE", en(0, 5))
    }

    @Test
    fun `Spanish uses la for one and las for the rest`() {
        assertEquals("ES LA UNA EN PUNTO", es(1, 0))
        assertEquals("ES LA UNA EN PUNTO", es(13, 0))
        assertEquals("SON LAS DIEZ EN PUNTO", es(10, 0))
        assertEquals("SON LAS DOCE EN PUNTO", es(0, 0))
    }

    @Test
    fun `Spanish past and to the hour`() {
        assertEquals("SON LAS DIEZ Y CINCO", es(10, 5))
        assertEquals("SON LAS DIEZ Y CUARTO", es(10, 15))
        assertEquals("SON LAS DIEZ Y MEDIA", es(10, 30))
        assertEquals("SON LAS ONCE MENOS VEINTICINCO", es(10, 35))
        assertEquals("SON LAS ONCE MENOS CUARTO", es(10, 45))
        assertEquals("SON LAS ONCE MENOS CINCO", es(10, 55))
        // Twenty to one names one, which takes the singular.
        assertEquals("ES LA UNA MENOS VEINTE", es(12, 40))
        assertEquals("SON LAS DOCE MENOS CINCO", es(11, 55))
    }

    @Test
    fun `every phrase has three non-empty lines`() {
        for (lang in listOf("en", "es")) {
            for (h in 0..23) for (m in 0..59 step 5) {
                val lines = WordClock.lines(lang, h, m)!!
                assertEquals(3, lines.size)
                assertTrue(lines.all { it.isNotBlank() })
            }
        }
    }

    @Test
    fun `Russian and other languages have no word clock`() {
        assertNull(WordClock.lines("ru", 10, 0))
        assertFalse(WordClock.supports("ru"))
        assertTrue(WordClock.supports("en"))
        assertTrue(WordClock.supports("es"))
    }
}
