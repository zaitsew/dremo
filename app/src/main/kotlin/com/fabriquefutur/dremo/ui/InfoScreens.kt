package com.fabriquefutur.dremo.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.fabriquefutur.dremo.R
import com.fabriquefutur.dremo.data.Settings

/** The package-and-class form `adb shell settings` expects for a dream. */
const val DREAM_COMPONENT = "com.fabriquefutur.dremo/.dream.DremoDream"

/** The Google TV screensaver Dremo replaces. Writing this back restores it. */
const val GOOGLE_TV_DREAM_COMPONENT = "com.google.android.apps.tv.dreamx/.service.Backdrop"

const val SOURCE_URL = "https://github.com/zaitsew/dremo"

fun setScreensaverCommand(): String = "adb shell settings put secure screensaver_components $DREAM_COMPONENT"

fun restoreScreensaverCommand(): String = "adb shell settings put secure screensaver_components $GOOGLE_TV_DREAM_COMPONENT"

/** A block of text that takes focus, so the D-pad can scroll a screen with no buttons on it. */
@Composable
private fun FocusText(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .border(3.dp, if (focused) DremoColors.Focus else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp))
            .focusable()
            .padding(12.dp),
    ) { content() }
}

@Composable
fun ScreensaverScreen(settings: Settings) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
    ) {
        Text(stringResource(R.string.saver_title), style = Type.Title)
        FocusText(Modifier.focusRequester(first)) {
            Text(stringResource(R.string.saver_intro), style = Type.Body)
        }
        FocusText {
            Text(stringResource(R.string.saver_older_title), style = Type.Section)
            Text(stringResource(R.string.saver_older_body), style = Type.Body)
        }
        FocusText {
            Text(stringResource(R.string.saver_google_title), style = Type.Section)
            Text(stringResource(R.string.saver_google_body), style = Type.Body)
            Text(setScreensaverCommand(), style = Type.Mono, modifier = Modifier.padding(top = 8.dp))
        }
        FocusText {
            Text(stringResource(R.string.saver_restore_title), style = Type.Section)
            Text(restoreScreensaverCommand(), style = Type.Mono, modifier = Modifier.padding(top = 4.dp))
        }
        FocusText {
            Text(
                stringResource(R.string.saver_note, stringResource(settings.dreamScene.nameRes())),
                style = Type.Label,
            )
        }
    }
}

fun versionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
} catch (_: Exception) {
    ""
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
    ) {
        Text(stringResource(R.string.about_title), style = Type.Title)
        FocusText(Modifier.focusRequester(first)) {
            Text(stringResource(R.string.about_version, versionName(context)), style = Type.Body)
            Text(stringResource(R.string.about_by), style = Type.Label)
        }
        FocusText {
            Text(stringResource(R.string.about_license), style = Type.Body)
            Text(stringResource(R.string.about_source), style = Type.Label)
            Text(SOURCE_URL, style = Type.Mono)
        }
        FocusText {
            Text(stringResource(R.string.about_credit), style = Type.Body)
            Text(stringResource(R.string.about_privacy), style = Type.Label, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
