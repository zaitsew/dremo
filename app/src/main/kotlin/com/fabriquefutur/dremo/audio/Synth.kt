package com.fabriquefutur.dremo.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** The sounds Dremo can synthesize. No audio files: every sample is computed. */
enum class SoundKind(val key: String) {
    WHITE("white"),
    PINK("pink"),
    BROWN("brown"),
    RAIN("rain"),
    FIRE("fire"),
    OCEAN("ocean");

    companion object {
        fun fromKey(key: String?): SoundKind? = entries.firstOrNull { it.key == key }
    }
}

/** Volume steps shown to the user: 0 is silent, [MAX_STEP] is full scale. */
object VolumeSteps {
    const val MAX_STEP = 10
    const val DEFAULT_STEP = 4

    fun clamp(step: Int): Int = step.coerceIn(0, MAX_STEP)

    /** Linear gain for a step. A power curve, so the low steps are fine-grained. */
    fun gain(step: Int): Float {
        val x = clamp(step) / MAX_STEP.toFloat()
        return x * kotlin.math.sqrt(x)
    }
}

/**
 * A mono sample source. [fill] writes [count] samples in -1..1 into [out]. All
 * state lives in fields, so a running source allocates nothing.
 */
abstract class SoundSource {
    abstract fun fill(out: FloatArray, count: Int)

    companion object {
        const val SAMPLE_RATE = 44_100

        fun create(kind: SoundKind, random: Random = Random.Default, sampleRate: Int = SAMPLE_RATE): SoundSource =
            when (kind) {
                SoundKind.WHITE -> WhiteNoise(random)
                SoundKind.PINK -> PinkNoise(random)
                SoundKind.BROWN -> BrownNoise(random)
                SoundKind.RAIN -> RainSound(random, sampleRate)
                SoundKind.FIRE -> FireSound(random, sampleRate)
                SoundKind.OCEAN -> OceanSound(random, sampleRate)
            }
    }
}

private fun Random.nextSigned(): Float = nextFloat() * 2f - 1f

/** Flat spectrum, scaled to a comfortable level. */
class WhiteNoise(private val random: Random) : SoundSource() {
    override fun fill(out: FloatArray, count: Int) {
        for (i in 0 until count) out[i] = random.nextSigned() * 0.35f
    }
}

/** Pink noise (-3 dB per octave) with Paul Kellet's economy filter. */
class PinkNoise(private val random: Random) : SoundSource() {
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f

    override fun fill(out: FloatArray, count: Int) {
        for (i in 0 until count) out[i] = next()
    }

    internal fun next(): Float {
        val white = random.nextSigned()
        b0 = 0.99765f * b0 + white * 0.0990460f
        b1 = 0.96300f * b1 + white * 0.2965164f
        b2 = 0.57000f * b2 + white * 1.0526913f
        return ((b0 + b1 + b2 + white * 0.1848f) * 0.16f).coerceIn(-1f, 1f)
    }
}

/** Brown (red) noise: integrated white noise with a slow leak so it never drifts away. */
class BrownNoise(private val random: Random) : SoundSource() {
    private var last = 0f

    override fun fill(out: FloatArray, count: Int) {
        for (i in 0 until count) out[i] = next()
    }

    internal fun next(): Float {
        last = (last + 0.02f * random.nextSigned()) / 1.02f
        return (last * 5.5f).coerceIn(-1f, 1f)
    }
}

/**
 * Rain: soft hiss (high-passed pink noise) plus random droplets. A droplet is a
 * short decaying sine "plink" at 1.5 to 4.5 kHz. At most [MAX_DROPS] ring at once.
 */
class RainSound(private val random: Random, private val sampleRate: Int) : SoundSource() {
    private val pink = PinkNoise(random)
    private var lowpass = 0f
    private val phase = FloatArray(MAX_DROPS)
    private val step = FloatArray(MAX_DROPS)
    private val amp = FloatArray(MAX_DROPS)
    private val decay = FloatArray(MAX_DROPS)
    private val dropChance = DROPS_PER_SECOND / sampleRate.toFloat()

    override fun fill(out: FloatArray, count: Int) {
        for (i in 0 until count) {
            val noise = pink.next()
            lowpass += 0.02f * (noise - lowpass)
            var s = (noise - lowpass) * 0.9f
            if (random.nextFloat() < dropChance) spawnDrop()
            for (d in 0 until MAX_DROPS) {
                if (amp[d] > 0.0005f) {
                    s += sin(phase[d]) * amp[d]
                    phase[d] += step[d]
                    amp[d] *= decay[d]
                }
            }
            out[i] = (s * 0.55f).coerceIn(-1f, 1f)
        }
    }

    private fun spawnDrop() {
        for (d in 0 until MAX_DROPS) {
            if (amp[d] <= 0.0005f) {
                val freq = 1500f + random.nextFloat() * 3000f
                phase[d] = 0f
                step[d] = (2.0 * PI * freq / sampleRate).toFloat()
                amp[d] = 0.12f + random.nextFloat() * 0.3f
                // About 6 to 14 ms to fall by a factor of e.
                val tau = (0.006f + random.nextFloat() * 0.008f) * sampleRate
                decay[d] = exp(-1.0 / tau).toFloat()
                return
            }
        }
    }

    private companion object {
        const val MAX_DROPS = 8
        const val DROPS_PER_SECOND = 45f
    }
}

/**
 * Fire: a low roar (brown noise whose level wanders slowly) plus crackles,
 * short bursts of noise with a fast decay, about 10 a second.
 */
class FireSound(private val random: Random, private val sampleRate: Int) : SoundSource() {
    private val brown = BrownNoise(random)
    private var roarPhase = 0f
    private val roarStep = (2.0 * PI * 0.17 / sampleRate).toFloat()
    private var crackle = 0f
    private var crackleDecay = 0.99f
    private val crackleChance = CRACKLES_PER_SECOND / sampleRate.toFloat()

    override fun fill(out: FloatArray, count: Int) {
        for (i in 0 until count) {
            roarPhase += roarStep
            val roar = brown.next() * (0.75f + 0.25f * sin(roarPhase))
            if (random.nextFloat() < crackleChance) {
                val big = random.nextFloat() < 0.15f
                crackle = if (big) 0.9f else 0.25f + random.nextFloat() * 0.35f
                val tau = (if (big) 0.012f else 0.002f + random.nextFloat() * 0.004f) * sampleRate
                crackleDecay = exp(-1.0 / tau).toFloat()
            }
            var s = roar * 0.8f
            if (crackle > 0.001f) {
                s += random.nextSigned() * crackle
                crackle *= crackleDecay
            }
            out[i] = (s * 0.6f).coerceIn(-1f, 1f)
        }
    }

    private companion object {
        const val CRACKLES_PER_SECOND = 10f
    }
}

/**
 * Ocean: pink noise through a one-pole low-pass. A slow swell, two sine waves
 * with unrelated periods (9 s and 14 s), raises both the level and the cutoff,
 * so each wave brightens as it breaks and dulls as it draws back.
 */
class OceanSound(private val random: Random, private val sampleRate: Int) : SoundSource() {
    private val pink = PinkNoise(random)
    private var lowpass = 0f
    private var phaseA = random.nextFloat() * 6.28f
    private var phaseB = random.nextFloat() * 6.28f
    private val stepA = (2.0 * PI / (9.0 * sampleRate)).toFloat()
    private val stepB = (2.0 * PI / (14.0 * sampleRate)).toFloat()

    override fun fill(out: FloatArray, count: Int) {
        for (i in 0 until count) {
            phaseA += stepA
            phaseB += stepB
            val swell = 0.5f + 0.5f * (0.6f * sin(phaseA) + 0.4f * sin(phaseB))
            val envelope = 0.15f + 0.85f * swell * swell
            lowpass += (0.02f + 0.2f * envelope) * (pink.next() - lowpass)
            out[i] = (lowpass * envelope * 1.5f).coerceIn(-1f, 1f)
        }
    }
}

/** Float to 16-bit PCM with a clamp, so a gain above 1 can never wrap around. */
fun floatsToPcm(src: FloatArray, count: Int, gain: Float, dst: ShortArray) {
    for (i in 0 until count) {
        val v = (src[i] * gain * 32767f).coerceIn(-32768f, 32767f)
        dst[i] = v.toInt().toShort()
    }
}
