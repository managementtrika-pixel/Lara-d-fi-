package com.zeubicardgames.app

import com.zeubicardgames.app.core.data.GameRepository
import com.zeubicardgames.app.core.gameengine.BattleEngine
import com.zeubicardgames.app.core.model.*
import org.junit.Assert.*
import org.junit.Test

class GameRulesTest {
    private fun card(name: String, hp: Int = 100, damage: Int = 40, cost: Int = 1) = CardDefinition(name, "test", "001", name, "pokemon", "base", null, hp, 1, Rarity.R, listOf(Attack("Impact", damage, cost)), null, emptyList())
    @Test fun attackRequiresEnergy() { val engine = BattleEngine(1); val start = engine.start(card("A"), card("B")); assertEquals(start, engine.attack(start, start.player!!.card.attacks.first())) }
    @Test fun attackDealsDamageAfterEnergy() { val engine = BattleEngine(1); var s = engine.start(card("A"), card("B")); s = engine.attachEnergy(s); s = engine.attack(s, s.player!!.card.attacks.first()); assertTrue(s.opponent!!.damage >= 40) }
    @Test fun deterministicSeedCreatesSameInstances() { val a = BattleEngine(42).start(card("A"), card("B")); val b = BattleEngine(42).start(card("A"), card("B")); assertEquals(a.player!!.instanceId, b.player!!.instanceId) }
}
