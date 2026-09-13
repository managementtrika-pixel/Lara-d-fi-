package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        assertNotEquals(VisualRebuild4.sceneAccent("HOME"), VisualRebuild4.sceneAccent("DESTIN"))
        assertNotEquals(VisualRebuild4.sceneAccent("VILLE"), VisualRebuild4.sceneAccent("CREATE"))
    }

    @Test fun majorPowerFamiliesHaveDistinctVfxLanguages() {
        assertEquals(PowerVfxKind.FIRE, powerVfxKind("Feu"))
        assertEquals(PowerVfxKind.ICE, powerVfxKind("Glace"))
        assertEquals(PowerVfxKind.ENERGY, powerVfxKind("Énergie électrique"))
        assertEquals(PowerVfxKind.PSYCHIC, powerVfxKind("Mental psychique"))
        assertEquals(PowerVfxKind.COSMIC, powerVfxKind("Cosmique gravité"))
        assertNotEquals(powerVfxKind("Feu"), powerVfxKind("Mental"))
    }

    @Test fun avatarGrowthContractIsPreservedByFourPointZeroRenderer() {
        assertTrue(pixelLegHeight(8, "Moyenne") < pixelLegHeight(18, "Moyenne"))
        assertTrue(pixelLegHeight(18, "Petite") < pixelLegHeight(18, "Grande"))
        assertEquals(0, pixelAgeTier(17))
        assertEquals(1, pixelAgeTier(18))
        assertTrue(pixelAgeTier(65) > pixelAgeTier(35))
    }

    @Test fun sceneRouterSeparatesMenuAndArchiveContexts() {
        assertEquals(CinematicSceneKind.HOME, cinematicSceneKind("HOME", null))
        assertEquals(CinematicSceneKind.DOSSIER, cinematicSceneKind("HALL", null))
        assertEquals(CinematicSceneKind.DOSSIER, cinematicSceneKind("SETTINGS", null))
    }

    @Test fun visualPressureMirrorsPersistentWorldState() {
        val state = UltimateState(
            seed = 7L,
            bodyBuild = "Athlétique",
            stature = "Moyenne",
            skinTone = "Moyen",
            faceShape = "Ovale",
            hair = "Court texturé",
            hairColor = "Brun",
            facialHair = "Aucune",
            eyes = "Bruns",
            civilianStyle = "Street sobre",
            accessory = "Aucun",
            cityArchetype = "Métropole verticale",
            climate = "Quatre saisons",
            architecture = "Contemporaine",
            cityMood = "Contrastes sociaux",
            cityCondition = 20,
            cityTech = 90,
            legalStatus = "Recherché",
            mediaFrame = "Icône controversée"
        )
        val visual = worldVisualPressure(state)
        assertTrue(visual.damage > .70f)
        assertTrue(visual.technology > .80f)
        assertTrue(visual.surveillance > .80f)
        assertTrue(visual.publicAttention > .80f)
    }
}
