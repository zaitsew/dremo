package com.fabriquefutur.dremo.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fabriquefutur.dremo.audio.VolumeSteps
import com.fabriquefutur.dremo.core.ClockFormat
import com.fabriquefutur.dremo.core.NightSchedule
import com.fabriquefutur.dremo.core.SceneId
import com.fabriquefutur.dremo.weather.City
import com.fabriquefutur.dremo.weather.TempUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Everything the viewer can set. The defaults are what a fresh install uses. */
data class Settings(
    val clockFormat: ClockFormat = ClockFormat.AUTO,
    val tempUnit: TempUnit = TempUnit.AUTO,
    val city: City? = null,
    val showWeather: Boolean = true,
    val cornerClock: Boolean = true,
    val soundEnabled: Boolean = true,
    val volume: Int = VolumeSteps.DEFAULT_STEP,
    val nightDim: Boolean = true,
    val nightStartMinute: Int = NightSchedule.DEFAULT_START,
    val nightEndMinute: Int = NightSchedule.DEFAULT_END,
    val dreamScene: SceneId = SceneId.CLOCK_DIGITAL,
    val dreamSound: Boolean = false,
) {
    val night: NightSchedule
        get() = NightSchedule(nightDim, nightStartMinute, nightEndMinute)
}

// A corrupt file falls back to the defaults instead of crashing every launch.
private val Context.settingsStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class SettingsStore(context: Context, scope: CoroutineScope) {
    private val store = context.applicationContext.settingsStore
    private val json = Json { ignoreUnknownKeys = true }

    /** Always has a value: the defaults until DataStore has been read. */
    val settings: StateFlow<Settings> = store.data
        .catch { emit(emptyPreferences()) }
        .map(::decode)
        .stateIn(scope, SharingStarted.Eagerly, Settings())

    private fun decode(p: Preferences): Settings {
        val d = Settings()
        val city = p[CITY]?.let {
            try {
                json.decodeFromString<City>(it)
            } catch (_: Exception) {
                null
            }
        }
        return Settings(
            clockFormat = ClockFormat.fromKey(p[CLOCK_FORMAT]),
            tempUnit = TempUnit.fromKey(p[TEMP_UNIT]),
            city = city,
            showWeather = p[SHOW_WEATHER] ?: d.showWeather,
            cornerClock = p[CORNER_CLOCK] ?: d.cornerClock,
            soundEnabled = p[SOUND] ?: d.soundEnabled,
            volume = VolumeSteps.clamp(p[VOLUME] ?: d.volume),
            nightDim = p[NIGHT_DIM] ?: d.nightDim,
            nightStartMinute = (p[NIGHT_START] ?: d.nightStartMinute).coerceIn(0, NightSchedule.MINUTES_PER_DAY - 1),
            nightEndMinute = (p[NIGHT_END] ?: d.nightEndMinute).coerceIn(0, NightSchedule.MINUTES_PER_DAY - 1),
            dreamScene = SceneId.fromKey(p[DREAM_SCENE]) ?: d.dreamScene,
            dreamSound = p[DREAM_SOUND] ?: d.dreamSound,
        )
    }

    suspend fun update(block: (MutablePrefs) -> Unit) {
        store.edit { block(MutablePrefs(it, json)) }
    }

    /** A small typed writer, so call sites do not touch preference keys. */
    class MutablePrefs(private val p: androidx.datastore.preferences.core.MutablePreferences, private val json: Json) {
        fun clockFormat(v: ClockFormat) { p[CLOCK_FORMAT] = v.key }
        fun tempUnit(v: TempUnit) { p[TEMP_UNIT] = v.key }
        fun city(v: City?) { if (v == null) p.remove(CITY) else p[CITY] = json.encodeToString(v) }
        fun showWeather(v: Boolean) { p[SHOW_WEATHER] = v }
        fun cornerClock(v: Boolean) { p[CORNER_CLOCK] = v }
        fun soundEnabled(v: Boolean) { p[SOUND] = v }
        fun volume(v: Int) { p[VOLUME] = VolumeSteps.clamp(v) }
        fun nightDim(v: Boolean) { p[NIGHT_DIM] = v }
        fun nightStart(v: Int) { p[NIGHT_START] = v }
        fun nightEnd(v: Int) { p[NIGHT_END] = v }
        fun dreamScene(v: SceneId) { p[DREAM_SCENE] = v.key }
        fun dreamSound(v: Boolean) { p[DREAM_SOUND] = v }
    }

    private companion object {
        val CLOCK_FORMAT = stringPreferencesKey("clock_format")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val CITY = stringPreferencesKey("city")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val CORNER_CLOCK = booleanPreferencesKey("corner_clock")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VOLUME = intPreferencesKey("volume")
        val NIGHT_DIM = booleanPreferencesKey("night_dim")
        val NIGHT_START = intPreferencesKey("night_start")
        val NIGHT_END = intPreferencesKey("night_end")
        val DREAM_SCENE = stringPreferencesKey("dream_scene")
        val DREAM_SOUND = booleanPreferencesKey("dream_sound")
    }
}
