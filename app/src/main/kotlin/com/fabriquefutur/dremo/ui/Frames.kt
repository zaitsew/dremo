package com.fabriquefutur.dremo.ui

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.fabriquefutur.dremo.core.NightSchedule
import com.fabriquefutur.dremo.core.PixelShift
import com.fabriquefutur.dremo.scene.SceneRenderer
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** The wall-clock time, truncated to the minute, updated on the minute. */
@Composable
fun rememberMinute(): State<LocalDateTime> = produceState(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)) {
    while (true) {
        val now = LocalDateTime.now()
        value = now.truncatedTo(ChronoUnit.MINUTES)
        val sinceMinute = now.second * 1000L + now.nano / 1_000_000L
        delay(60_000L - sinceMinute + 20L)
    }
}

/** A monotonic clock in milliseconds, updated every second. */
@Composable
fun rememberElapsedMs(): State<Long> = produceState(SystemClock.elapsedRealtime()) {
    while (true) {
        delay(1000)
        value = SystemClock.elapsedRealtime()
    }
}

/**
 * Runs a [SceneRenderer] on the frame clock at no more than [fps] frames a
 * second. Only the draw phase re-runs per frame; nothing recomposes.
 */
@Composable
fun SceneCanvas(renderer: SceneRenderer, fps: Int, modifier: Modifier = Modifier) {
    var frame by remember(renderer) { mutableLongStateOf(0L) }
    LaunchedEffect(renderer, fps) {
        val interval = 1_000_000_000L / fps
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last == 0L) last = now
                val elapsed = now - last
                if (elapsed >= interval) {
                    renderer.advance(elapsed / 1_000_000_000f)
                    last = now
                    frame++
                }
            }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        // Reading the counter here, in the draw scope, is what schedules the redraw.
        if (frame >= 0L) renderer.draw(this)
    }
}

/**
 * Screen care for OLED and LCD panels alike. Provides the once-a-minute pixel
 * shift to everything inside, and draws the night-dimming overlay on top.
 */
@Composable
fun ScreenCare(night: NightSchedule, content: @Composable () -> Unit) {
    val now by rememberMinute()
    val step = remember(now) { now.atZone(ZoneId.systemDefault()).toEpochSecond() / 60 }
    val cell = PixelShift.cell(step)
    val shift = DpOffset(CELL * cell.first, CELL * cell.second)
    val isNight = night.isNight(now.hour * 60 + now.minute)
    val alpha by animateFloatAsState(if (isNight) NightSchedule.DIM_ALPHA else 0f, tween(3000), label = "nightDim")
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalPixelShift provides shift) { content() }
        // Drawn, not composed, so the fade does not recompose the screen.
        Box(Modifier.fillMaxSize().drawBehind { if (alpha > 0f) drawRect(Color.Black, alpha = alpha) })
    }
}

private val CELL = 6.dp
