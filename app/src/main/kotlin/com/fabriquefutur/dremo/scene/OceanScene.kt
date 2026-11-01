package com.fabriquefutur.dremo.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/**
 * Waves at dusk: a violet-to-amber sky with the sun on the horizon, six layers of
 * sea from a far, glassy band to a dark foreground, and sparkles on the water
 * under the sun.
 */
class OceanScene(detail: Float = 1f, seed: Int = 5) : BaseScene(detail, seed) {
    private val path = Path()
    private var sky: Brush = Brush.verticalGradient(listOf(Color.Black, Color.Black))
    private var horizon = 0f
    private val sunGlow = Sprites.soft(Color(0xFFFF9A4A), core = 0f, size = 128)
    private val sunDisc = Sprites.soft(Color(0xFFFFD98A), core = 0.7f, size = 96)

    private val glitterCount = count(46, 10)
    private val gx = FloatArray(glitterCount)
    private val gy = FloatArray(glitterCount)
    private val gw = FloatArray(glitterCount)
    private val gp = FloatArray(glitterCount)
    private val gs = FloatArray(glitterCount)

    private val layerColors = Array(LAYERS) { mix(NEAR, FAR, it / (LAYERS - 1f)) }

    override fun onResize() {
        horizon = h * 0.44f
        sky = Brush.verticalGradient(
            0f to Color(0xFF150F33),
            0.5f to Color(0xFF4B2670),
            0.8f to Color(0xFFD8584A),
            1f to Color(0xFFF7B267),
            startY = 0f,
            endY = horizon,
        )
        for (i in 0 until glitterCount) {
            val f = random.nextFloat()
            gy[i] = horizon + (h - horizon) * 0.55f * f * f + 6f * unit
            // The path of light widens towards the viewer.
            gx[i] = w * 0.5f + rand(-1f, 1f) * (30f + 260f * f) * unit
            gw[i] = rand(10f, 36f) * unit * (0.6f + f)
            gp[i] = rand(0f, 6.28f)
            gs[i] = rand(0.8f, 2.4f)
        }
    }

    override fun step(dt: Float) = Unit

    override fun render(s: DrawScope) {
        s.drawRect(sky, size = Size(w, horizon + 2f))
        s.sprite(sunGlow, w * 0.5f, horizon, w * 0.8f, h * 0.55f, alpha = 0.75f)
        s.sprite(sunDisc, w * 0.5f, horizon - h * 0.005f, h * 0.17f, alpha = 0.95f)

        // The far sea is a flat dark-violet base; the waves are drawn over it.
        s.drawRect(layerColors[LAYERS - 1], Offset(0f, horizon), Size(w, h - horizon))

        for (layer in LAYERS - 1 downTo 0) {
            val f = layer / (LAYERS - 1f) // 1 = far, 0 = near
            val baseY = horizon + (h - horizon) * (1f - f) * (1f - f) * 0.92f + h * 0.012f * (LAYERS - layer)
            val amp = (7f + 26f * (1f - f)) * unit
            val k1 = 0.0042f / unit * (1f + 0.5f * f)
            val k2 = 0.0095f / unit
            val sp1 = 0.55f + 0.2f * layer
            val sp2 = 0.4f + 0.15f * layer
            path.rewind()
            path.moveTo(0f, h)
            var x = 0f
            val stepX = (w / 80f).coerceAtLeast(8f)
            while (x <= w + stepX) {
                val y = baseY + amp * sin(x * k1 + t * sp1 + layer * 1.9f) + amp * 0.45f * sin(x * k2 - t * sp2 + layer)
                path.lineTo(x, y)
                x += stepX
            }
            path.lineTo(w, h)
            path.close()
            s.drawPath(path, layerColors[layer])
        }

        // Sparkles under the sun, over the far and middle waves.
        for (i in 0 until glitterCount) {
            val a = (sin(t * gs[i] + gp[i]) * 0.5f + 0.5f)
            s.drawRect(
                Color(0xFFFFE2A8),
                Offset(gx[i] - gw[i] / 2f, gy[i]),
                Size(gw[i], 2.5f * unit),
                alpha = a * a * 0.8f,
            )
        }
    }

    private companion object {
        const val LAYERS = 6
        val FAR = Color(0xFF3A2A66)
        val NEAR = Color(0xFF070D1E)
    }
}
