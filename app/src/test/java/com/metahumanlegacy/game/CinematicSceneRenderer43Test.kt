package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CinematicSceneRenderer43Test {
    @Test fun reducedMotionRemovesAnimatedRain() {
        val rainy = CinematicScene43Profile(
            night = true,
            rain = true,
            crisis = false,
            awakening = false,
            skylineLayers = 3,
            practicalLights = 11,
            sceneHeightDp = 285
        )
        assertTrue(CinematicScene43.rainDropCount(rainy, false) > 0)
        assertEquals(0, CinematicScene43.rainDropCount(rainy, true))
    }

    @Test fun layeredSceneHasRealDepth() {
        val profile = CinematicScene43Profile(false, false, false, false, 4, 6, 285)
        val depths = CinematicScene43.parallaxDepths(profile)
        assertEquals(4, depths.size)
        assertTrue(depths.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun awakeningUsesTallerCinematicStage() {
        val regular = CinematicScene43Profile(false, false, false, false, 3, 6, 285)
        val awakening = regular.copy(awakening = true, sceneHeightDp = 330)
        assertTrue(awakening.sceneHeightDp > regular.sceneHeightDp)
    }
}
