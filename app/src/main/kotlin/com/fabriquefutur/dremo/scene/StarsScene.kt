package com.fabriquefutur.dremo.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A clear night sky turning slowly around a pole, with a faint band of the Milky
 * Way and, now and then, a shooting star. Positions are rotated with one sine and
 * one cosine per frame, not per star.
 */
class StarsScene(detail: Float = 1f, seed: Int = 4) : BaseScene(detail, seed) {
    private val starCount = count(240, 30)
    private val x0 = FloatArray(starCount)
    private val y0 = FloatArray(starCount)
    private val size = FloatArray(starCount)
    private val bright = FloatArray(starCount)
    private val twinkle = FloatArray(starCount)
    private val phase = FloatArray(starCount)
    private val tint = Array(starCount) { STAR_COLORS[it % STAR_COLORS.size] }

    private val dust = Sprites.soft(Color(0xFF8FA8FF), core = 0f, size = 64)
    private val flare = Sprites.soft(Color.White, core = 0.1f, size = 32)

    private var sky: Brush = Brush.verticalGradient(listOf(Color.Black, Color.Black))
    private val ridge = Path()
    private var poleX = 0f
    private var poleY = 0f
    private var angle = 0f

    // One shooting star at a time: position, direction, age.
    private var shootCountdown = 5f
    private var shootAge = -1f
    private var shootX = 0f
    private var shootY = 0f
    private var shootDx = 0f
    private var shootDy = 0f

    override fun onResize() {
        sky = Brush.verticalGradient(
            0f to Color(0xFF01020A),
            0.6f to Color(0xFF070B22),
            1f to Color(0xFF191B3D),
            startY = 0f,
            endY = h,
        )
        poleX = w * 0.62f
        poleY = h * 0.28f
        val reach = hypot(w, h) * 0.75f
        for (i in 0 until starCount) {
            // Uniform over a disc: radius is the square root of a uniform number.
            val r = reach * kotlin.math.sqrt(random.nextFloat())
            val a = rand(0f, 6.2832f)
            x0[i] = r * cos(a)
            y0[i] = r * sin(a)
            val big = random.nextFloat() < 0.08f
            size[i] = (if (big) rand(2.4f, 3.6f) else rand(0.8f, 2.0f)) * unit
            bright[i] = if (big) rand(0.8f, 1f) else rand(0.3f, 0.85f)
            twinkle[i] = rand(0.6f, 2.2f)
            phase[i] = rand(0f, 6.28f)
        }
        ridge.rewind()
        ridge.moveTo(0f, h)
        var x = 0f
        val step = w / 40f
        while (x <= w + step) {
            ridge.lineTo(x, h * 0.9f - h * 0.035f * sin(x / w * 4.3f + 2f) - h * 0.02f * sin(x / w * 11f))
            x += step
        }
        ridge.lineTo(w, h)
        ridge.close()
    }

    override fun step(dt: Float) {
        angle += dt * 0.012f
        if (shootAge >= 0f) {
            shootAge += dt
            shootX += shootDx * dt
            shootY += shootDy * dt
            if (shootAge > 0.9f) shootAge = -1f
        } else {
            shootCountdown -= dt
            if (shootCountdown <= 0f) {
                shootCountdown = rand(9f, 16f)
                shootAge = 0f
                shootX = rand(0.1f, 0.8f) * w
                shootY = rand(0.05f, 0.35f) * h
                shootDx = rand(900f, 1300f) * unit
                shootDy = rand(350f, 600f) * unit
            }
        }
    }

    override fun render(s: DrawScope) {
        s.drawRect(sky, size = Size(w, h))

        // The Milky Way: a diagonal chain of large, very faint blobs.
        for (i in 0 until MILKY_BLOBS) {
            val f = i / (MILKY_BLOBS - 1f)
            s.sprite(
                dust,
                w * (0.15f + 0.7f * f) + sin(t * 0.05f + i) * 12f * unit,
                h * (0.7f - 0.5f * f),
                w * 0.34f, h * 0.30f,
                alpha = 0.11f,
            )
        }

        val c = cos(angle)
        val sn = sin(angle)
        for (i in 0 until starCount) {
            val x = poleX + x0[i] * c - y0[i] * sn
            val y = poleY + x0[i] * sn + y0[i] * c
            if (x < 0f || x > w || y < 0f || y > h * 0.92f) continue
            val a = bright[i] * (0.72f + 0.28f * sin(t * twinkle[i] + phase[i]))
            s.drawCircle(tint[i], size[i], Offset(x, y), alpha = a)
            if (size[i] > 2.3f * unit) s.sprite(flare, x, y, size[i] * 7f, alpha = a * 0.5f)
        }

        if (shootAge >= 0f) {
            val fade = 1f - shootAge / 0.9f
            for (k in 0 until 4) {
                val back = k * 0.035f
                s.drawLine(
                    Color.White,
                    Offset(shootX - shootDx * back, shootY - shootDy * back),
                    Offset(shootX - shootDx * (back + 0.035f), shootY - shootDy * (back + 0.035f)),
                    strokeWidth = (3.2f - k * 0.7f) * unit,
                    alpha = fade * (0.9f - k * 0.2f),
                )
            }
        }

        s.drawPath(ridge, Color(0xFF03040A))
    }

    private companion object {
        const val MILKY_BLOBS = 6
        val STAR_COLORS = listOf(
            Color(0xFFFFFFFF), Color(0xFFDCE6FF), Color(0xFFFFF1D6), Color(0xFFFFFFFF), Color(0xFFBFD2FF),
        )
    }
}
