package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CinematicAdultDestiny44Test {
    @Test fun techniqueChoicesAreVisuallyDistinguished() {
        val choice = Choice(label = "Technique", flag = "story_technique:burst")
        assertEquals("TECHNIQUE", CinematicAdult44.choiceKind(choice))
    }

    @Test fun identityPressureChoicesAreVisuallyDistinguished() {
        val choice = Choice(label = "Identité", flag = "identity_pressure:contain")
        assertEquals("IDENTITÉ", CinematicAdult44.choiceKind(choice))
    }

    @Test fun extremeRiskGetsDangerTreatment() {
        val choice = Choice(label = "Danger", risk = 7)
        assertEquals("DANGER", CinematicAdult44.choiceKind(choice))
    }

    @Test fun powerIntensityAlwaysStaysInSafeVisualRange() {
        val low = Campaign(seed = 1L, name = "A", modifier = "x", power = 0)
        val high = low.copy(power = 100)
        val calm = EventNode("a", "A", "A", emptyList(), "TEST", "", 1)
        val crisis = calm.copy(id = "b", stakes = 8)
        val state = UltimateState(
            seed = 1L,
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
            cityMood = "Contrastes sociaux"
        )

        val lowValue = CinematicAdult44.powerIntensity(low, state, calm)
        val highValue = CinematicAdult44.powerIntensity(high, state.copy(powerStrain = 100), crisis)
        assertTrue(lowValue >= .18f)
        assertTrue(highValue <= 1f)
        assertTrue(highValue > lowValue)
    }
}
