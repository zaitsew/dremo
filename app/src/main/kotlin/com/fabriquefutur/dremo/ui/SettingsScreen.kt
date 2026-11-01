package com.fabriquefutur.dremo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.fabriquefutur.dremo.R
import com.fabriquefutur.dremo.core.ClockFormat
import com.fabriquefutur.dremo.core.NightSchedule
import com.fabriquefutur.dremo.core.SceneId
import com.fabriquefutur.dremo.data.Settings
import com.fabriquefutur.dremo.data.SettingsStore
import com.fabriquefutur.dremo.weather.TempUnit
import kotlinx.coroutines.launch

/** Steps through [values] from [current], forwards or back, wrapping. */
private fun <T> cycle(values: List<T>, current: T, direction: Int): T {
    val i = values.indexOf(current).coerceAtLeast(0)
    return values[Math.floorMod(i + direction, values.size)]
}

@Composable
fun SettingsScreen(
    settings: Settings,
    scenes: List<SceneId>,
    store: SettingsStore,
    onCity: () -> Unit,
    onScreensaver: () -> Unit,
    onAbout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    fun change(block: (SettingsStore.MutablePrefs) -> Unit) { scope.launch { store.update(block) } }

    val formatNames = mapOf(
        ClockFormat.AUTO to stringResource(R.string.format_auto),
        ClockFormat.H12 to stringResource(R.string.format_12),
        ClockFormat.H24 to stringResource(R.string.format_24),
    )
    val unitNames = mapOf(
        TempUnit.AUTO to stringResource(R.string.unit_auto),
        TempUnit.CELSIUS to stringResource(R.string.unit_c),
        TempUnit.FAHRENHEIT to stringResource(R.string.unit_f),
    )
    val on = stringResource(R.string.state_on)
    val off = stringResource(R.string.state_off)
    fun onOff(v: Boolean) = if (v) on else off

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
    ) {
        Text(stringResource(R.string.settings_title), style = Type.Title)
        Text(stringResource(R.string.settings_hint), style = Type.Label, modifier = Modifier.padding(top = 4.dp))

        SectionTitle(stringResource(R.string.section_display))
        val formats = ClockFormat.entries
        SettingRow(
            stringResource(R.string.setting_clock_format),
            formatNames.getValue(settings.clockFormat),
            Modifier.focusRequester(first),
            onNext = { change { it.clockFormat(cycle(formats, settings.clockFormat, 1)) } },
            onPrevious = { change { it.clockFormat(cycle(formats, settings.clockFormat, -1)) } },
        )

        SectionTitle(stringResource(R.string.section_weather))
        SettingRow(
            stringResource(R.string.setting_city),
            settings.city?.label ?: stringResource(R.string.city_not_set),
            onNext = onCity,
        )
        val units = TempUnit.entries
        SettingRow(
            stringResource(R.string.setting_temp_unit),
            unitNames.getValue(settings.tempUnit),
            onNext = { change { it.tempUnit(cycle(units, settings.tempUnit, 1)) } },
            onPrevious = { change { it.tempUnit(cycle(units, settings.tempUnit, -1)) } },
        )
        SettingRow(
            stringResource(R.string.setting_show_weather),
            onOff(settings.showWeather),
            onNext = { change { it.showWeather(!settings.showWeather) } },
        )
        if (settings.city != null) {
            ActionRow(stringResource(R.string.setting_city_remove), onClick = { change { it.city(null) } })
        }
        Text(
            stringResource(R.string.weather_credit),
            style = Type.Label,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )

        SectionTitle(stringResource(R.string.section_night))
        SettingRow(
            stringResource(R.string.setting_night),
            onOff(settings.nightDim),
            onNext = { change { it.nightDim(!settings.nightDim) } },
        )
        SettingRow(
            stringResource(R.string.setting_night_from),
            NightSchedule.label(settings.nightStartMinute),
            onNext = { change { it.nightStart(NightSchedule.step(settings.nightStartMinute, 1)) } },
            onPrevious = { change { it.nightStart(NightSchedule.step(settings.nightStartMinute, -1)) } },
        )
        SettingRow(
            stringResource(R.string.setting_night_until),
            NightSchedule.label(settings.nightEndMinute),
            onNext = { change { it.nightEnd(NightSchedule.step(settings.nightEndMinute, 1)) } },
            onPrevious = { change { it.nightEnd(NightSchedule.step(settings.nightEndMinute, -1)) } },
        )

        SectionTitle(stringResource(R.string.section_screensaver))
        val dreamScene = if (settings.dreamScene in scenes) settings.dreamScene else scenes.first()
        SettingRow(
            stringResource(R.string.setting_dream_scene),
            stringResource(dreamScene.nameRes()),
            onNext = { change { it.dreamScene(cycle(scenes, dreamScene, 1)) } },
            onPrevious = { change { it.dreamScene(cycle(scenes, dreamScene, -1)) } },
        )
        SettingRow(
            stringResource(R.string.setting_dream_sound),
            onOff(settings.dreamSound),
            onNext = { change { it.dreamSound(!settings.dreamSound) } },
        )
        ActionRow(stringResource(R.string.setting_screensaver), onClick = onScreensaver)

        SectionTitle(stringResource(R.string.setting_about))
        ActionRow(stringResource(R.string.setting_about), onClick = onAbout)
    }
}
