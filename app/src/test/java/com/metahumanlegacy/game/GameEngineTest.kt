package com.metahumanlegacy.game

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    @Test fun sameSeedProducesSameCampaignEventAndOutcome() {
        val a = GameEngine.newCampaign(42L)
        val b = GameEngine.newCampaign(42L)
        assertEquals(a, b)
        val eventA = GameEngine.event(a)
        val eventB = GameEngine.event(b)
        assertEquals(eventA, eventB)
        assertEquals(GameEngine.resolve(a, eventA, eventA.choices.first()), GameEngine.resolve(b, eventB, eventB.choices.first()))
    }

    @Test fun completeCreationChangesStartingCharacter() {
        val base = GameEngine.randomBlueprint(5L).copy(
            firstName = "Maya",
            lastName = "Vale",
            alias = "Aster",
            motivation = "Protéger les miens",
            visualStyle = "Visage découvert"
        )
        val military = GameEngine.newCampaign(99L, base.copy(socialBackground = "Famille militaire"))
        val unstable = GameEngine.newCampaign(99L, base.copy(socialBackground = "Foyer instable"))
        assertEquals("Maya Vale", military.name)
        assertEquals("Aster", military.alias)
        assertTrue(military.control > unstable.control)
        assertTrue(military.familyBond > 50)
        assertTrue(military.identityExposure >= 30)
    }

    @Test fun eventsOfferSixContextualChoices() {
        val c = GameEngine.newCampaign(7L)
        val e = GameEngine.event(c)
        assertEquals(6, e.choices.size)
        assertEquals(6, e.choices.map { it.label }.distinct().size)
        assertTrue(e.provocation.isNotBlank())
        assertTrue(e.choices.all { it.sourceCategory == e.category })
    }

    @Test fun majorDominationChoiceHasLargeConsequences() {
        var c = GameEngine.newCampaign(71L)
        var chosen: Choice? = null
        var event: EventNode? = null
        repeat(80) { turn ->
            val candidateCampaign = c.copy(turn = turn)
            val candidateEvent = GameEngine.event(candidateCampaign)
            val candidate = candidateEvent.choices.firstOrNull { it.approach == "DOMINATE" && it.stakes == 3 }
            if (chosen == null && candidate != null) {
                c = candidateCampaign
                event = candidateEvent
                chosen = candidate
            }
        }
        assertNotNull(event)
        assertNotNull(chosen)
        val next = GameEngine.choose(c, chosen!!)
        assertTrue(next.prestige > c.prestige)
        assertTrue(next.fear >= c.fear + 15)
        assertTrue(next.morality <= c.morality - 15)
    }

    @Test fun resolutionWritesNarrativeConsequenceToTimeline() {
        val c = GameEngine.newCampaign(1234L)
        val event = GameEngine.event(c)
        val resolution = GameEngine.resolve(c, event, event.choices.first())
        assertTrue(resolution.outcome.length > 100)
        assertTrue(resolution.campaign.timeline.last().startsWith("↳"))
        assertEquals(c.turn + 1, resolution.campaign.turn)
    }

    @Test fun choicesOpenThreadsThatReturnLater() {
        var c = GameEngine.newCampaign(551L)
        var openingEvent = GameEngine.event(c)
        var guard = 0
        while (openingEvent.threadId == null && guard < 40) {
            c = c.copy(turn = c.turn + 1)
            openingEvent = GameEngine.event(c)
            guard++
        }
        assertNotNull(openingEvent.threadId)
        val openingChoice = openingEvent.choices.first { it.threadId != null }
        val after = GameEngine.resolve(c, openingEvent, openingChoice).campaign
        assertTrue(after.threads.isNotEmpty())

        var futureTurn = after.turn + 2
        while (futureTurn % 5 != 0) futureTurn++
        val followUp = GameEngine.event(after.copy(turn = futureTurn))
        assertTrue(followUp.threadStage > 0)
        assertEquals(after.threads.first().id, followUp.threadId)
        assertTrue(followUp.text.contains("souvi", ignoreCase = true) || followUp.text.contains("ancien", ignoreCase = true))
    }

    @Test fun eventAddressSpaceIsExpanded() {
        val ids = (0 until 15000).map { turn ->
            GameEngine.event(GameEngine.newCampaign(991L).copy(turn = turn)).id
        }.toSet()
        assertTrue(ids.size > 850)
    }

    @Test fun authoredSituationTitlesVarySubstantially() {
        val titles = (0 until 300).map { turn ->
            GameEngine.event(GameEngine.newCampaign(991L).copy(turn = turn)).title
        }.toSet()
        assertTrue(titles.size >= 55)
    }

    @Test fun scopeCanGrowWithoutPowerBeingMaxed() {
        var c = GameEngine.newCampaign(88L)
        repeat(120) {
            val event = GameEngine.event(c)
            val effective = event.choices.filter { it.approach != "DOMINATE" }.maxByOrNull { it.impact } ?: event.choices.first()
            c = GameEngine.choose(c, effective)
        }
        assertTrue(c.scope.ordinal >= Scope.CITY.ordinal)
        assertTrue(c.power <= 100)
    }
}
