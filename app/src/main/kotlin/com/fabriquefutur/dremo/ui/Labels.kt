package com.fabriquefutur.dremo.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.fabriquefutur.dremo.R
import com.fabriquefutur.dremo.audio.SoundKind
import com.fabriquefutur.dremo.core.ClockText
import com.fabriquefutur.dremo.core.SceneId
import com.fabriquefutur.dremo.data.Settings
import com.fabriquefutur.dremo.weather.Condition
import com.fabriquefutur.dremo.weather.TempUnit
import com.fabriquefutur.dremo.weather.Weather
import com.fabriquefutur.dremo.weather.WeatherRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@StringRes
fun SceneId.nameRes(): Int = when (this) {
    SceneId.CLOCK_DIGITAL -> R.string.scene_clock_digital
    SceneId.CLOCK_FLIP -> R.string.scene_clock_flip
    SceneId.CLOCK_ANALOG -> R.string.scene_clock_analog
    SceneId.CLOCK_WORDS -> R.string.scene_clock_words
    SceneId.RAIN -> R.string.scene_rain
    SceneId.FIREPLACE -> R.string.scene_fireplace
    SceneId.AURORA -> R.string.scene_aurora
    SceneId.STARS -> R.string.scene_stars
    SceneId.OCEAN -> R.string.scene_ocean
    SceneId.LAVA -> R.string.scene_lava
}

@StringRes
fun SoundKind.nameRes(): Int = when (this) {
    SoundKind.WHITE -> R.string.sound_white
    SoundKind.PINK -> R.string.sound_pink
    SoundKind.BROWN -> R.string.sound_brown
    SoundKind.RAIN -> R.string.sound_rain
    SoundKind.FIRE -> R.string.sound_fire
    SoundKind.OCEAN -> R.string.sound_ocean
}

@StringRes
fun Condition.nameRes(): Int = when (this) {
    Condition.CLEAR -> R.string.cond_clear
    Condition.PARTLY_CLOUDY -> R.string.cond_partly
    Condition.CLOUDY -> R.string.cond_cloudy
    Condition.FOG -> R.string.cond_fog
    Condition.DRIZZLE -> R.string.cond_drizzle
    Condition.RAIN -> R.string.cond_rain
    Condition.SNOW -> R.string.cond_snow
    Condition.THUNDER -> R.string.cond_thunder
}

/** The system's 24-hour preference, resolved against the user's own setting. */
fun use24h(context: Context, settings: Settings): Boolean =
    settings.clockFormat.use24h(DateFormat.is24HourFormat(context))

fun dateText(time: LocalDateTime, locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    return ClockText.capitalised(time.format(DateTimeFormatter.ofPattern(pattern, locale)), locale)
}

/** True when temperatures show in Fahrenheit for this viewer. */
fun fahrenheit(settings: Settings, locale: Locale): Boolean = settings.tempUnit.isFahrenheit(locale.country)

/** The weather lines for a clock, or null when no city is set, weather is hidden, or nothing has been fetched yet. */
fun weatherText(context: Context, settings: Settings, weather: Weather?, locale: Locale, use24h: Boolean): WeatherText? {
    if (!settings.showWeather || !WeatherRepository.matches(weather, settings.city)) return null
    weather ?: return null
    val f = fahrenheit(settings, locale)
    val temp = TempUnit.format(weather.tempC, f)
    val condition = context.getString(Condition.fromCode(weather.code).nameRes())
    val parts = mutableListOf(temp, condition)
    if (weather.highC != null && weather.lowC != null) {
        parts += context.getString(
            R.string.weather_range,
            TempUnit.format(weather.highC, f),
            TempUnit.format(weather.lowC, f),
        )
    }
    val hours = weather.hours.takeIf { it.isNotEmpty() }?.joinToString("   ") {
        "${ClockText.hourLabel(it.hour, use24h)} ${TempUnit.format(it.tempC, f)}"
    }
    return WeatherText(parts.joinToString("  ·  "), hours, temp)
}

/** Builds the [ClockInput] a clock face or corner clock draws from. */
@Composable
fun rememberClockInput(time: LocalDateTime, settings: Settings, weather: Weather?): ClockInput {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val h24 = use24h(context, settings)
    return remember(time, settings.clockFormat, settings.tempUnit, settings.city, settings.showWeather, weather) {
        ClockInput(
            time = time,
            use24h = h24,
            locale = locale,
            dateText = dateText(time, locale),
            weather = weatherText(context, settings, weather, locale, h24),
        )
    }
}
