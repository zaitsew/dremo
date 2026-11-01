package com.fabriquefutur.dremo.core

/**
 * The night-dimming window, in minutes after midnight. It may cross midnight:
 * 23:00 to 07:00 is start 1380, end 420.
 */
data class NightSchedule(
    val enabled: Boolean = true,
    val startMinute: Int = DEFAULT_START,
    val endMinute: Int = DEFAULT_END,
) {
    /** True when [minuteOfDay] falls inside the window. Equal start and end means an empty window. */
    fun isNight(minuteOfDay: Int): Boolean {
        if (!enabled || startMinute == endMinute) return false
        val m = Math.floorMod(minuteOfDay, MINUTES_PER_DAY)
        return if (startMinute < endMinute) {
            m in startMinute until endMinute
        } else {
            m >= startMinute || m < endMinute
        }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val DEFAULT_START = 23 * 60
        const val DEFAULT_END = 7 * 60

        /** Opacity of the black overlay at night: 60% black leaves about 40% brightness. */
        const val DIM_ALPHA = 0.6f

        /** Settings move the time in half-hour steps. */
        const val STEP = 30

        fun step(minute: Int, direction: Int): Int =
            Math.floorMod(minute + direction * STEP, MINUTES_PER_DAY)

        fun label(minute: Int): String {
            val m = Math.floorMod(minute, MINUTES_PER_DAY)
            return "%02d:%02d".format(m / 60, m % 60)
        }
    }
}
