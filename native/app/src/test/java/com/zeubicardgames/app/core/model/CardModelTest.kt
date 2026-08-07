package com.zeubicardgames.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardModelTest {
    @Test
    fun `legacy pokemon type is normalized as personnage`() {
        assertEquals(CardType.PERSONNAGE, CardType.from("pokemon"))
        assertEquals(CardType.PERSONNAGE, CardType.from("personnage"))
    }

    @Test
    fun `reply aliases are normalized as replique`() {
        assertEquals(CardType.REPLIQUE, CardType.from("réplique"))
        assertEquals(CardType.REPLIQUE, CardType.from("reply"))
        assertEquals(CardType.REPLIQUE, CardType.from("trap"))
    }

    @Test
    fun `legacy evolution stages map to ZeubiCardGames stages`() {
        assertEquals(EvolutionStage.BASE, EvolutionStage.from("base"))
        assertEquals(EvolutionStage.EVOLUTION, EvolutionStage.from("evo1"))
        assertEquals(EvolutionStage.SUREVOLUTION, EvolutionStage.from("evo2"))
    }

    @Test
    fun `supra rarity is above ultra rare`() {
        assertTrue(Rarity.SUPRA.rank > Rarity.UR.rank)
    }

    @Test
    fun `extension code defaults to permanent upper case id`() {
        val extension = ExtensionDefinition(
            id = "dbz",
            name = "Trika Ball Z",
            subtitle = "Puissance sans limite",
            accent = 0L,
            boosterPath = "boosters/dbz.webp",
            cardCount = 30,
        )

        assertEquals("DBZ", extension.code)
        assertEquals(ContentStatus.ACTIVE, extension.status)
        assertEquals(1, extension.schemaVersion)
    }
}
