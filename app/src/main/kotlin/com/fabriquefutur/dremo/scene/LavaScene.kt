package com.fabriquefutur.dremo.scene

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/**
 * A lava lamp: soft wax blobs that rise and sink on slow sine paths, stretch a
 * little, and glow where they overlap.
 */
class LavaScene(detail: Float = 1f, seed: Int = 6) : BaseScene(detail, seed) {
    private val blobs = count(9, 4)
    private val bx = FloatArray(blobs)
    private val bRadius = FloatArray(blobs)
    private val bw1 = FloatArray(blobs)
    private val bp1 = FloatArray(blobs)
    private val bp2 = FloatArray(blobs)
    private val bp3 = FloatArray(blobs)

    private val sprites = Array(WAX.size) { Sprites.soft(WAX[it], core = 0.5f, size = 96) }
    private val heat = Sprites.soft(Color(0xFFFF5A3C), core = 0f, size = 96)

    private var background: Brush = Brush.verticalGradient(listOf(Color.Black, Color.Black))

    override fun onResize() {
        background = Brush.verticalGradient(
            0f to Color(0xFF140A30),
            0.55f to Color(0xFF330F45),
            1f to Color(0xFF7A1D3F),
            startY = 0f,
            endY = h,
        )
        for (i in 0 until blobs) {
            bx[i] = w * (0.14f + 0.72f * (i + rand(0.1f, 0.9f)) / blobs)
            bRadius[i] = rand(0.09f, 0.17f) * h
            bw1[i] = rand(0.09f, 0.17f) // rad/s: one full cycle every 37 to 70 s
            bp1[i] = rand(0f, 6.28f)
            bp2[i] = rand(0f, 6.28f)
            bp3[i] = rand(0f, 6.28f)
        }
    }

    override fun step(dt: Float) = Unit

    override fun render(s: DrawScope) {
        s.drawRect(background, size = Size(w, h))
        s.sprite(heat, w * 0.5f, h * 1.02f, w * 1.1f, h * 0.7f, alpha = 0.55f)
        for (i in 0 until blobs) {
            val y = h * (0.5f + 0.5f * sin(t * bw1[i] + bp1[i]))
            val x = bx[i] + w * 0.035f * sin(t * 0.07f + bp2[i])
            val pulse = 1f + 0.09f * sin(t * 0.31f + bp3[i])
            // Blobs stretch while they move fast, which is at mid-height.
            val speed = kotlin.math.abs(kotlin.math.cos(t * bw1[i] + bp1[i]))
            val r = bRadius[i] * pulse
            s.sprite(
                sprites[i % sprites.size], x, y,
                width = r * 2.6f * (1f - 0.08f * speed),
                height = r * 2.6f * (1f + 0.22f * speed),
                alpha = 0.78f,
                blend = BlendMode.Plus,
            )
        }
    }

    private companion object {
        val WAX = listOf(Color(0xFFFF8A3D), Color(0xFFFF4F7B), Color(0xFFFFB347))
    }
}
