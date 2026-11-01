package com.fabriquefutur.dremo.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ClockTextTest {
    @Test
    fun `24-hour keeps two digits`() {
        assertEquals(TimeParts("07", "05", ""), ClockText.parts(7, 5, true))
        assertEquals(TimeParts("00", "00", ""), ClockText.parts(0, 0, true))
        assertEquals(TimeParts("23", "59", ""), ClockText.parts(23, 59, true))
    }

    @Test
    fun `12-hour has no leading zero and an AM or PM suffix`() {
        assertEquals(TimeParts("9", "05", "AM"), ClockText.parts(9, 5, false))
        assertEquals(TimeParts("12", "00", "AM"), ClockText.parts(0, 0, false))
        assertEquals(TimeParts("12", "30", "PM"), ClockText.parts(12, 30, false))
        assertEquals(TimeParts("1", "00", "PM"), ClockText.parts(13, 0, false))
        assertEquals(TimeParts("11", "59", "PM"), ClockText.parts(23, 59, false))
    }

    @Test
    fun `digits for the flip clock`() {
        assertEquals(listOf('9', '0', '5'), ClockText.digits(ClockText.parts(9, 5, false)))
        assertEquals(listOf('1', '0', '4', '2'), ClockText.digits(ClockText.parts(10, 42, true)))
    }

    @Test
    fun `hour labels follow the format`() {
        assertEquals("14:00", ClockText.hourLabel(14, true))
        assertEquals("02:00", ClockText.hourLabel(2, true))
        assertEquals("2 PM", ClockText.hourLabel(14, false))
        assertEquals("12 AM", ClockText.hourLabel(0, false))
        assertEquals("12 PM", ClockText.hourLabel(12, false))
    }

    @Test
    fun `the format setting overrides the system only when set`() {
        assertEquals(true, ClockFormat.AUTO.use24h(true))
        assertEquals(false, ClockFormat.AUTO.use24h(false))
        assertEquals(false, ClockFormat.H12.use24h(true))
        assertEquals(true, ClockFormat.H24.use24h(false))
        assertEquals(ClockFormat.AUTO, ClockFormat.fromKey("nonsense"))
    }

    @Test
    fun `capitalises a lowercase weekday`() {
        assertEquals("Domingo, 1 de noviembre", ClockText.capitalised("domingo, 1 de noviembre", Locale.forLanguageTag("es")))
        assertEquals("Sunday", ClockText.capitalised("Sunday", Locale.ENGLISH))
    }
}
