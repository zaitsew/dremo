package com.fabriquefutur.dremo.scene

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

/**
 * A fireplace: a glowing hearth, two logs, additive flame particles that cool
 * from white-yellow through orange to deep red as they rise, and a few embers.
 */
class FireScene(detail: Float = 1f, seed: Int = 2) : BaseScene(detail, seed) {
    private val flameCount = count(100, 20)
    private val fx = FloatArray(flameCount)
    private val fy = FloatArray(flameCount)
    private val fvx = FloatArray(flameCount)
    private val fvy = FloatArray(flameCount)
    private val fage = FloatArray(flameCount)
    private val flife = FloatArray(flameCount)
    private val fsize = FloatArray(flameCount)
    private val fseed = FloatArray(flameCount)

    private val emberCount = count(26, 6)
    private val ex = FloatArray(emberCount)
    private val ey = FloatArray(emberCount)
    private val evx = FloatArray(emberCount)
    private val evy = FloatArray(emberCount)
    private val eage = FloatArray(emberCount)
    private val elife = FloatArray(emberCount)
    private val eseed = FloatArray(emberCount)

    // Hot to cool.
    private val flameSprites = Array(FLAME_COLORS.size) { Sprites.soft(FLAME_COLORS[it], core = 0.1f, size = 64) }
    private val glowSprite = Sprites.soft(Color(0xFFFF7A1A), core = 0f, size = 128)

    private var background: Brush = Brush.verticalGradient(listOf(Color.Black, Color.Black))
    private var baseX = 0f
    private var baseY = 0f
    private var spread = 0f

    override fun onResize() {
        background = Brush.verticalGradient(
            0f to Color(0xFF060303),
            0.7f to Color(0xFF110806),
            1f to Color(0xFF1C0D08),
            startY = 0f,
            endY = h,
        )
        baseX = w * 0.5f
        baseY = h * 0.80f
        spread = w * 0.075f
        for (i in 0 until flameCount) {
            spawnFlame(i)
            fage[i] = random.nextFloat() * flife[i] // spread the first generation over the whole column
        }
        for (i in 0 until emberCount) {
            spawnEmber(i)
            eage[i] = random.nextFloat() * elife[i]
        }
    }

    private fun spawnFlame(i: Int) {
        // Two draws averaged: more flames in the middle of the logs than at the ends.
        fx[i] = baseX + (random.nextFloat() + random.nextFloat() - 1f) * spread
        fy[i] = baseY + rand(-10f, 10f) * unit
        fvx[i] = rand(-14f, 14f) * unit
        fvy[i] = -rand(150f, 290f) * unit
        flife[i] = rand(0.7f, 1.5f)
        fage[i] = 0f
        // Flames in the middle are larger.
        val centre = 1f - kotlin.math.abs(fx[i] - baseX) / spread
        fsize[i] = rand(70f, 120f) * unit * (0.6f + 0.5f * centre.coerceIn(0f, 1f))
        fseed[i] = rand(0f, 6.28f)
    }

    private fun spawnEmber(i: Int) {
        ex[i] = baseX + rand(-1f, 1f) * spread * 0.9f
        ey[i] = baseY - rand(0f, 30f) * unit
        evx[i] = rand(-30f, 30f) * unit
        evy[i] = -rand(70f, 180f) * unit
        eage[i] = 0f
        elife[i] = rand(2f, 4.5f)
        eseed[i] = rand(0f, 6.28f)
    }

    override fun step(dt: Float) {
        for (i in 0 until flameCount) {
            fage[i] += dt
            if (fage[i] >= flife[i]) {
                spawnFlame(i)
                continue
            }
            val a = fage[i] / flife[i]
            fx[i] += (fvx[i] + sin(t * 4f + fseed[i]) * 26f * unit * a) * dt
            fy[i] += fvy[i] * dt
        }
        for (i in 0 until emberCount) {
            eage[i] += dt
            if (eage[i] >= elife[i]) {
                spawnEmber(i)
                continue
            }
            ex[i] += (evx[i] + sin(t * 2.2f + eseed[i]) * 40f * unit) * dt
            ey[i] += evy[i] * dt
        }
    }

    override fun render(s: DrawScope) {
        s.drawRect(background, size = Size(w, h))
        val flicker = 0.85f + 0.15f * sin(t * 7f) * sin(t * 2.3f + 1f)

        // Light thrown on the room.
        s.sprite(glowSprite, baseX, baseY - h * 0.12f, w * 0.95f, h * 1.25f, alpha = 0.38f * flicker)

        // Logs: two dark rounded bars, crossed a little, with a hot underside.
        val logW = w * 0.20f
        val logH = h * 0.052f
        s.rotate(-7f, Offset(baseX, baseY + logH * 0.5f)) {
            drawRoundRect(
                Color(0xFF2A160C), Offset(baseX - logW * 0.55f, baseY), Size(logW * 1.1f, logH),
                CornerRadius(logH / 2f, logH / 2f),
            )
        }
        s.rotate(7f, Offset(baseX, baseY + logH * 0.5f)) {
            drawRoundRect(
                Color(0xFF331B0F), Offset(baseX - logW * 0.55f, baseY + logH * 0.25f), Size(logW * 1.1f, logH),
                CornerRadius(logH / 2f, logH / 2f),
            )
        }
        // Bed of coals under the flames.
        s.sprite(glowSprite, baseX, baseY + logH * 0.5f, spread * 3.4f, logH * 3.2f, alpha = 0.8f * flicker)

        // Flames, additive. Colour index and brightness come from the age.
        for (i in 0 until flameCount) {
            val a = fage[i] / flife[i]
            val idx = (a * (flameSprites.size - 0.01f)).toInt().coerceIn(0, flameSprites.size - 1)
            val size = fsize[i] * (1f - 0.55f * a)
            val alpha = (1f - a) * 0.55f
            s.sprite(flameSprites[idx], fx[i], fy[i], size * 0.8f, size * 1.3f, alpha = alpha)
        }

        // Embers: small, bright, fading.
        for (i in 0 until emberCount) {
            val a = eage[i] / elife[i]
            val twinkle = 0.6f + 0.4f * sin(t * 9f + eseed[i])
            s.drawCircle(
                Color(0xFFFFB04A), (2.2f + 1.6f * (1f - a)) * unit, Offset(ex[i], ey[i]),
                alpha = (1f - a) * twinkle,
                blendMode = BlendMode.Plus,
            )
        }
    }

    private companion object {
        val FLAME_COLORS = listOf(
            Color(0xFFFFF1B8), Color(0xFFFFC857), Color(0xFFFF8C1A),
            Color(0xFFE8541A), Color(0xFFB0301A), Color(0xFF6A1A12),
        )
    }
}
