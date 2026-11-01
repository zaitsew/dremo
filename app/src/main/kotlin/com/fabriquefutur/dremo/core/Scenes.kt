package com.fabriquefutur.dremo.core

import com.fabriquefutur.dremo.audio.SoundKind

enum class SceneFamily { CLOCK, AMBIENCE }

/**
 * Every full-screen scene. The key is what DataStore saves, so it must never
 * change. [defaultSound] is what plays unless the viewer picks another; a clock
 * face is silent by default.
 */
enum class SceneId(val key: String, val family: SceneFamily, val defaultSound: SoundKind?) {
    CLOCK_DIGITAL("clock_digital", SceneFamily.CLOCK, null),
    CLOCK_FLIP("clock_flip", SceneFamily.CLOCK, null),
    CLOCK_ANALOG("clock_analog", SceneFamily.CLOCK, null),
    CLOCK_WORDS("clock_words", SceneFamily.CLOCK, null),
    RAIN("rain", SceneFamily.AMBIENCE, SoundKind.RAIN),
    FIREPLACE("fireplace", SceneFamily.AMBIENCE, SoundKind.FIRE),
    AURORA("aurora", SceneFamily.AMBIENCE, SoundKind.PINK),
    STARS("stars", SceneFamily.AMBIENCE, SoundKind.BROWN),
    OCEAN("ocean", SceneFamily.AMBIENCE, SoundKind.OCEAN),
    LAVA("lava", SceneFamily.AMBIENCE, SoundKind.BROWN);

    val isClock: Boolean get() = family == SceneFamily.CLOCK

    companion object {
        fun fromKey(key: String?): SceneId? = entries.firstOrNull { it.key == key }

        /** The scenes offered in a language: the word clock exists only where [com.fabriquefutur.dremo.clock.WordClock] can phrase the time. */
        fun available(language: String): List<SceneId> =
            entries.filter { it != CLOCK_WORDS || com.fabriquefutur.dremo.clock.WordClock.supports(language) }
    }
}

/** The sound choices in the overlay, in the order the button cycles through them. */
object SoundChoices {
    /** null is "the scene's own sound". */
    val ORDER: List<SoundKind?> = listOf(null) + SoundKind.entries

    fun next(current: SoundKind?): SoundKind? {
        val i = ORDER.indexOf(current)
        return ORDER[(i + 1) % ORDER.size]
    }
}
