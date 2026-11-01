package com.fabriquefutur.dremo.dream

import android.service.dreams.DreamService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fabriquefutur.dremo.DremoApp
import com.fabriquefutur.dremo.core.SceneId
import com.fabriquefutur.dremo.audio.VolumeSteps
import com.fabriquefutur.dremo.ui.DremoTheme
import com.fabriquefutur.dremo.ui.SceneContent
import com.fabriquefutur.dremo.ui.ScreenCare
import com.fabriquefutur.dremo.ui.SoundEffect
import com.fabriquefutur.dremo.ui.rememberClockInput
import com.fabriquefutur.dremo.ui.rememberMinute
import java.util.Locale

/**
 * Dremo as a screensaver, for Android TV versions that still list third-party
 * dreams. It shows the scene chosen in Settings, with night dimming and pixel
 * shift, and no controls: any key or touch ends it, which is the DreamService
 * default for a non-interactive dream.
 */
class DremoDream : DreamService() {
    private val owner = DreamOwner()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        val app = application as DremoApp
        val view = ComposeView(this)
        // A dream window has no Activity, so it supplies the owners Compose looks for.
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setContent { DremoTheme { DreamContent(app) } }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        owner.resume()
    }

    override fun onDreamingStopped() {
        owner.destroy()
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        owner.destroy()
        super.onDetachedFromWindow()
    }
}

@Composable
private fun DreamContent(app: DremoApp) {
    val settings by app.settingsStore.settings.collectAsStateWithLifecycle()
    val weather by app.weather.weather.collectAsStateWithLifecycle()
    val minute by rememberMinute()
    val input = rememberClockInput(minute, settings, weather)
    val scenes = remember { SceneId.available(Locale.getDefault().language) }
    val scene = if (settings.dreamScene in scenes) settings.dreamScene else scenes.first()

    // The dream shows the cached forecast; it never fetches, so it costs no data while idle.
    ScreenCare(settings.night) {
        SceneContent(scene, input, settings.cornerClock)
    }
    val kind = scene.defaultSound.takeIf { settings.dreamSound }
    SoundEffect(app.audio, kind, VolumeSteps.gain(settings.volume))
}

/** A minimal lifecycle and saved-state owner, so a ComposeView can run inside a [DreamService]. */
private class DreamOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)

    init {
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun resume() {
        if (registry.currentState != Lifecycle.State.DESTROYED) registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        if (registry.currentState != Lifecycle.State.DESTROYED) registry.currentState = Lifecycle.State.DESTROYED
    }
}
