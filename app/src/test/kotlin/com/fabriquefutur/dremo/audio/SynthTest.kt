package com.fabriquefutur.dremo.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class SynthTest {
    private val rate = SoundSource.SAMPLE_RATE

    private fun render(kind: SoundKind, seconds: Int = 3, seed: Int = 7): FloatArray {
        val source = SoundSource.create(kind, Random(seed), rate)
        val out = FloatArray(rate * seconds)
        // Fill in chunks of the size the engine uses, to cover state carried across buffers.
        var i = 0
        val chunk = FloatArray(2048)
        while (i < out.size) {
            val n = minOf(chunk.size, out.size - i)
            source.fill(chunk, n)
            chunk.copyInto(out, i, 0, n)
            i += n
        }
        return out
    }

    private fun rms(a: FloatArray): Double = sqrt(a.fold(0.0) { s, v -> s + v * v } / a.size)

    @Test
    fun `every sound is bounded and not silent`() {
        for (kind in SoundKind.entries) {
            val samples = render(kind)
            assertTrue("$kind peak", samples.all { it in -1f..1f })
            assertTrue("$kind rms ${rms(samples)}", rms(samples) > 0.02)
            assertTrue("$kind peak level", samples.maxOf { abs(it) } > 0.1f)
            assertTrue("$kind has no NaN", samples.none { it.isNaN() })
        }
    }

    @Test
    fun `no sound is clipped flat`() {
        for (kind in SoundKind.entries) {
            val samples = render(kind)
            val clipped = samples.count { abs(it) >= 0.999f }
            assertTrue("$kind clipped $clipped", clipped < samples.size / 200)
        }
    }

    @Test
    fun `sound is not constant and has no DC offset`() {
        for (kind in SoundKind.entries) {
            val samples = render(kind)
            val mean = samples.average()
            assertTrue("$kind mean $mean", abs(mean) < 0.1)
            assertTrue("$kind varies", samples.toSet().size > 1000)
        }
    }

    @Test
    fun `the same seed gives the same samples and another seed does not`() {
        val a = render(SoundKind.RAIN, 1, seed = 1)
        val b = render(SoundKind.RAIN, 1, seed = 1)
        val c = render(SoundKind.RAIN, 1, seed = 2)
        assertTrue(a.contentEquals(b))
        assertTrue(!a.contentEquals(c))
    }

    @Test
    fun `fill honours the count and leaves the rest alone`() {
        val out = FloatArray(100) { 9f }
        SoundSource.create(SoundKind.PINK, Random(1)).fill(out, 40)
        assertTrue(out.take(40).all { it in -1f..1f })
        assertTrue(out.drop(40).all { it == 9f })
    }

    @Test
    fun `brown noise has more low-frequency energy than white`() {
        // Compare the energy of sample-to-sample differences with the energy of the signal:
        // for a smooth (low-pass) signal the ratio is small.
        fun roughness(a: FloatArray): Double {
            var d = 0.0
            var s = 0.0
            for (i in 1 until a.size) { d += (a[i] - a[i - 1]).let { it * it }; s += a[i] * a[i] }
            return d / s
        }
        assertTrue(roughness(render(SoundKind.BROWN)) < roughness(render(SoundKind.PINK)))
        assertTrue(roughness(render(SoundKind.PINK)) < roughness(render(SoundKind.WHITE)))
    }

    @Test
    fun `fire and rain have bursts above their average level`() {
        for (kind in listOf(SoundKind.FIRE, SoundKind.RAIN)) {
            val s = render(kind)
            assertTrue("$kind crest factor", s.maxOf { abs(it) } > 3 * rms(s))
        }
    }

    @Test
    fun `ocean swells in level over seconds`() {
        val s = render(SoundKind.OCEAN, seconds = 20)
        val windows = s.toList().chunked(rate / 2).map { w -> sqrt(w.fold(0.0) { a, v -> a + v * v } / w.size) }
        assertTrue("swell ratio", windows.max() > 2.0 * windows.min())
    }

    @Test
    fun `volume steps are monotonic from silence to full`() {
        assertEquals(0f, VolumeSteps.gain(0), 0f)
        assertEquals(1f, VolumeSteps.gain(VolumeSteps.MAX_STEP), 0.0001f)
        for (step in 0 until VolumeSteps.MAX_STEP) {
            assertTrue(VolumeSteps.gain(step) < VolumeSteps.gain(step + 1))
        }
        assertEquals(VolumeSteps.gain(0), VolumeSteps.gain(-5), 0f)
        assertEquals(VolumeSteps.gain(10), VolumeSteps.gain(99), 0f)
        assertEquals(10, VolumeSteps.clamp(99))
    }

    @Test
    fun `PCM conversion clamps instead of wrapping`() {
        val src = floatArrayOf(0f, 0.5f, -0.5f, 1f, -1f, 2f)
        val dst = ShortArray(6)
        floatsToPcm(src, 6, 1f, dst)
        assertEquals(0, dst[0].toInt())
        assertEquals(16383, dst[1].toInt())
        assertEquals(-16383, dst[2].toInt())
        assertEquals(32767, dst[3].toInt())
        assertEquals(-32767, dst[4].toInt())
        assertEquals(32767, dst[5].toInt())
        floatsToPcm(src, 6, 0f, dst)
        assertTrue(dst.all { it.toInt() == 0 })
    }

    @Test
    fun `kinds round-trip through their keys`() {
        SoundKind.entries.forEach { assertEquals(it, SoundKind.fromKey(it.key)) }
        assertEquals(null, SoundKind.fromKey("zzz"))
    }
}
