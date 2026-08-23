package com.zeubicardgames.app.feature.beatemup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatEmUpEngineTest {
    private val fighter = FighterSpec(
        id = "test",
        name = "Test Fighter",
        maxHp = 250f,
        moveSpeed = 180f,
        attack = 35f,
        defense = 15f,
    )

    private val stage = StageSpec(
        id = "test_stage",
        name = "Test Stage",
        subtitle = "Combat",
        waves = 3,
        bossName = "Test Boss",
    )

    @Test
    fun firstWaveIsReadyImmediately() {
        val engine = BeatEmUpEngine(fighter, stage, seed = 42)
        val snapshot = engine.snapshot()
        assertEquals(1, snapshot.wave)
        assertFalse(snapshot.stageComplete)
        assertTrue(snapshot.enemies.isNotEmpty())
    }

    @Test
    fun playerMovementAdvancesInWorldSpace() {
        val engine = BeatEmUpEngine(fighter, stage, seed = 42)
        val start = engine.snapshot().playerX
        engine.setMove(1f, 0f)
        repeat(30) { engine.update(1f / 60f) }
        assertTrue(engine.snapshot().playerX > start)
    }

    @Test
    fun dashMovesFartherThanNormalMovementAndGrantsInvulnerability() {
        val normal = BeatEmUpEngine(fighter, stage, seed = 42)
        val dashed = BeatEmUpEngine(fighter, stage, seed = 42)
        val start = normal.snapshot().playerX

        normal.setMove(1f, 0f)
        dashed.setMove(1f, 0f)
        dashed.pressDash()
        repeat(8) {
            normal.update(1f / 60f)
            dashed.update(1f / 60f)
        }

        assertTrue(dashed.snapshot().playerX - start > normal.snapshot().playerX - start)
        assertTrue(dashed.snapshot().invulnerable)
    }

    @Test
    fun specialCannotFireBeforeEnergyIsFull() {
        val engine = BeatEmUpEngine(fighter, stage, seed = 42)
        val before = engine.snapshot().energy
        engine.pressSpecial()
        val after = engine.update(1f / 60f).energy
        assertEquals(before, after, 0.001f)
    }
}
