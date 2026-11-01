package com.fabriquefutur.dremo.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.fabriquefutur.dremo.clock.WordClock
import com.fabriquefutur.dremo.core.ClockText
import com.fabriquefutur.dremo.core.SceneId
import com.fabriquefutur.dremo.core.TimeParts
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The weather as two lines of text, already in the viewer's units and language. */
data class WeatherText(val line: String, val hours: String?, val shortTemp: String)

/** Everything a clock face needs. The caller builds it once a minute. */
data class ClockInput(
    val time: LocalDateTime,
    val use24h: Boolean,
    val locale: Locale,
    val dateText: String,
    val weather: WeatherText?,
)

/**
 * Picks the composable for a clock face. The word clock falls back to the digital face where the language has none.
 * [preview] is for the small cards on the home screen: the date line is left out, so only the time itself competes for the space.
 */
@Composable
fun ClockFace(
    id: SceneId,
    input: ClockInput,
    modifier: Modifier = Modifier,
    tint: Color = DremoColors.ClockText,
    preview: Boolean = false,
) {
    when {
        id == SceneId.CLOCK_FLIP -> FlipClock(input, modifier, tint, preview)
        id == SceneId.CLOCK_ANALOG -> AnalogClock(input, modifier, tint, preview)
        id == SceneId.CLOCK_WORDS && WordClock.supports(input.locale.language) -> WordClockFace(input, modifier, tint, preview)
        else -> DigitalClock(input, modifier, tint, preview)
    }
}

@Composable
private fun dpToSp(value: Float): TextUnit = with(LocalDensity.current) { value.dp.toSp() }

/** Scales the alpha instead of replacing it, so a dimmed tint stays dim. */
private fun Color.faded(factor: Float): Color = copy(alpha = alpha * factor)

/** The date, then the weather, under the time. Sizes follow [base], the size of the time text in dp. */
@Composable
private fun Caption(input: ClockInput, base: Float, tint: Color, withHours: Boolean = true, showDate: Boolean = true) {
    if (showDate) {
        Text(
            input.dateText,
            style = TextStyle(fontSize = dpToSp(base * 0.13f), color = tint.faded(0.8f), fontWeight = FontWeight.Light),
            textAlign = TextAlign.Center,
        )
    }
    val weather = input.weather ?: return
    Text(
        weather.line,
        style = TextStyle(fontSize = dpToSp(base * 0.095f), color = tint.faded(0.7f)),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = (base * 0.03f).dp),
    )
    if (withHours && weather.hours != null) {
        Text(
            weather.hours,
            style = TextStyle(fontSize = dpToSp(base * 0.075f), color = tint.faded(0.55f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = (base * 0.015f).dp),
        )
    }
}

@Composable
fun DigitalClock(input: ClockInput, modifier: Modifier = Modifier, tint: Color = DremoColors.ClockText, preview: Boolean = false) {
    val parts = ClockText.parts(input.time.hour, input.time.minute, input.use24h)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val base = min(maxWidth.value * 0.25f, maxHeight.value * 0.42f)
        val shift = LocalPixelShift.current
        Column(
            Modifier.align(Alignment.Center).offset(shift.x, shift.y),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${parts.hourText}:${parts.minuteText}",
                    style = TextStyle(
                        fontSize = dpToSp(base),
                        fontWeight = FontWeight.ExtraLight,
                        color = tint,
                        fontFeatureSettings = "tnum",
                    ),
                )
                if (parts.suffix.isNotEmpty()) {
                    Text(
                        parts.suffix,
                        style = TextStyle(fontSize = dpToSp(base * 0.2f), color = tint.faded(0.7f)),
                        modifier = Modifier.padding(start = (base * 0.04f).dp, top = (base * 0.16f).dp),
                    )
                }
            }
            Caption(input, base, tint, showDate = !preview)
        }
    }
}

@Composable
fun WordClockFace(input: ClockInput, modifier: Modifier = Modifier, tint: Color = DremoColors.ClockText, preview: Boolean = false) {
    val lines = WordClock.lines(input.locale.language, input.time.hour, input.time.minute).orEmpty()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val longest = lines.maxOfOrNull { it.length } ?: 8
        // Capital letters in a light face are about 0.66 em wide.
        val size = min(maxHeight.value * 0.15f, maxWidth.value * 0.82f / (0.66f * longest))
        val shift = LocalPixelShift.current
        Column(
            Modifier.align(Alignment.Center).offset(shift.x, shift.y),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            lines.forEachIndexed { i, line ->
                Text(
                    line,
                    style = TextStyle(
                        fontSize = dpToSp(size),
                        fontWeight = if (i == 0) FontWeight.ExtraLight else FontWeight.Normal,
                        color = if (i == 0) tint.faded(0.55f) else tint,
                        letterSpacing = dpToSp(size * 0.06f),
                    ),
                )
            }
            Box(Modifier.height((size * 0.35f).dp))
            if (!preview) {
                Text(
                    input.dateText,
                    style = TextStyle(fontSize = dpToSp(size * 0.36f), color = tint.faded(0.7f), fontWeight = FontWeight.Light),
                )
            }
            input.weather?.let {
                Text(
                    it.line,
                    style = TextStyle(fontSize = dpToSp(size * 0.3f), color = tint.faded(0.6f)),
                    modifier = Modifier.padding(top = (size * 0.08f).dp),
                )
            }
        }
    }
}

@Composable
fun AnalogClock(input: ClockInput, modifier: Modifier = Modifier, tint: Color = DremoColors.ClockText, preview: Boolean = false) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val dial = min(maxHeight.value * 0.68f, maxWidth.value * 0.5f)
        val shift = LocalPixelShift.current
        Column(
            Modifier.align(Alignment.Center).offset(shift.x, shift.y),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val hour = input.time.hour
            val minute = input.time.minute
            Canvas(Modifier.size(dial.dp)) {
                val c = center
                val r = size.minDimension / 2f
                val stroke = r * 0.012f
                drawCircle(tint.faded(0.25f), r, c, style = Stroke(stroke))
                for (i in 0 until 12) {
                    val a = i * PI.toFloat() / 6f
                    val major = i % 3 == 0
                    val len = if (major) r * 0.12f else r * 0.06f
                    drawLine(
                        tint.faded(if (major) 0.9f else 0.45f),
                        Offset(c.x + sin(a) * (r * 0.92f - len), c.y - cos(a) * (r * 0.92f - len)),
                        Offset(c.x + sin(a) * r * 0.92f, c.y - cos(a) * r * 0.92f),
                        strokeWidth = if (major) r * 0.025f else r * 0.012f,
                        cap = StrokeCap.Round,
                    )
                }
                val minuteAngle = (minute * 6f) * PI.toFloat() / 180f
                val hourAngle = ((hour % 12) * 30f + minute * 0.5f) * PI.toFloat() / 180f
                drawLine(
                    tint, c,
                    Offset(c.x + sin(hourAngle) * r * 0.5f, c.y - cos(hourAngle) * r * 0.5f),
                    strokeWidth = r * 0.04f, cap = StrokeCap.Round,
                )
                drawLine(
                    tint, c,
                    Offset(c.x + sin(minuteAngle) * r * 0.76f, c.y - cos(minuteAngle) * r * 0.76f),
                    strokeWidth = r * 0.028f, cap = StrokeCap.Round,
                )
                drawCircle(Color(0xFFFDE68A), r * 0.035f, c)
            }
            Box(Modifier.height((dial * 0.04f).dp))
            Caption(input, dial * 0.4f, tint, showDate = !preview)
        }
    }
}

// --- Flip clock -------------------------------------------------------------

@Composable
fun FlipClock(input: ClockInput, modifier: Modifier = Modifier, tint: Color = DremoColors.ClockText, preview: Boolean = false) {
    val parts = ClockText.parts(input.time.hour, input.time.minute, input.use24h)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val cardH = min(maxHeight.value * 0.46f, maxWidth.value * 0.27f)
        val cardW = cardH * 0.72f
        val gap = cardH * 0.06f
        val shift = LocalPixelShift.current
        Column(
            Modifier.align(Alignment.Center).offset(shift.x, shift.y),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                parts.hourText.forEach { FlipDigit(it, cardW.dp, cardH.dp); Box(Modifier.width(gap.dp)) }
                Box(Modifier.width((gap * 2f).dp))
                parts.minuteText.forEachIndexed { i, c ->
                    FlipDigit(c, cardW.dp, cardH.dp)
                    if (i < parts.minuteText.lastIndex) Box(Modifier.width(gap.dp))
                }
                if (parts.suffix.isNotEmpty()) {
                    Text(
                        parts.suffix,
                        style = TextStyle(fontSize = dpToSp(cardH * 0.16f), color = tint.faded(0.7f)),
                        modifier = Modifier.padding(start = (gap * 2f).dp),
                    )
                }
            }
            Box(Modifier.height((cardH * 0.08f).dp))
            Caption(input, cardH * 0.9f, tint, withHours = false, showDate = !preview)
        }
    }
}

/**
 * One digit on a split-flap card. When [digit] changes, the top half of the old
 * digit folds down, then the bottom half of the new one lands. Both halves are
 * the same card drawn twice and clipped to a half each.
 */
@Composable
private fun FlipDigit(digit: Char, width: Dp, height: Dp) {
    var shown by remember { mutableStateOf(digit) }
    var previous by remember { mutableStateOf(digit) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(digit) {
        if (digit != shown) {
            previous = shown
            shown = digit
            progress.snapTo(0f)
            progress.animateTo(1f, tween(620))
        }
    }
    val p = progress.value
    val fontSize = dpToSp(height.value * 0.78f)
    Box(Modifier.size(width, height)) {
        // Static halves: the new top, and the old bottom until the new one lands.
        Half(shown, top = true, width, height, fontSize, Modifier)
        Half(if (p < 0.5f) previous else shown, top = false, width, height, fontSize, Modifier)
        if (p < 0.5f) {
            // The old top folds down: 0 to 90 degrees about the middle line.
            Half(
                previous, top = true, width, height, fontSize,
                Modifier.graphicsLayer {
                    rotationX = -180f * p
                    cameraDistance = 14f * density
                },
            )
        } else if (p < 1f) {
            // The new bottom drops into place: 90 to 0 degrees.
            Half(
                shown, top = false, width, height, fontSize,
                Modifier.graphicsLayer {
                    rotationX = 180f * (1f - p)
                    cameraDistance = 14f * density
                },
            )
        }
        // The hinge.
        Box(
            Modifier
                .align(Alignment.Center)
                .width(width)
                .height((height.value * 0.012f).coerceAtLeast(1f).dp)
                .background(Color.Black.copy(alpha = 0.85f)),
        )
    }
}

@Composable
private fun Half(char: Char, top: Boolean, width: Dp, height: Dp, fontSize: TextUnit, modifier: Modifier) {
    Box(
        modifier
            .size(width, height)
            .drawWithContent {
                val content = this
                clipRect(top = if (top) 0f else size.height / 2f, bottom = if (top) size.height / 2f else size.height) {
                    content.drawContent()
                }
            }
            .clip(RoundedCornerShape((height.value * 0.08f).dp))
            .background(Color(0xFF181B26)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            char.toString(),
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = DremoColors.ClockText,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

// --- Corner clock for ambience scenes --------------------------------------

/** A small clock in the top-right corner of an ambience scene. */
@Composable
fun CornerClock(input: ClockInput, modifier: Modifier = Modifier) {
    val parts: TimeParts = ClockText.parts(input.time.hour, input.time.minute, input.use24h)
    val shift = LocalPixelShift.current
    val shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.7f), blurRadius = 10f)
    Box(modifier.fillMaxSize().padding(horizontal = SafeHorizontal, vertical = SafeVertical)) {
        Column(
            Modifier.align(Alignment.TopEnd).offset(shift.x, shift.y),
            horizontalAlignment = Alignment.End,
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${parts.hourText}:${parts.minuteText}",
                    style = TextStyle(
                        fontSize = 56.sp, fontWeight = FontWeight.Light, color = DremoColors.ClockText.copy(alpha = 0.85f),
                        fontFeatureSettings = "tnum", shadow = shadow,
                    ),
                )
                if (parts.suffix.isNotEmpty()) {
                    Text(
                        parts.suffix,
                        style = TextStyle(fontSize = 22.sp, color = DremoColors.ClockText.copy(alpha = 0.7f), shadow = shadow),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            val short = input.weather?.shortTemp
            Text(
                if (short != null) "${input.dateText}  ·  $short" else input.dateText,
                style = TextStyle(fontSize = 20.sp, color = DremoColors.ClockText.copy(alpha = 0.7f), shadow = shadow),
            )
        }
    }
}
