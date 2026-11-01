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

    @Test
    fun `sound is on only when something plays`() {
        assertTrue(SoundToggle.isOn(true, SoundKind.RAIN))
        assertFalse(SoundToggle.isOn(true, null))
        assertFalse(SoundToggle.isOn(false, SoundKind.RAIN))
        assertEquals(SoundKind.WHITE, SoundToggle.effective(SoundKind.WHITE, SoundKind.RAIN))
        assertEquals(SoundKind.RAIN, SoundToggle.effective(null, SoundKind.RAIN))
        assertNull(SoundToggle.effective(null, null))
    }

    @Test
    fun `turning sound on from none on a clock picks a default`() {
        // The master switch is on by default, so a clock starts with sound "on" and no sound: the button shows off.
        val state = SoundToggle.toggle(enabled = true, choice = null, sceneDefault = null)
        assertEquals(SoundToggle.State(enabled = true, choice = SoundToggle.FALLBACK), state)
        assertTrue(SoundToggle.isOn(state.enabled, SoundToggle.effective(state.choice, null)))
        // Switched off in settings: same result.
        assertEquals(state, SoundToggle.toggle(enabled = false, choice = null, sceneDefault = null))
    }

    @Test
    fun `turning sound off keeps the pick, and on again plays it`() {
        val off = SoundToggle.toggle(enabled = true, choice = SoundKind.OCEAN, sceneDefault = null)
        assertEquals(SoundToggle.State(enabled = false, choice = SoundKind.OCEAN), off)
        val on = SoundToggle.toggle(off.enabled, off.choice, null)
        assertEquals(SoundToggle.State(enabled = true, choice = SoundKind.OCEAN), on)
    }

    @Test
    fun `an ambience scene toggles without pinning its own sound`() {
        val off = SoundToggle.toggle(enabled = true, choice = null, sceneDefault = SoundKind.FIRE)
        assertEquals(SoundToggle.State(enabled = false, choice = null), off)
        val on = SoundToggle.toggle(off.enabled, off.choice, SoundKind.FIRE)
        assertEquals(SoundToggle.State(enabled = true, choice = null), on)
    }
}
