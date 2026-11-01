package com.fabriquefutur.dremo

import android.app.Application
import com.fabriquefutur.dremo.audio.AudioEngine
import com.fabriquefutur.dremo.data.SettingsStore
import com.fabriquefutur.dremo.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Process-wide singletons, shared by the activity and the screensaver service. */
class DremoApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settingsStore: SettingsStore by lazy { SettingsStore(this, scope) }
    val weather: WeatherRepository by lazy { WeatherRepository(this) }
    val audio: AudioEngine by lazy { AudioEngine() }

    override fun onCreate() {
        super.onCreate()
        scope.launch { weather.loadCache() }
    }
}
