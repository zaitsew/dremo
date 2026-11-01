package com.fabriquefutur.dremo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.fabriquefutur.dremo.R
import com.fabriquefutur.dremo.weather.City
import com.fabriquefutur.dremo.weather.WeatherRepository
import kotlinx.coroutines.delay
import java.util.Locale

private sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data object Failed : SearchState
    data class Done(val cities: List<City>) : SearchState
}

/**
 * Finds a city for the weather line. The text field uses the TV's own keyboard:
 * press OK on the field to open it. Searching starts by itself 0.7 s after the
 * last key, and the Search button does the same at once.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CitySearchScreen(
    current: City?,
    repository: WeatherRepository,
    onChosen: (City) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var searchNow by remember { mutableStateOf(0) }
    var handledSearch by remember { mutableStateOf(0) }
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val language = remember { Locale.getDefault().language }
    var fieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }

    LaunchedEffect(query, searchNow) {
        if (query.trim().length < 2) {
            state = SearchState.Idle
            return@LaunchedEffect
        }
        // A pending search is dropped when the text changes: the effect restarts.
        // The Search button (a changed counter) skips the wait.
        val manual = searchNow != handledSearch
        handledSearch = searchNow
        if (!manual) delay(700)
        state = SearchState.Searching
        val result = repository.searchCities(query, language)
        state = if (result == null) SearchState.Failed else SearchState.Done(result)
    }

    ScreenColumn(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.city_title), style = Type.Title)
        Text(stringResource(R.string.city_hint), style = Type.Label)
        if (current != null) {
            Text(stringResource(R.string.city_current, current.label), style = Type.Label)
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(DremoColors.Card)
                    .border(3.dp, if (fieldFocused) DremoColors.Focus else DremoColors.Track, shape)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = Type.Body,
                    cursorBrush = SolidColor(DremoColors.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchNow++; keyboard?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fieldFocus)
                        .onFocusChanged { fieldFocused = it.isFocused }
                        // On a TV, OK on a focused field is what opens the keyboard.
                        .onKeyEvent { e ->
                            if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter)) {
                                keyboard?.show()
                                true
                            } else {
                                false
                            }
                        },
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(stringResource(R.string.city_placeholder), style = Type.Body.copy(color = DremoColors.Muted))
                        }
                        inner()
                    },
                )
            }
            FocusButton(stringResource(R.string.city_search), { searchNow++; keyboard?.hide() }, primary = true)
        }

        when (val s = state) {
            SearchState.Idle -> Unit
            SearchState.Searching -> Text(stringResource(R.string.city_searching), style = Type.Label)
            SearchState.Failed -> Text(stringResource(R.string.city_error), style = Type.Body.copy(color = DremoColors.Error))
            is SearchState.Done -> {
                if (s.cities.isEmpty()) {
                    Text(stringResource(R.string.city_none), style = Type.Label)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.cities.forEach { city ->
                        SettingRow(
                            city.label,
                            "›",
                            onNext = { onChosen(city) },
                        )
                    }
                }
            }
        }
    }
}
