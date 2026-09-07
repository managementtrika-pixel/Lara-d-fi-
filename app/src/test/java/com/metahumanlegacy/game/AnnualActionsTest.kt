package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnnualActionsTest {
    @Before
    fun installNarrativeBundle() {
        NarrativeCodec.installAssetParts { path -> File("src/main/assets/$path").readBytes() }
    }

    @Test
    fun sideActionNeverAdvancesTheYear() {
        val c = GameEngine.newCampaign(7001L)
        val state = AnnualActionState.fresh(c)
        val card = AnnualActionEngine.available(c, state).first()
        val result = AnnualActionEngine.perform(c, state, card)
        assertNotNull(result)
        assertEquals(c.turn, result!!.campaign.turn)
        assertEquals(c.age, result.campaign.age)
        assertEquals(1, result.state.used)
    }

    @Test
    fun civilianActionsCannotInfluenceHiddenPowerFormation() {
        val c = GameEngine.newCampaign(7002L)
        val state = AnnualActionState.fresh(c)
        var after = c
        var s = state
        repeat(3) {
            val card = AnnualActionEngine.available(after, s).first()
            val r = AnnualActionEngine.perform(after, s, card)!!
            after = r.campaign
            s = r.state
        }
        assertEquals(c.affinityScores, after.affinityScores)
        assertEquals(c.expressionScores, after.expressionScores)
        assertEquals(c.costScores, after.costScores)
        assertEquals("Non révélé", after.powerFamily)
        assertFalse(after.powerRevealed)
    }

    @Test
    fun exactlyThreeSideActionsAreAvailablePerNarrativeTurn() {
        val c = GameEngine.newCampaign(7003L)
        var state = AnnualActionState.fresh(c)
        var current = c
        repeat(3) {
            val card = AnnualActionEngine.available(current, state).first()
            val result = AnnualActionEngine.perform(current, state, card)!!
            current = result.campaign
            state = result.state
        }
        assertEquals(0, state.remaining)
        assertTrue(AnnualActionEngine.available(current, state).isEmpty())
        val anyCard = AnnualActionCard(
            id = "test", title = "Test", description = "Test",
            category = AnnualActionCategory.CIVIL, iconKey = "alt_01",
            focus = "Test", outcome = "Test"
        )
        assertNull(AnnualActionEngine.perform(current, state, anyCard))
    }

    @Test
    fun mainDecisionResetsSlotsButPreservesLearnedSkills() {
        var c = GameEngine.newCampaign(7004L)
        var state = AnnualActionState.fresh(c)
        val side = AnnualActionEngine.available(c, state).first { it.rescue > 0 || it.discipline > 0 || it.investigation > 0 || it.presence > 0 }
        val sideResult = AnnualActionEngine.perform(c, state, side)!!
        c = sideResult.campaign
        state = sideResult.state
        val learned = listOf(state.rescue, state.investigation, state.presence, state.discipline)

        val event = GameEngine.event(c)
        c = GameEngine.resolve(c, event, event.choices.first()).campaign
        state = state.synced(c)

        assertEquals(ANNUAL_ACTION_LIMIT, state.remaining)
        assertEquals(learned, listOf(state.rescue, state.investigation, state.presence, state.discipline))
        assertTrue(state.usedIds.isEmpty())
    }

    @Test
    fun availableCardsAreDeterministicForSameLifeAndYear() {
        val c = GameEngine.newCampaign(7005L)
        val state = AnnualActionState.fresh(c)
        val a = AnnualActionEngine.available(c, state).map { it.id }
        val b = AnnualActionEngine.available(c, state).map { it.id }
        assertEquals(a, b)
        assertNotEquals(emptyList<String>(), a)
    }

    @Test
    fun childhoodSideActionsStayAgeAppropriateAndCivil() {
        var c = GameEngine.newCampaign(7010L)
        repeat(10) { year ->
            assertEquals(8 + year, c.age)
            val state = AnnualActionState.fresh(c)
            val cards = AnnualActionEngine.available(c, state)
            assertTrue(cards.isNotEmpty())
            assertTrue(cards.all { it.id.startsWith("child_") })
            assertTrue(cards.none { it.requiresPower })
            assertTrue(cards.none { it.title.contains("finances", ignoreCase = true) })
            assertTrue(cards.none { it.title.contains("interview", ignoreCase = true) })

            val event = GameEngine.event(c)
            c = GameEngine.resolve(c, event, event.choices.first()).campaign
        }
        assertEquals(18, c.age)
        assertTrue(AnnualActionEngine.available(c, AnnualActionState.fresh(c)).isEmpty())
    }

    @Test
    fun awakeningTurnBlocksSideErrands() {
        var c = GameEngine.newCampaign(7006L)
        repeat(10) {
            val event = GameEngine.event(c)
            c = GameEngine.resolve(c, event, event.choices.first()).campaign
        }
        assertFalse(c.powerRevealed)
        assertEquals("AWAKENING", GameEngine.event(c).kind)
        assertTrue(AnnualActionEngine.available(c, AnnualActionState.fresh(c)).isEmpty())
    }
}
