package com.fabriquefutur.dremo.core

/**
 * Burn-in protection: the clock and overlays move a few pixels once a minute.
 * The offsets walk a fixed, scattered path over a 5 x 5 grid so that no spot
 * is held for long and no two steps in a row are neighbours.
 */
object PixelShift {
    /** The maximum distance from the centre, in grid cells. */
    const val RADIUS = 2

    private val PATH = listOf(
        0 to 0, 2 to -1, -1 to 2, 1 to 1, -2 to -1, 0 to -2, 2 to 2, -2 to 1,
        1 to -2, -1 to -1, 2 to 0, -2 to 2, 0 to 2, 1 to -1, -1 to 0, 2 to -2,
    )

    val LENGTH: Int get() = PATH.size

    /** The offset in grid cells for a step number (the minutes since the epoch will do). */
    fun cell(step: Long): Pair<Int, Int> = PATH[Math.floorMod(step, PATH.size.toLong()).toInt()]
}
