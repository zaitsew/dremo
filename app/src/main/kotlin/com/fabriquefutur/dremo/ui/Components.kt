package com.fabriquefutur.dremo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

private val ButtonShape = RoundedCornerShape(14.dp)
private val RowShape = RoundedCornerShape(14.dp)

/**
 * A clickable surface that shows D-pad focus with a bright border. Everything
 * the D-pad can reach in Dremo is built on this, so focus always looks the same.
 */
@Composable
fun FocusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        modifier
            .clip(ButtonShape)
            .background(
                when {
                    primary -> if (focused) DremoColors.Accent else DremoColors.Accent.copy(alpha = 0.85f)
                    focused -> DremoColors.CardHigh
                    else -> DremoColors.Card
                },
            )
            .border(3.dp, if (focused) DremoColors.Focus else DremoColors.Track, ButtonShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
fun FocusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    FocusButton(onClick, modifier, primary) {
        Text(
            text,
            style = Type.BodyBold.copy(color = if (primary) DremoColors.Background else DremoColors.Text),
        )
    }
}

/**
 * A settings row: a title on the left, the current value on the right. OK steps
 * to the next value; left and right step back and forward, so a long list of
 * choices is quick to walk.
 */
@Composable
fun SettingRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    onPrevious: (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(if (focused) DremoColors.CardHigh else DremoColors.Card)
            .border(3.dp, if (focused) DremoColors.Focus else DremoColors.Card, RowShape)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> { (onPrevious ?: onNext)(); true }
                        Key.DirectionRight -> { onNext(); true }
                        else -> false
                    }
                }
            }
            .clickable(interactionSource = source, indication = null, onClick = onNext)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = Type.Body)
        Text(value, style = Type.BodyBold.copy(color = DremoColors.Accent))
    }
}

/** A row that only opens something or runs an action: no value, a chevron-style hint. */
@Composable
fun ActionRow(title: String, modifier: Modifier = Modifier, hint: String? = null, onClick: () -> Unit) {
    SettingRow(title, hint ?: "›", modifier, onNext = onClick)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Type.Section, modifier = modifier.padding(top = 18.dp, bottom = 4.dp, start = 4.dp))
}

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
