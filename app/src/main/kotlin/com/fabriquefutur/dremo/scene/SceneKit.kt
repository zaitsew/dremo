package com.fabriquefutur.dremo.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.random.Random

/**
 * A procedurally drawn scene. The loop is: [advance] moves the world by a time
 * step, [draw] paints it. Both run on the main thread and allocate nothing per
 * frame: particles live in preallocated arrays, brushes and paths are built
 * when the size changes.
 */
interface SceneRenderer {
    fun advance(dtSeconds: Float)
    fun draw(scope: DrawScope)
}

/**
 * Shared plumbing: tracks the canvas size, calls [onResize] when it changes and
 * keeps a running clock in [t]. [detail] scales particle counts: 1 for a full
 * screen, about 0.4 for a grid preview.
 */
abstract class BaseScene(protected val detail: Float, seed: Int) : SceneRenderer {
    protected val random = Random(seed)
    protected var w = 0f
    protected var h = 0f
    protected var t = 0f

    /** Pixels per "design unit": 1 at 1080 lines, so sizes read the same on every screen. */
    protected var unit = 1f

    protected abstract fun onResize()
    protected abstract fun step(dt: Float)
    protected abstract fun render(s: DrawScope)

    final override fun advance(dtSeconds: Float) {
        if (w <= 0f) return
        val dt = dtSeconds.coerceIn(0f, 0.1f)
        t += dt
        step(dt)
    }

    final override fun draw(scope: DrawScope) {
        val sw = scope.size.width
        val sh = scope.size.height
        if (sw <= 0f || sh <= 0f) return
        if (sw != w || sh != h) {
            w = sw
            h = sh
            unit = sh / 1080f
            onResize()
        }
        render(scope)
    }

    protected fun count(base: Int, min: Int = 3): Int = (base * detail).toInt().coerceAtLeast(min)

    protected fun rand(from: Float, to: Float): Float = from + random.nextFloat() * (to - from)
}

/** Pre-rendered soft sprites, so a glow costs one textured quad instead of a gradient shader per draw. */
object Sprites {
    /**
     * A round sprite in [color]: fully opaque to [core] of the radius, then a
     * smooth fall to transparent at the edge. [core] 0 is a pure glow.
     */
    fun soft(color: Color, core: Float = 0f, size: Int = 96): ImageBitmap {
        val bitmap = ImageBitmap(size, size)
        val canvas = Canvas(bitmap)
        val r = size / 2f
        val paint = Paint().apply {
            shader = RadialGradientShader(
                center = Offset(r, r),
                radius = r,
                colors = listOf(
                    color,
                    color.copy(alpha = color.alpha * 0.55f),
                    color.copy(alpha = color.alpha * 0.18f),
                    color.copy(alpha = 0f),
                ),
                colorStops = listOf(core, core + (1f - core) * 0.4f, core + (1f - core) * 0.75f, 1f),
            )
        }
        canvas.drawCircle(Offset(r, r), r, paint)
        return bitmap
    }

    /**
     * One soft vertical ray in [color], for building light curtains. Alpha rises
     * smoothly from nothing at the top to a peak near the bottom, then falls to
     * nothing at the very bottom; across the width it follows a raised cosine, so
     * rays drawn at half-width spacing add up to an even band with no seams.
     */
    fun curtainRay(color: Color, width: Int = 48, height: Int = 128): ImageBitmap {
        val rgb = color.toArgb() and 0x00FFFFFF
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val v = (y + 0.5f) / height
            val vertical = if (v < PEAK) {
                val s = v / PEAK
                (s * s * (3f - 2f * s)).pow(1.3f)
            } else {
                val f = (v - PEAK) / (1f - PEAK)
                1f - f * f * (3f - 2f * f)
            }
            for (x in 0 until width) {
                val horizontal = 0.5f - 0.5f * cos(2f * PI.toFloat() * (x + 0.5f) / width)
                val a = (vertical * horizontal * 255f + 0.5f).toInt().coerceIn(0, 255)
                pixels[y * width + x] = (a shl 24) or rgb
            }
        }
        return android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888)
            .asImageBitmap()
    }

    private const val PEAK = 0.78f
}

/** Draws [image] centred on ([cx], [cy]) at [width] x [height] pixels. */
fun DrawScope.sprite(
    image: ImageBitmap,
    cx: Float,
    cy: Float,
    width: Float,
    height: Float = width,
    alpha: Float = 1f,
    blend: BlendMode = BlendMode.Plus,
) {
    drawImage(
        image = image,
        dstOffset = IntOffset((cx - width / 2f).toInt(), (cy - height / 2f).toInt()),
        dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
        alpha = alpha.coerceIn(0f, 1f),
        blendMode = blend,
    )
}

fun lerp(a: Float, b: Float, f: Float): Float = a + (b - a) * f

fun mix(a: Color, b: Color, f: Float): Color = Color(
    red = lerp(a.red, b.red, f),
    green = lerp(a.green, b.green, f),
    blue = lerp(a.blue, b.blue, f),
    alpha = lerp(a.alpha, b.alpha, f),
)
