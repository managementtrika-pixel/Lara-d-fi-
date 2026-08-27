package com.metahumanlegacy.game

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    @Test fun sameSeedProducesSameOriginEventAndOutcome() {
        val a = GameEngine.newCampaign(42L)
        val b = GameEngine.newCampaign(42L)
        assertEquals(a, b)
        val eventA = GameEngine.event(a)
        val eventB = GameEngine.event(b)
        assertEquals(eventA, eventB)
        assertEquals(GameEngine.resolve(a, eventA, eventA.choices.first()), GameEngine.resolve(b, eventB, eventB.choices.first()))
    }

    @Test fun eventsOfferFiveContextualChoices() {
        val c = GameEngine.newCampaign(7L)
        val e = GameEngine.event(c)
        assertEquals(5, e.choices.size)
        assertEquals(5, e.choices.map { it.label }.distinct().size)
        assertTrue(e.provocation.isNotBlank())
    }

    @Test fun dominationChangesIndependentAxes() {
        var c = GameEngine.newCampaign(7L)
        var event = GameEngine.event(c)
        var brutal = event.choices.firstOrNull { it.approach == "DOMINATE" }
        var guard = 0
        while (brutal == null && guard < 20) {
            c = c.copy(turn = c.turn + 1)
            event = GameEngine.event(c)
            brutal = event.choices.firstOrNull { it.approach == "DOMINATE" }
            guard++
        }
        assertNotNull(brutal)
        val next = GameEngine.choose(c, brutal!!)
        assertTrue(next.prestige > c.prestige)
        assertTrue(next.fear > c.fear)
        assertTrue(next.morality < c.morality)
    }

    @Test fun resolutionWritesNarrativeConsequenceToTimeline() {
        val c = GameEngine.newCampaign(1234L)
        val event = GameEngine.event(c)
        val resolution = GameEngine.resolve(c, event, event.choices.first())
        assertTrue(resolution.outcome.length > 80)
        assertTrue(resolution.campaign.timeline.last().startsWith("↳"))
        assertEquals(c.turn + 1, resolution.campaign.turn)
    }

    @Test fun contentSpaceContainsSixHundredSixtyIds() {
        val ids = (0 until 10000).map { turn ->
            val c = GameEngine.newCampaign(991L).copy(turn = turn)
            GameEngine.event(c).id
        }.toSet()
        assertTrue(ids.size > 600)
    }

    @Test fun scenarioWritingActuallyVariesAcrossCampaign() {
        val titles = (0 until 120).map { turn ->
            GameEngine.event(GameEngine.newCampaign(991L).copy(turn = turn)).title.substringBefore(" · ")
        }.toSet()
        assertTrue(titles.size >= 30)
    }

    @Test fun scopeCanGrowWithoutPowerBeingMaxed() {
        var c = GameEngine.newCampaign(88L)
        repeat(110) {
            val event = GameEngine.event(c)
            val moderate = event.choices.minByOrNull { it.risk }!!
            c = GameEngine.choose(c, moderate)
        }
        assertTrue(c.scope.ordinal >= Scope.CITY.ordinal)
        assertTrue(c.power < 100)
    }
}
