package com.fabriquefutur.dremo.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.tv.material3.Text
import com.fabriquefutur.dremo.R
import com.fabriquefutur.dremo.audio.AudioEngine
import com.fabriquefutur.dremo.audio.SoundKind
import com.fabriquefutur.dremo.audio.VolumeSteps
import com.fabriquefutur.dremo.core.SceneId
import com.fabriquefutur.dremo.core.SleepPhase
import com.fabriquefutur.dremo.core.SleepTimerState
import com.fabriquefutur.dremo.core.SoundChoices
import com.fabriquefutur.dremo.core.SoundToggle
import com.fabriquefutur.dremo.data.Settings
import com.fabriquefutur.dremo.data.SettingsStore
import com.fabriquefutur.dremo.scene.createRenderer
import com.fabriquefutur.dremo.weather.Weather
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Frames a second for a full-screen ambience scene. 30 is smooth and gentle on a weak TV chip. */
private const val SCENE_FPS = 30

/** A scene with its optional corner clock; no controls. Shared by the app and the screensaver. */
@Composable
fun SceneContent(scene: SceneId, input: ClockInput, cornerClock: Boolean, modifier: Modifier = Modifier) {
    if (scene.isClock) {
        ClockFace(scene, input, modifier)
    } else {
        val renderer = remember(scene) { createRenderer(scene, 1f) }
        Box(modifier.fillMaxSize()) {
            if (renderer != null) SceneCanvas(renderer, SCENE_FPS)
            if (cornerClock) CornerClock(input)
        }
    }
}

/**
 * Plays [kind] while the screen is started. The engine stops when the app goes to
 * the background (and when this leaves the composition), and starts again when
 * the screen returns. [gain] is the linear volume, set without restarting.
 */
@Composable
fun SoundEffect(audio: AudioEngine, kind: SoundKind?, gain: Float) {
    if (kind != null && gain > 0f) {
        LifecycleStartEffect(kind, audio) {
            audio.start(kind)
            onStopOrDispose { audio.stop() }
        }
    }
    LaunchedEffect(gain) { audio.targetGain = gain }
}

private const val OVERLAY_MS = 7000L

/**
 * The interactive scene: any key shows the overlay, Back leaves. Owns the sleep
 * timer, the sound choice for this viewing, and the overlay's auto-hide.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneScreen(
    scene: SceneId,
    settings: Settings,
    weather: Weather?,
    store: SettingsStore,
    audio: AudioEngine,
    onExit: () -> Unit,
) {
    BackHandler(onBack = onExit)
    val scope = rememberCoroutineScope()

    // The TV must not dim or start its own screensaver while a scene runs.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val minute by rememberMinute()
    val input = rememberClockInput(minute, settings, weather)
    val elapsed = rememberElapsedMs()

    var timer by remember { mutableStateOf(SleepTimerState()) }
    val phase by remember { derivedStateOf { timer.phase(elapsed.value) } }
    var soundOverride by remember(scene) { mutableStateOf<SoundKind?>(null) }
    val kind = soundOverride ?: scene.defaultSound

    var overlayVisible by remember { mutableStateOf(false) }
    var lastActivity by remember { mutableLongStateOf(0L) }
    fun poke() { lastActivity = System.nanoTime() }

    LaunchedEffect(overlayVisible, lastActivity) {
        if (overlayVisible) {
            delay(OVERLAY_MS)
            overlayVisible = false
        }
    }

    val gain by remember(settings.volume) {
        derivedStateOf { VolumeSteps.gain(settings.volume) * timer.volumeFactor(elapsed.value) }
    }
    SoundEffect(audio, kind.takeIf { settings.soundEnabled && phase != SleepPhase.DIM }, gain)

    // Focus: the root takes keys while the overlay is hidden; the overlay takes them while it shows.
    val rootFocus = remember { FocusRequester() }
    val firstButton = remember { FocusRequester() }
    LaunchedEffect(overlayVisible) {
        runCatching { if (overlayVisible) firstButton.requestFocus() else rootFocus.requestFocus() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                val isBack = event.key.nativeKeyCode == KeyEvent.KEYCODE_BACK
                if (event.type != KeyEventType.KeyDown || isBack) {
                    false
                } else if (phase == SleepPhase.DIM) {
                    // Any key wakes the scene: the timer is cancelled and the overlay shows.
                    timer = timer.cancelled()
                    overlayVisible = true
                    poke()
                    true
                } else if (!overlayVisible) {
                    overlayVisible = true
                    poke()
                    true
                } else {
                    poke()
                    false
                }
            }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        if (phase == SleepPhase.DIM) {
            DigitalClock(input, tint = Color.White.copy(alpha = 0.26f))
        } else {
            SceneContent(scene, input, settings.cornerClock)
        }

        AnimatedVisibility(
            visible = overlayVisible && phase != SleepPhase.DIM,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                    .padding(horizontal = SafeHorizontal, vertical = SafeVertical)
                    .padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(scene.nameRes()), style = Type.Title)
                val hasCity = settings.city != null
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // The first toggle is the corner clock on a scene, the weather line on a clock face.
                    if (scene.isClock) {
                        if (hasCity) {
                            FocusButton(
                                stringResource(R.string.overlay_weather, onOff(settings.showWeather)),
                                { poke(); scope.launch { store.update { it.showWeather(!settings.showWeather) } } },
                                Modifier.focusRequester(firstButton),
                            )
                        }
                    } else {
                        FocusButton(
                            stringResource(R.string.overlay_clock, onOff(settings.cornerClock)),
                            { poke(); scope.launch { store.update { it.cornerClock(!settings.cornerClock) } } },
                            Modifier.focusRequester(firstButton),
                        )
                    }
                    FocusButton(
                        stringResource(R.string.overlay_sound, onOff(SoundToggle.isOn(settings.soundEnabled, kind))),
                        {
                            poke()
                            val next = SoundToggle.toggle(settings.soundEnabled, soundOverride, scene.defaultSound)
                            soundOverride = next.choice
                            scope.launch { store.update { it.soundEnabled(next.enabled) } }
                        },
                        if (scene.isClock && !hasCity) Modifier.focusRequester(firstButton) else Modifier,
                    )
                    FocusButton(
                        "−",
                        { poke(); scope.launch { store.update { it.volume(settings.volume - 1) } } },
                    )
                    Text(
                        stringResource(R.string.overlay_volume, settings.volume),
                        style = Type.Body,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    FocusButton(
                        "+",
                        { poke(); scope.launch { store.update { it.volume(settings.volume + 1) } } },
                    )
                    FocusButton(
                        stringResource(
                            R.string.overlay_sound_kind,
                            kind?.let { stringResource(it.nameRes()) }
                                ?: stringResource(if (soundOverride == null && scene.isClock) R.string.sound_none else R.string.sound_default),
                        ),
                        { poke(); soundOverride = SoundChoices.next(soundOverride) },
                    )
                    FocusButton(
                        sleepLabel(timer, elapsed.value),
                        { poke(); timer = timer.cycled(elapsed.value) },
                    )
                }
            }
        }
    }
}

@Composable
private fun onOff(on: Boolean): String = stringResource(if (on) R.string.state_on else R.string.state_off)

@Composable
private fun sleepLabel(timer: SleepTimerState, nowMs: Long): String =
    if (!timer.isSet) {
        stringResource(R.string.overlay_sleep_off)
    } else {
        val left = ((timer.remainingMs(nowMs) + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
        stringResource(R.string.overlay_sleep_left, left)
    }
