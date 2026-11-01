package com.fabriquefutur.dremo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {
    private val min = 60_000L

    @Test
    fun `an unset timer is off and silent-free`() {
        val t = SleepTimerState()
        assertFalse(t.isSet)
        assertEquals(SleepPhase.OFF, t.phase(123_456L))
        assertEquals(1f, t.volumeFactor(123_456L), 0f)
        assertEquals(0L, t.remainingMs(123_456L))
    }

    @Test
    fun `counts at full volume, then fades, then goes dim`() {
        val t = SleepTimerState(15, startedAtMs = 1_000L)
        val end = 1_000L + 15 * min
        assertEquals(SleepPhase.COUNTING, t.phase(1_000L))
        assertEquals(SleepPhase.COUNTING, t.phase(end - SleepTimerState.FADE_MS - 1))
        assertEquals(SleepPhase.FADING, t.phase(end - SleepTimerState.FADE_MS))
        assertEquals(SleepPhase.FADING, t.phase(end - 1))
        assertEquals(SleepPhase.DIM, t.phase(end))
        assertEquals(SleepPhase.DIM, t.phase(end + 10 * min))
    }

    @Test
    fun `volume falls linearly across the fade`() {
        val t = SleepTimerState(30, startedAtMs = 0L)
        val end = 30 * min
        assertEquals(1f, t.volumeFactor(end - SleepTimerState.FADE_MS - 1), 0f)
        assertEquals(1f, t.volumeFactor(end - SleepTimerState.FADE_MS), 0.0001f)
        assertEquals(0.5f, t.volumeFactor(end - SleepTimerState.FADE_MS / 2), 0.0001f)
        assertEquals(0f, t.volumeFactor(end), 0f)
        assertEquals(0f, t.volumeFactor(end + 5_000L), 0f)
    }

    @Test
    fun `volume never rises during the fade`() {
        val t = SleepTimerState(15, startedAtMs = 0L)
        val end = 15 * min
        var last = 1f
        var now = end - SleepTimerState.FADE_MS
        while (now <= end) {
            val v = t.volumeFactor(now)
            assertTrue(v <= last)
            last = v
            now += 1_000L
        }
    }

    @Test
    fun `remaining time counts down to zero`() {
        val t = SleepTimerState(60, startedAtMs = 5_000L)
        assertEquals(60 * min, t.remainingMs(5_000L))
        assertEquals(59 * min, t.remainingMs(5_000L + min))
        assertEquals(0L, t.remainingMs(5_000L + 61 * min))
    }

    @Test
    fun `cycling steps through the options and back to off`() {
        var t = SleepTimerState()
        val seen = mutableListOf<Int>()
        repeat(5) {
            t = t.cycled(nowMs = 10_000L * it)
            seen += t.minutes
        }
        assertEquals(listOf(15, 30, 60, 90, 0), seen)
        assertFalse(t.isSet)
    }

    @Test
    fun `cycling restarts the countdown`() {
        val t = SleepTimerState(15, startedAtMs = 0L).cycled(nowMs = 7 * min)
        assertEquals(30, t.minutes)
        assertEquals(7 * min, t.startedAtMs)
        assertEquals(SleepPhase.COUNTING, t.phase(7 * min))
    }

    @Test
    fun `cancelling returns to off`() {
        assertEquals(SleepPhase.OFF, SleepTimerState(15, 0L).cancelled().phase(1L))
    }

    @Test
    fun `an unknown duration restarts the cycle`() {
        assertEquals(15, SleepOptions.next(7))
        assertEquals(15, SleepOptions.next(0))
        assertEquals(0, SleepOptions.next(90))
    }
}
