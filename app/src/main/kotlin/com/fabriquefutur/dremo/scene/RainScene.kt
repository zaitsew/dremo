package com.fabriquefutur.dremo.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/**
 * Rain on a window at night: blurred city lights behind the glass, a few
 * hundred still droplets, and larger drops that slide down in stops and starts,
 * leaving a faint trail.
 */
class RainScene(detail: Float = 1f, seed: Int = 1) : BaseScene(detail, seed) {
    private val bokehCount = count(14)
    private val bx = FloatArray(bokehCount)
    private val by = FloatArray(bokehCount)
    private val br = FloatArray(bokehCount)
    private val bphase = FloatArray(bokehCount)
    private val bokehSprites = Array(BOKEH.size) { Sprites.soft(BOKEH[it], core = 0.55f, size = 64) }

    private val stillCount = count(110)
    private val sx = FloatArray(stillCount)
    private val sy = FloatArray(stillCount)
    private val sr = FloatArray(stillCount)
    private val sa = FloatArray(stillCount)

    private val dropCount = count(36)
    private val dx = FloatArray(dropCount)
    private val dy = FloatArray(dropCount)
    private val dr = FloatArray(dropCount)
    private val dspeed = FloatArray(dropCount)
    private val dpause = FloatArray(dropCount)
    private val dtrail = FloatArray(dropCount)

    private var background: Brush = Brush.verticalGradient(listOf(Color.Black, Color.Black))

    override fun onResize() {
        background = Brush.verticalGradient(
            0f to Color(0xFF0A111E),
            0.6f to Color(0xFF141F33),
            1f to Color(0xFF1E2C45),
            startY = 0f,
            endY = h,
        )
        for (i in 0 until bokehCount) {
            bx[i] = rand(0.05f, 0.95f) * w
            by[i] = rand(0.35f, 0.95f) * h
            br[i] = rand(34f, 90f) * unit
            bphase[i] = rand(0f, 6.28f)
        }
        for (i in 0 until stillCount) {
            sx[i] = rand(0f, w)
            sy[i] = rand(0f, h)
            sr[i] = rand(2f, 7f) * unit
            sa[i] = rand(0.25f, 0.7f)
        }
        for (i in 0 until dropCount) respawn(i, anywhere = true)
    }

    private fun respawn(i: Int, anywhere: Boolean) {
        dx[i] = rand(0f, w)
        dy[i] = if (anywhere) rand(0f, h) else -rand(10f, 200f) * unit
        dr[i] = rand(5f, 12f) * unit
        dspeed[i] = rand(60f, 190f) * unit
        dpause[i] = rand(0f, 1.5f)
        dtrail[i] = 0f
    }

    override fun step(dt: Float) {
        for (i in 0 until dropCount) {
            if (dpause[i] > 0f) {
                dpause[i] -= dt
                dtrail[i] = (dtrail[i] - 60f * unit * dt).coerceAtLeast(0f)
                continue
            }
            val move = dspeed[i] * dt
            dy[i] += move
            dtrail[i] = (dtrail[i] + move * 0.6f).coerceAtMost(dr[i] * 14f)
            // Drops catch on the glass: a stop of up to a second and a half, at random.
            if (random.nextFloat() < dt * 0.9f) dpause[i] = rand(0.2f, 1.5f)
            if (dy[i] > h + dr[i]) respawn(i, anywhere = false)
        }
    }

    override fun render(s: DrawScope) {
        s.drawRect(background, size = Size(w, h))
        // City lights out of focus; their brightness drifts slowly.
        for (i in 0 until bokehCount) {
            val pulse = 0.75f + 0.25f * sin(t * 0.25f + bphase[i])
            s.sprite(bokehSprites[i % bokehSprites.size], bx[i], by[i], br[i] * 2.4f, alpha = 0.55f * pulse, blend = BlendMode.Plus)
        }
        // Still droplets.
        for (i in 0 until stillCount) {
            s.drawCircle(Color(0xFFCFE0F7), sr[i], Offset(sx[i], sy[i]), alpha = sa[i] * 0.22f)
            s.drawCircle(Color.White, sr[i] * 0.3f, Offset(sx[i] - sr[i] * 0.3f, sy[i] - sr[i] * 0.3f), alpha = sa[i] * 0.6f)
        }
        // Sliding drops with trails.
        for (i in 0 until dropCount) {
            val r = dr[i]
            if (dtrail[i] > 1f) {
                s.drawLine(
                    Color(0xFFCFE0F7),
                    Offset(dx[i], dy[i] - dtrail[i]),
                    Offset(dx[i], dy[i]),
                    strokeWidth = r * 0.7f,
                    alpha = 0.10f,
                )
            }
            s.drawCircle(Color(0xFFCFE0F7), r, Offset(dx[i], dy[i]), alpha = 0.28f)
            s.drawCircle(Color.White, r * 0.35f, Offset(dx[i] - r * 0.3f, dy[i] - r * 0.35f), alpha = 0.85f)
        }
    }

    private companion object {
        val BOKEH = listOf(
            Color(0xFFFFC46B), Color(0xFF6BA8FF), Color(0xFFFF8F6B), Color(0xFFFFE08A),
            Color(0xFF8FB7FF), Color(0xFFFF6B8A),
        )
    }
}
