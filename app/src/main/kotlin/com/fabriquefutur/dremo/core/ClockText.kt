package com.fabriquefutur.dremo.core

import java.util.Locale

enum class ClockFormat(val key: String) {
    AUTO("auto"),
    H12("12"),
    H24("24");

    /** The 12/24 hour choice, given what the system setting says. */
    fun use24h(systemIs24h: Boolean): Boolean = when (this) {
        AUTO -> systemIs24h
        H12 -> false
        H24 -> true
    }

    companion object {
        fun fromKey(key: String?): ClockFormat = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

/** The pieces of a displayed time. [suffix] is "AM" or "PM" in 12-hour mode, empty otherwise. */
data class TimeParts(val hourText: String, val minuteText: String, val suffix: String)

object ClockText {
    fun parts(hour: Int, minute: Int, use24h: Boolean): TimeParts {
        val h = Math.floorMod(hour, 24)
        val m = Math.floorMod(minute, 60)
        return if (use24h) {
            TimeParts("%02d".format(h), "%02d".format(m), "")
        } else {
            val h12 = if (h % 12 == 0) 12 else h % 12
            // The hour has no leading zero: "9:05", not "09:05".
            TimeParts(h12.toString(), "%02d".format(m), if (h < 12) "AM" else "PM")
        }
    }

    /** Hour and minute digits as separate characters, for the flip clock: "9", "0", "5" in 12-hour mode. */
    fun digits(parts: TimeParts): List<Char> = (parts.hourText + parts.minuteText).toList()

    /** An hour for the "next hours" line: "14:00" in 24-hour mode, "2 PM" in 12-hour mode. */
    fun hourLabel(hour: Int, use24h: Boolean): String {
        val h = Math.floorMod(hour, 24)
        if (use24h) return "%02d:00".format(h)
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12 ${if (h < 12) "AM" else "PM"}"
    }

    /** Capitalises the first letter, since some locales (Russian, Spanish) give lowercase weekday names. */
    fun capitalised(text: String, locale: Locale): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
