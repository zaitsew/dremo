package com.fabriquefutur.dremo.core

import com.fabriquefutur.dremo.audio.SoundKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenesTest {
    @Test
    fun `keys are unique and round-trip`() {
        assertEquals(SceneId.entries.size, SceneId.entries.map { it.key }.toSet().size)
        SceneId.entries.forEach { assertEquals(it, SceneId.fromKey(it.key)) }
        assertNull(SceneId.fromKey("nope"))
        assertNull(SceneId.fromKey(null))
    }

    @Test
    fun `the word clock is offered in English and Spanish only`() {
        assertTrue(SceneId.CLOCK_WORDS in SceneId.available("en"))
        assertTrue(SceneId.CLOCK_WORDS in SceneId.available("es"))
        assertFalse(SceneId.CLOCK_WORDS in SceneId.available("ru"))
        assertFalse(SceneId.CLOCK_WORDS in SceneId.available("de"))
        assertEquals(SceneId.entries.size - 1, SceneId.available("ru").size)
    }

    @Test
    fun `ambience scenes have a default sound and clocks are silent`() {
        SceneId.entries.forEach {
            if (it.isClock) assertNull(it.key, it.defaultSound) else assertTrue(it.key, it.defaultSound != null)
        }
        assertEquals(SoundKind.RAIN, SceneId.RAIN.defaultSound)
        assertEquals(SoundKind.FIRE, SceneId.FIREPLACE.defaultSound)
        assertEquals(SoundKind.OCEAN, SceneId.OCEAN.defaultSound)
    }

    @Test
    fun `there are four clock faces and six ambience scenes`() {
        assertEquals(4, SceneId.entries.count { it.isClock })
        assertEquals(6, SceneId.entries.count { !it.isClock })
    }

    @Test
    fun `the sound choice cycles through every kind and back to the scene default`() {
        var choice: SoundKind? = null
        val seen = mutableListOf<SoundKind?>()
        repeat(SoundChoices.ORDER.size) {
            choice = SoundChoices.next(choice)
            seen += choice
        }
        assertEquals(SoundKind.entries.toSet(), seen.filterNotNull().toSet())
        assertNull(seen.last())
    }
}
