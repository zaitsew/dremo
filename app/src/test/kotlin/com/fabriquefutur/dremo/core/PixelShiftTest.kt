package com.fabriquefutur.dremo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class PixelShiftTest {
    @Test
    fun `every offset stays within the radius`() {
        for (step in -50L..200L) {
            val (x, y) = PixelShift.cell(step)
            assertTrue(abs(x) <= PixelShift.RADIUS)
            assertTrue(abs(y) <= PixelShift.RADIUS)
        }
    }

    @Test
    fun `consecutive steps are never neighbours`() {
        for (step in 0L until PixelShift.LENGTH.toLong()) {
            val (ax, ay) = PixelShift.cell(step)
            val (bx, by) = PixelShift.cell(step + 1)
            assertTrue("step $step", max(abs(ax - bx), abs(ay - by)) >= 2)
        }
    }

    @Test
    fun `the path visits many different spots and repeats none within a cycle`() {
        val cells = (0L until PixelShift.LENGTH.toLong()).map { PixelShift.cell(it) }
        assertEquals(cells.size, cells.toSet().size)
        assertTrue(cells.size >= 12)
    }

    @Test
    fun `the sequence is a cycle and handles negative steps`() {
        assertEquals(PixelShift.cell(3), PixelShift.cell(3L + PixelShift.LENGTH))
        assertEquals(PixelShift.cell(PixelShift.LENGTH - 1L), PixelShift.cell(-1))
    }

    @Test
    fun `starts at the centre`() {
        assertEquals(0 to 0, PixelShift.cell(0))
    }
}
