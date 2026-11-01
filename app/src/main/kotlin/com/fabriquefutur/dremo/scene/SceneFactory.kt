package com.fabriquefutur.dremo.scene

import com.fabriquefutur.dremo.core.SceneId

/** The renderer for an ambience scene. Clock faces are composables, not renderers. */
fun createRenderer(id: SceneId, detail: Float = 1f): SceneRenderer? = when (id) {
    SceneId.RAIN -> RainScene(detail)
    SceneId.FIREPLACE -> FireScene(detail)
    SceneId.AURORA -> AuroraScene(detail)
    SceneId.STARS -> StarsScene(detail)
    SceneId.OCEAN -> OceanScene(detail)
    SceneId.LAVA -> LavaScene(detail)
    else -> null
}
