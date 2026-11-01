package com.fabriquefutur.dremo.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import com.fabriquefutur.dremo.R
import com.fabriquefutur.dremo.core.SceneId
import com.fabriquefutur.dremo.scene.createRenderer

private const val COLUMNS = 4
private const val PREVIEW_FPS = 12

/**
 * The home screen: a grid of scenes, each a live miniature, and Settings. The
 * scene that was open last gets focus again when the viewer comes back.
 */
@Composable
fun HomeScreen(
    scenes: List<SceneId>,
    focusScene: SceneId?,
    input: ClockInput,
    onOpenScene: (SceneId) -> Unit,
    onSettings: () -> Unit,
) {
    val requesters = remember(scenes) { scenes.associateWith { FocusRequester() } }
    LaunchedEffect(scenes, focusScene) {
        val target = requesters[focusScene] ?: requesters[scenes.first()]
        runCatching { target?.requestFocus() }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = Type.Hero.copy(fontSize = 48.sp))
                Text(stringResource(R.string.tagline), style = Type.Label)
            }
            FocusButton(stringResource(R.string.home_settings), onSettings)
        }
        // Previews run their own clocks; the shift is for the real screen only.
        CompositionLocalProvider(LocalPixelShift provides DpOffset.Zero) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                scenes.chunked(COLUMNS).forEach { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { id ->
                            SceneCard(
                                id, input,
                                Modifier.weight(1f).focusRequester(requesters.getValue(id)),
                            ) { onOpenScene(id) }
                        }
                        // Keep the last row's cards the same width as the others.
                        repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneCard(id: SceneId, input: ClockInput, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "cardScale")
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(DremoColors.Card)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
    ) {
        if (id.isClock) {
            ClockFace(id, input, Modifier.fillMaxSize())
        } else {
            val renderer = remember(id) { createRenderer(id, 0.4f) }
            if (renderer != null) SceneCanvas(renderer, PREVIEW_FPS)
        }
        // The name sits on a scrim so it reads on any scene.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
        ) {
            Text(stringResource(id.nameRes()), style = Type.BodyBold)
        }
        // Drawn last, above the preview, so the focus ring is never hidden.
        Box(
            Modifier
                .fillMaxSize()
                .border(if (focused) 4.dp else 1.dp, if (focused) DremoColors.Focus else DremoColors.Track.copy(alpha = 0.6f), shape),
        )
    }
}
