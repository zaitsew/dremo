package com.fabriquefutur.dremo.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/**
 * Northern lights over a snowy ridge. Each curtain is a row of soft rays that
 * overlap by half their width, so the row reads as one band of light with no
 * seams. A ray fades to nothing at its top and its bottom, its height and
 * brightness follow slow waves, and the whole curtain sways sideways. Rays are
 * drawn additively from one pre-rendered sprite per curtain colour.
 */
class AuroraScene(detail: Float = 1f, seed: Int = 3) : BaseScene(detail, seed) {
    private val rays = count(96, 24)
    private val starCount = count(110, 20)
    private val stx = FloatArray(starCount)
    private val sty = FloatArray(starCount)
    private val sts = FloatArray(starCount)
    private val stp = FloatArray(starCount)

    private val raySprites = Array(CURTAINS) { Sprites.curtainRay(COLORS[it]) }

    private var sky: Brush = Brush.verticalGradient(listOf(Color.Black, Color.Black))
    private val ridge = Path()
    private val trees = Path()

    override fun onResize() {
        sky = Brush.verticalGradient(
            0f to Color(0xFF02040C),
            0.65f to Color(0xFF081428),
            1f to Color(0xFF0F2433),
            startY = 0f,
            endY = h,
        )
        for (i in 0 until starCount) {
            stx[i] = rand(0f, w)
            sty[i] = rand(0f, h * 0.7f)
            sts[i] = rand(1f, 2.6f) * unit
            stp[i] = rand(0f, 6.28f)
        }
        buildRidge()
    }

    private fun baseY(k: Int): Float = h * (0.54f + 0.05f * k)

    private fun buildRidge() {
        ridge.rewind()
        ridge.moveTo(0f, h)
        var x = 0f
        val step = w / 48f
        while (x <= w + step) {
            val y = h * 0.86f - h * 0.045f * sin(x / w * 5.1f + 0.7f) - h * 0.025f * sin(x / w * 13f)
            ridge.lineTo(x, y)
            x += step
        }
        ridge.lineTo(w, h)
        ridge.close()

        // A few pines on the ridge, as plain triangles.
        trees.rewind()
        val treeCount = count(14, 5)
        for (i in 0 until treeCount) {
            val tx = w * (0.04f + 0.92f * i / treeCount) + rand(-20f, 20f) * unit
            val ty = h * 0.86f - h * 0.045f * sin(tx / w * 5.1f + 0.7f) - h * 0.025f * sin(tx / w * 13f)
            val th = rand(60f, 120f) * unit
            trees.moveTo(tx, ty - th)
            trees.lineTo(tx - th * 0.22f, ty + 4f * unit)
            trees.lineTo(tx + th * 0.22f, ty + 4f * unit)
            trees.close()
        }
    }

    override fun step(dt: Float) = Unit

    override fun render(s: DrawScope) {
        s.drawRect(sky, size = Size(w, h))

        for (i in 0 until starCount) {
            val tw = 0.55f + 0.45f * sin(t * 1.3f + stp[i])
            s.drawCircle(Color.White, sts[i], Offset(stx[i], sty[i]), alpha = 0.7f * tw)
        }

        val spacing = w / rays
        // Back curtain first.
        for (k in CURTAINS - 1 downTo 0) {
            val sprite = raySprites[k]
            val speed = 1f + 0.25f * k
            val base = baseY(k)
            val phase = k * 1.7f
            // A slow sideways drift of the whole curtain, up to about 3% of the width.
            val sway = w * 0.03f * sin(t * 0.045f * speed + phase)
            val dim = if (k == CURTAINS - 1) 0.75f else 1f
            // One ray more than needed on each side, so the sway never uncovers an edge.
            for (i in -1..rays) {
                val x = i * spacing
                val bottom = base + h * 0.03f * sin(x * 0.0035f / unit * 0.6f + t * 0.16f * speed + phase)
                val length = h * (0.30f +
                    0.14f * (0.5f + 0.5f * sin(x * 0.0021f / unit + t * 0.12f * speed + phase)) +
                    0.05f * sin(x * 0.0063f / unit - t * 0.25f * speed + phase))
                val shimmer = 0.5f + 0.5f * sin(x * 0.0045f / unit + t * 0.35f * speed + phase * 2f)
                s.sprite(
                    sprite,
                    cx = x + sway,
                    cy = bottom - length / 2f,
                    width = spacing * 2f,
                    height = length,
                    alpha = (0.28f + 0.30f * shimmer) * dim,
                )
            }
        }

        s.drawPath(ridge, Color(0xFF04070C))
        s.drawPath(trees, Color(0xFF03060A))
    }

    private companion object {
        const val CURTAINS = 3
        val COLORS = listOf(Color(0xFF3DFFA0), Color(0xFF35E0D0), Color(0xFFB06BFF))
    }
}
