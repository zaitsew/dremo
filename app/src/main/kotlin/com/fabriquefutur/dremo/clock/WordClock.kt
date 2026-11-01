package com.fabriquefutur.dremo.clock

/**
 * Phrases the time in words, in lines that fit a big display. The time is
 * floored to five minutes, as on a classic word clock: 10:29 reads "TEN",
 * 10:30 reads "HALF PAST TEN".
 *
 * English and Spanish are supported. Russian is not: its hour names change
 * case with the phrase, and a clumsy word clock is worse than none, so the
 * face is hidden in Russian (see [supports]).
 */
object WordClock {
    fun supports(language: String): Boolean = language == "en" || language == "es"

    /** The lines for a time, or null when [language] has no word clock. */
    fun lines(language: String, hour: Int, minute: Int): List<String>? = when (language) {
        "en" -> english(hour, minute)
        "es" -> spanish(hour, minute)
        else -> null
    }

    private val EN_HOURS = listOf(
        "TWELVE", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX",
        "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN",
    )

    private fun english(hour: Int, minute: Int): List<String> {
        val five = Math.floorMod(minute, 60) / 5
        val h = Math.floorMod(hour, 24)
        val current = EN_HOURS[h % 12]
        val next = EN_HOURS[(h + 1) % 12]
        return when (five) {
            0 -> listOf("IT IS", current, "O'CLOCK")
            1 -> listOf("IT IS", "FIVE PAST", current)
            2 -> listOf("IT IS", "TEN PAST", current)
            3 -> listOf("IT IS", "A QUARTER PAST", current)
            4 -> listOf("IT IS", "TWENTY PAST", current)
            5 -> listOf("IT IS", "TWENTY FIVE PAST", current)
            6 -> listOf("IT IS", "HALF PAST", current)
            7 -> listOf("IT IS", "TWENTY FIVE TO", next)
            8 -> listOf("IT IS", "TWENTY TO", next)
            9 -> listOf("IT IS", "A QUARTER TO", next)
            10 -> listOf("IT IS", "TEN TO", next)
            else -> listOf("IT IS", "FIVE TO", next)
        }
    }

    private val ES_HOURS = listOf(
        "DOCE", "UNA", "DOS", "TRES", "CUATRO", "CINCO", "SEIS",
        "SIETE", "OCHO", "NUEVE", "DIEZ", "ONCE",
    )

    private fun spanish(hour: Int, minute: Int): List<String> {
        val five = Math.floorMod(minute, 60) / 5
        val h = Math.floorMod(hour, 24)
        // Past the half hour, Spanish names the next hour: "las dos menos cuarto".
        val named = if (five >= 7) (h + 1) % 12 else h % 12
        val prefix = if (named == 1) "ES LA" else "SON LAS"
        val hourWord = ES_HOURS[named]
        val tail = when (five) {
            0 -> "EN PUNTO"
            1 -> "Y CINCO"
            2 -> "Y DIEZ"
            3 -> "Y CUARTO"
            4 -> "Y VEINTE"
            5 -> "Y VEINTICINCO"
            6 -> "Y MEDIA"
            7 -> "MENOS VEINTICINCO"
            8 -> "MENOS VEINTE"
            9 -> "MENOS CUARTO"
            10 -> "MENOS DIEZ"
            else -> "MENOS CINCO"
        }
        return listOf(prefix, hourWord, tail)
    }
}
