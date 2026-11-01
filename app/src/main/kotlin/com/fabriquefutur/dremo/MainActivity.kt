package com.fabriquefutur.dremo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.fabriquefutur.dremo.core.SceneId
import com.fabriquefutur.dremo.ui.AboutScreen
import com.fabriquefutur.dremo.ui.DremoTheme
import com.fabriquefutur.dremo.ui.CitySearchScreen
import com.fabriquefutur.dremo.ui.HomeScreen
import com.fabriquefutur.dremo.ui.SceneScreen
import com.fabriquefutur.dremo.ui.ScreenCare
import com.fabriquefutur.dremo.ui.ScreensaverScreen
import com.fabriquefutur.dremo.ui.SettingsScreen
import com.fabriquefutur.dremo.ui.rememberClockInput
import com.fabriquefutur.dremo.ui.rememberMinute
import com.fabriquefutur.dremo.weather.RefreshPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** Where the viewer is. Back goes one level up; Back on the home screen leaves the app. */
private sealed interface Screen {
    data object Home : Screen
    data class Scene(val id: SceneId) : Screen
    data object Settings : Screen
    data object City : Screen
    data object Screensaver : Screen
    data object About : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DremoApp
        setContent { DremoTheme { DremoRoot(app) } }
    }
}

@Composable
private fun DremoRoot(app: DremoApp) {
    val settings by app.settingsStore.settings.collectAsStateWithLifecycle()
    val weather by app.weather.weather.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scenes = remember { SceneId.available(Locale.getDefault().language) }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var lastScene by remember { mutableStateOf<SceneId?>(null) }

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (screen) {
            is Screen.City, Screen.Screensaver, Screen.About -> Screen.Settings
            else -> Screen.Home
        }
    }

    WeatherRefresher(app, settings.city)

    ScreenCare(settings.night) {
        Box(Modifier.fillMaxSize()) {
            when (val s = screen) {
                Screen.Home -> {
                    val minute by rememberMinute()
                    // The previews carry no weather: the cards are too small to read it.
                    val input = rememberClockInput(minute, settings, null)
                    HomeScreen(
                        scenes = scenes,
                        focusScene = lastScene,
                        input = input,
                        onOpenScene = { lastScene = it; screen = Screen.Scene(it) },
                        onSettings = { screen = Screen.Settings },
                    )
                }
                is Screen.Scene -> SceneScreen(
                    scene = s.id,
                    settings = settings,
                    weather = weather,
                    store = app.settingsStore,
                    audio = app.audio,
                    onExit = { screen = Screen.Home },
                )
                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    scenes = scenes,
                    store = app.settingsStore,
                    onCity = { screen = Screen.City },
                    onScreensaver = { screen = Screen.Screensaver },
                    onAbout = { screen = Screen.About },
                )
                Screen.City -> CitySearchScreen(
                    current = settings.city,
                    repository = app.weather,
                    onChosen = { city ->
                        scope.launch { app.settingsStore.update { it.city(city) } }
                        screen = Screen.Settings
                    },
                )
                Screen.Screensaver -> ScreensaverScreen(settings)
                Screen.About -> AboutScreen()
            }
        }
    }
}

/**
 * Keeps the forecast fresh while the app is on screen: it fetches when the
 * cache is older than 30 minutes, then waits out the rest of the interval.
 * Nothing runs in the background, and with no city chosen nothing is sent.
 */
@Composable
fun WeatherRefresher(app: DremoApp, city: com.fabriquefutur.dremo.weather.City?) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(city, lifecycle) {
        if (city == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val cached = app.weather.weather.value
                val last = if (com.fabriquefutur.dremo.weather.WeatherRepository.matches(cached, city)) cached?.fetchedAtMs ?: 0L else 0L
                val wait = RefreshPolicy.delayUntilNext(last, System.currentTimeMillis())
                if (wait > 0L) {
                    delay(wait)
                } else {
                    val ok = app.weather.refresh(city)
                    // On a failure, try again in a minute rather than at the next interval.
                    delay(if (ok) RefreshPolicy.INTERVAL_MS else RefreshPolicy.MIN_WAIT_MS)
                }
            }
        }
    }
}
