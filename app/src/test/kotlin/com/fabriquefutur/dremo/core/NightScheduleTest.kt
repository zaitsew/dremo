package com.fabriquefutur.dremo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightScheduleTest {
    private fun at(h: Int, m: Int = 0) = h * 60 + m

    @Test
    fun `default window crosses midnight`() {
        val s = NightSchedule()
        assertTrue(s.isNight(at(23)))
        assertTrue(s.isNight(at(23, 59)))
        assertTrue(s.isNight(at(0)))
        assertTrue(s.isNight(at(3, 30)))
        assertTrue(s.isNight(at(6, 59)))
        assertFalse(s.isNight(at(7)))
        assertFalse(s.isNight(at(12)))
        assertFalse(s.isNight(at(22, 59)))
    }

    @Test
    fun `a same-day window includes the start and excludes the end`() {
        val s = NightSchedule(true, at(13), at(15))
        assertFalse(s.isNight(at(12, 59)))
        assertTrue(s.isNight(at(13)))
        assertTrue(s.isNight(at(14, 59)))
        assertFalse(s.isNight(at(15)))
    }

    @Test
    fun `disabled is never night`() {
        assertFalse(NightSchedule(false).isNight(at(2)))
    }

    @Test
    fun `equal start and end is an empty window`() {
        val s = NightSchedule(true, at(5), at(5))
        assertFalse(s.isNight(at(5)))
        assertFalse(s.isNight(at(17)))
    }

    @Test
    fun `minutes outside a day wrap`() {
        val s = NightSchedule()
        assertTrue(s.isNight(NightSchedule.MINUTES_PER_DAY + at(1)))
        assertTrue(s.isNight(-at(0, 30)))
    }

    @Test
    fun `dim leaves about 40 percent brightness`() {
        assertEquals(0.4f, 1f - NightSchedule.DIM_ALPHA, 0.0001f)
    }

    @Test
    fun `stepping moves by half an hour and wraps`() {
        assertEquals(at(23, 30), NightSchedule.step(at(23), 1))
        assertEquals(0, NightSchedule.step(at(23, 30), 1))
        assertEquals(at(23, 30), NightSchedule.step(0, -1))
    }

    @Test
    fun `labels are 24-hour`() {
        assertEquals("23:00", NightSchedule.label(at(23)))
        assertEquals("07:30", NightSchedule.label(at(7, 30)))
        assertEquals("00:00", NightSchedule.label(0))
    }
}
