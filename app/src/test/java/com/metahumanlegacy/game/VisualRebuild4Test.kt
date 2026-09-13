package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualRebuild4Test {
    @Test fun reducedMotionDisablesAtmosphericParticles() {
        assertEquals(0, VisualRebuild4.particleCount(true))
        assertTrue(VisualRebuild4.particleCount(false) >= 20)
    }

    @Test fun highContrastStrengthensEdgeSeparation() {
        assertTrue(VisualRebuild4.vignetteAlpha(true) > VisualRebuild4.vignetteAlpha(false))
    }

    @Test fun visualLanguageUsesSceneSpecificAccents() {
        assertTrue(VisualRebuild4.sceneAccent("HOME") != VisualRebuild4.sceneAccent("DESTIN"))
        assertTrue(VisualRebuild4.sceneAccent("VILLE") != VisualRebuild4.sceneAccent("CREATE"))
    }
}
