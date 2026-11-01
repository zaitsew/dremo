package com.fabriquefutur.dremo.core

enum class SleepPhase {
    /** No timer is set. */
    OFF,

    /** Counting down at full volume. */
    COUNTING,

    /** The last [SleepTimerState.FADE_MS]: the sound fades to silence. */
    FADING,

    /** The timer ran out: sound is off and the screen shows a dim clock. */
    DIM,
}

/**
 * The sleep timer as a pure value. The caller supplies a monotonic clock in
 * milliseconds ("now") so the state machine is testable without waiting.
 */
data class SleepTimerState(val minutes: Int = 0, val startedAtMs: Long = 0L) {
    val isSet: Boolean get() = minutes > 0

    private val durationMs: Long get() = minutes * 60_000L
    private val endAtMs: Long get() = startedAtMs + durationMs

    fun phase(nowMs: Long): SleepPhase = when {
        !isSet -> SleepPhase.OFF
        nowMs >= endAtMs -> SleepPhase.DIM
        nowMs >= endAtMs - FADE_MS -> SleepPhase.FADING
        else -> SleepPhase.COUNTING
    }

    /** 1 at full volume, falling linearly to 0 across the fade, 0 once the timer ran out. */
    fun volumeFactor(nowMs: Long): Float = when (phase(nowMs)) {
        SleepPhase.OFF, SleepPhase.COUNTING -> 1f
        SleepPhase.FADING -> ((endAtMs - nowMs).toFloat() / FADE_MS).coerceIn(0f, 1f)
        SleepPhase.DIM -> 0f
    }

    fun remainingMs(nowMs: Long): Long = if (isSet) (endAtMs - nowMs).coerceAtLeast(0L) else 0L

    /** The next choice in 15, 30, 60, 90, off, restarted from [nowMs]. */
    fun cycled(nowMs: Long): SleepTimerState {
        val next = SleepOptions.next(minutes)
        return if (next == 0) SleepTimerState() else SleepTimerState(next, nowMs)
    }

    fun cancelled(): SleepTimerState = SleepTimerState()

    companion object {
        /** The fade lasts 30 s, and the shortest timer is 15 min, so it always fits. */
        const val FADE_MS = 30_000L
    }
}

object SleepOptions {
    val MINUTES = listOf(15, 30, 60, 90)

    /** 0 means off. Anything that is not an option starts the cycle again at the first. */
    fun next(current: Int): Int {
        if (current <= 0) return MINUTES.first()
        val index = MINUTES.indexOf(current)
        return if (index < 0) MINUTES.first() else MINUTES.getOrElse(index + 1) { 0 }
    }
}
