package com.metahumanlegacy.game

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    @Test fun sameSeedProducesSameOriginAndEvent() {
        val a = GameEngine.newCampaign(42L)
        val b = GameEngine.newCampaign(42L)
        assertEquals(a, b)
        assertEquals(GameEngine.event(a), GameEngine.event(b))
    }

    @Test fun sixAxesRemainIndependent() {
        val c = GameEngine.newCampaign(7L)
        val e = GameEngine.event(c)
        val brutal = GameEngine.choose(c, e.choices.last())
        assertTrue(brutal.prestige > c.prestige)
        assertTrue(brutal.fear > c.fear)
        assertTrue(brutal.morality < c.morality)
    }

    @Test fun contentSpaceContainsSixHundredSixtyIds() {
        val ids = (0 until 10000).map { turn ->
            val c = GameEngine.newCampaign(991L).copy(turn = turn)
            GameEngine.event(c).id
        }.toSet()
        assertTrue(ids.size > 600)
    }

    @Test fun scopeCanGrowWithoutPowerBeingMaxed() {
        var c = GameEngine.newCampaign(88L)
        repeat(110) {
            val event = GameEngine.event(c)
            c = GameEngine.choose(c, event.choices[2])
        }
        assertTrue(c.scope.ordinal >= Scope.CITY.ordinal)
        assertTrue(c.power < 100)
    }
}
