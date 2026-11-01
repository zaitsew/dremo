package com.fabriquefutur.dremo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** A deep, low-glare palette: the screen runs for hours in a dim room. */
object DremoColors {
    val Background = Color(0xFF05070D)
    val Card = Color(0xFF111626)
    val CardHigh = Color(0xFF1C2338)
    val Text = Color(0xFFF2F0EA)
    val Muted = Color(0xFFB4B9C9)
    val Accent = Color(0xFFFDE68A)
    val AccentSoft = Color(0xFFA5B4FC)
    val Focus = Color(0xFFFFFFFF)
    val Track = Color(0xFF2A3350)
    val Error = Color(0xFFFCA5A5)
    val ClockText = Color(0xFFF4EFE4)
}

/** Type scale for reading from about 3 m. Nothing is smaller than 18 sp. */
object Type {
    val Hero = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Light, color = DremoColors.Text)
    val Title = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = DremoColors.Text)
    val Section = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = DremoColors.AccentSoft)
    val Body = TextStyle(fontSize = 22.sp, color = DremoColors.Text)
    val BodyBold = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DremoColors.Text)
    val Label = TextStyle(fontSize = 18.sp, color = DremoColors.Muted)
    val Mono = TextStyle(fontSize = 18.sp, fontFamily = FontFamily.Monospace, color = DremoColors.Accent)
}

/** TV safe area: 5% of a 960 x 540 dp screen on each side. */
val SafeHorizontal = 48.dp
val SafeVertical = 27.dp

/** The burn-in shift for this minute, applied to clocks and overlays. See [com.fabriquefutur.dremo.core.PixelShift]. */
val LocalPixelShift = compositionLocalOf { DpOffset.Zero }

@Composable
fun DremoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DremoColors.Accent,
            onPrimary = DremoColors.Background,
            background = DremoColors.Background,
            onBackground = DremoColors.Text,
            surface = DremoColors.Card,
            onSurface = DremoColors.Text,
            surfaceVariant = DremoColors.CardHigh,
            onSurfaceVariant = DremoColors.Muted,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(DremoColors.Background)) { content() }
    }
}
