package com.metahumanlegacy.game

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PixelAvatarIdentityTest {
    @Test
    fun chosenCostumePaletteOverridesPowerFamilyColors() {
        val palette = pixelHeroPalette("Noir / argent", "Énergie")

        assertEquals(Color(0xFF15191F), palette.first)
        assertEquals(Color(0xFFC6CDD6), palette.second)
        assertNotEquals(Color(0xFF205BD7), palette.first)
    }

    @Test
    fun distinctCostumeChoicesProduceDistinctHeroPalettes() {
        val blueGold = pixelHeroPalette("Bleu / or", "Force")
        val redAnthracite = pixelHeroPalette("Rouge / anthracite", "Force")

        assertNotEquals(blueGold, redAnthracite)
        assertEquals(Color(0xFF1F4E9A), blueGold.first)
        assertEquals(Color(0xFFF2C85A), blueGold.second)
        assertEquals(Color(0xFF8F2831), redAnthracite.first)
        assertEquals(Color(0xFF343A42), redAnthracite.second)
    }

    @Test
    fun powerPersonalizedPaletteFallsBackToPowerIdentity() {
        val palette = pixelHeroPalette("Personnalisée au pouvoir", "Force")

        assertEquals(Color(0xFF8D2834), palette.first)
        assertEquals(Color(0xFFFFC85B), palette.second)
    }
}
