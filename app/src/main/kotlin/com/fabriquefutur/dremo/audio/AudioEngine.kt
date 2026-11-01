package com.fabriquefutur.dremo.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.random.Random

/**
 * Plays a [SoundSource] through an [AudioTrack] on its own thread. No media
 * session: the caller stops it when the app leaves the screen.
 *
 * [targetGain] may be set from any thread. The writer thread ramps towards it
 * across each buffer, so a volume step or a sleep-timer fade has no clicks.
 */
class AudioEngine {
    @Volatile
    var targetGain: Float = 0f

    private var thread: Thread? = null
    @Volatile
    private var running = false

    @Synchronized
    fun start(kind: SoundKind) {
        stop()
        running = true
        val t = Thread({ run(kind) }, "dremo-audio")
        t.isDaemon = true
        thread = t
        t.start()
    }

    @Synchronized
    fun stop() {
        running = false
        val t = thread ?: return
        thread = null
        // The writer notices within one buffer (about 50 ms).
        runCatching { t.join(500) }
    }

    private fun run(kind: SoundKind) {
        val rate = SoundSource.SAMPLE_RATE
        val minBytes = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBytes <= 0) return
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(maxOf(minBytes, CHUNK * 2 * 4))
                .build()
        } catch (_: Exception) {
            return
        }
        try {
            val source = SoundSource.create(kind, Random.Default, rate)
            val floats = FloatArray(CHUNK)
            val pcm = ShortArray(CHUNK)
            var gain = 0f
            track.play()
            while (running) {
                source.fill(floats, CHUNK)
                val target = targetGain
                // Ramp across the buffer in place, then convert.
                val delta = (target - gain) / CHUNK
                for (i in 0 until CHUNK) {
                    gain += delta
                    floats[i] *= gain
                }
                gain = target
                floatsToPcm(floats, CHUNK, 1f, pcm)
                if (track.write(pcm, 0, CHUNK) < 0) break
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private companion object {
        /** About 46 ms of audio at 44.1 kHz. */
        const val CHUNK = 2048
    }
}
