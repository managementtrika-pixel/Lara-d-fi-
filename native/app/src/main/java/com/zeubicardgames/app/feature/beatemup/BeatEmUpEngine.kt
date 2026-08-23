package com.zeubicardgames.app.feature.beatemup

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class EnemyKind { GRUNT, ELITE, BOSS }
enum class FxKind { HIT, HEAVY, SPECIAL, KO, HURT, EVOLUTION }

data class FighterSpec(
    val id: String = "zaim",
    val name: String = "Zaim Sinja",
    val maxHp: Float = 240f,
    val moveSpeed: Float = 205f,
    val attack: Float = 31f,
    val defense: Float = 14f,
)

data class StageSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val waves: Int,
    val bossName: String,
    val finalMission: Boolean = false,
)

data class EnemyView(
    val id: Int,
    val kind: EnemyKind,
    val x: Float,
    val y: Float,
    val hp: Float,
    val maxHp: Float,
    val facing: Int,
    val hitFlash: Float,
    val label: String,
    val attacking: Boolean,
)

data class PickupView(val id: Int, val x: Float, val y: Float, val health: Boolean)
data class FxView(val x: Float, val y: Float, val life: Float, val maxLife: Float, val kind: FxKind)

data class GameSnapshot(
    val playerX: Float,
    val playerY: Float,
    val playerHp: Float,
    val playerMaxHp: Float,
    val energy: Float,
    val evolution: Float,
    val form: Int,
    val facing: Int,
    val moving: Boolean,
    val attacking: Boolean,
    val attackStep: Int,
    val invulnerable: Boolean,
    val enemies: List<EnemyView>,
    val pickups: List<PickupView>,
    val effects: List<FxView>,
    val wave: Int,
    val totalWaves: Int,
    val score: Int,
    val combo: Int,
    val maxCombo: Int,
    val comboTimer: Float,
    val stageComplete: Boolean,
    val playerDown: Boolean,
    val bossActive: Boolean,
    val elapsed: Float,
)

class BeatEmUpEngine(
    val fighter: FighterSpec,
    val stage: StageSpec,
    private val difficulty: Int = 1,
    seed: Int = stage.id.hashCode() * 31 + difficulty,
) {
    companion object {
        const val WORLD_W = 1000f
        const val WORLD_H = 560f
        const val FLOOR_TOP = 245f
        const val FLOOR_BOTTOM = 515f
    }

    private data class Enemy(
        val id: Int,
        val kind: EnemyKind,
        var x: Float,
        var y: Float,
        var hp: Float,
        val maxHp: Float,
        val speed: Float,
        val damage: Float,
        val label: String,
        var facing: Int = -1,
        var attackCooldown: Float = 0f,
        var attackPose: Float = 0f,
        var stun: Float = 0f,
        var hitFlash: Float = 0f,
        var knockX: Float = 0f,
    )

    private data class Pickup(val id: Int, var x: Float, var y: Float, val health: Boolean, var ttl: Float = 10f)
    private data class Fx(val x: Float, val y: Float, var life: Float, val maxLife: Float, val kind: FxKind)

    private val random = Random(seed)
    private val enemies = mutableListOf<Enemy>()
    private val pickups = mutableListOf<Pickup>()
    private val effects = mutableListOf<Fx>()

    private var playerX = 170f
    private var playerY = 390f
    private var playerHp = fighter.maxHp
    private var energy = 25f
    private var evolution = 0f
    private var form = 0
    private var facing = 1
    private var moveX = 0f
    private var moveY = 0f
    private var attackCooldown = 0f
    private var attackPose = 0f
    private var attackStep = 0
    private var invuln = 0f
    private var dashTime = 0f
    private var dashDirX = 1f
    private var dashDirY = 0f
    private var lightQueued = false
    private var heavyQueued = false
    private var dashQueued = false
    private var specialQueued = false
    private var evolutionQueued = false

    private var wave = 0
    private var spawnDelay = 0.55f
    private var nextId = 1
    private var score = 0
    private var combo = 0
    private var maxCombo = 0
    private var comboTimer = 0f
    private var comboStep = 0
    private var stageComplete = false
    private var playerDown = false
    private var elapsed = 0f
    private var bossSpawned = false
    private var cameraShake = 0f

    init { spawnNextWave() }

    fun setMove(x: Float, y: Float) {
        val len = hypot(x, y)
        if (len > 1f) { moveX = x / len; moveY = y / len } else { moveX = x; moveY = y }
        if (abs(moveX) > 0.08f) facing = if (moveX >= 0f) 1 else -1
    }

    fun pressLight() { lightQueued = true }
    fun pressHeavy() { heavyQueued = true }
    fun pressDash() { dashQueued = true }
    fun pressSpecial() { specialQueued = true }
    fun pressEvolution() { evolutionQueued = true }

    fun update(rawDt: Float): GameSnapshot {
        val dt = rawDt.coerceIn(0f, 0.034f)
        if (stageComplete || playerDown) return snapshot()
        elapsed += dt
        attackCooldown = max(0f, attackCooldown - dt)
        attackPose = max(0f, attackPose - dt)
        invuln = max(0f, invuln - dt)
        comboTimer = max(0f, comboTimer - dt)
        cameraShake = max(0f, cameraShake - dt)
        if (comboTimer <= 0f) { combo = 0; comboStep = 0 }

        if (evolutionQueued) evolve()
        evolutionQueued = false
        if (dashQueued && dashTime <= 0f) startDash()
        dashQueued = false
        if (specialQueued) performSpecial()
        specialQueued = false
        if (heavyQueued) performHeavy()
        heavyQueued = false
        if (lightQueued) performLight()
        lightQueued = false

        updatePlayerMovement(dt)
        updateEnemies(dt)
        updatePickups(dt)
        updateEffects(dt)

        if (enemies.isEmpty() && !stageComplete) {
            spawnDelay -= dt
            if (spawnDelay <= 0f) spawnNextWave()
        } else spawnDelay = 0.72f

        if (playerHp <= 0f) {
            playerHp = 0f; playerDown = true
            effects += Fx(playerX, playerY - 20f, 0.8f, 0.8f, FxKind.KO)
        }
        return snapshot()
    }

    fun snapshot(): GameSnapshot = GameSnapshot(
        playerX, playerY, playerHp, fighter.maxHp, energy, evolution, form, facing,
        hypot(moveX, moveY) > 0.16f && dashTime <= 0f && attackPose <= 0.12f,
        attackPose > 0f, attackStep, invuln > 0f,
        enemies.map { EnemyView(it.id, it.kind, it.x, it.y, it.hp, it.maxHp, it.facing, it.hitFlash, it.label, it.attackPose > 0f) },
        pickups.map { PickupView(it.id, it.x, it.y, it.health) },
        effects.map { FxView(it.x, it.y, it.life, it.maxLife, it.kind) },
        wave, stage.waves + 1, score, combo, maxCombo, comboTimer, stageComplete, playerDown,
        enemies.any { it.kind == EnemyKind.BOSS }, elapsed,
    )

    fun shakeAmount(): Float = cameraShake
    private fun formPower() = 1f + form * 0.34f
    private fun formSpeed() = 1f + form * 0.08f

    private fun evolve() {
        if (evolution < 100f || form >= 2 || attackCooldown > 0f) return
        form++; evolution = 0f
        playerHp = min(fighter.maxHp, playerHp + fighter.maxHp * 0.28f)
        energy = min(100f, energy + 25f)
        invuln = 1.2f; attackPose = 0.8f; attackCooldown = 0.85f; cameraShake = 0.55f
        effects += Fx(playerX, playerY - 35f, 1f, 1f, FxKind.EVOLUTION)
        score += 1000 * form
    }

    private fun startDash() {
        val len = hypot(moveX, moveY)
        if (len > 0.12f) { dashDirX = moveX / len; dashDirY = moveY / len } else { dashDirX = facing.toFloat(); dashDirY = 0f }
        dashTime = 0.17f; invuln = max(invuln, 0.34f); attackCooldown = max(attackCooldown, 0.12f)
    }

    private fun updatePlayerMovement(dt: Float) {
        if (dashTime > 0f) {
            dashTime -= dt
            val speed = fighter.moveSpeed * formSpeed() * 3.4f
            playerX += dashDirX * speed * dt; playerY += dashDirY * speed * 0.72f * dt
        } else if (attackPose <= 0.16f) {
            val speed = fighter.moveSpeed * formSpeed()
            playerX += moveX * speed * dt; playerY += moveY * speed * 0.72f * dt
        }
        playerX = playerX.coerceIn(42f, WORLD_W - 42f); playerY = playerY.coerceIn(FLOOR_TOP, FLOOR_BOTTOM)
    }

    private fun performLight() {
        if (attackCooldown > 0f || dashTime > 0f) return
        comboStep = if (comboTimer > 0f) (comboStep % 4) + 1 else 1; attackStep = comboStep
        attackCooldown = 0.18f; attackPose = 0.18f
        val damage = fighter.attack * formPower() * when (comboStep) { 1 -> .78f; 2 -> .88f; 3 -> 1.02f; else -> 1.30f }
        val hits = damageEnemies(if (comboStep == 4) 115f else 92f, 54f, damage, 26f + comboStep * 8f, true)
        if (hits > 0) registerCombo(hits, if (comboStep == 4) 150 else 90)
    }

    private fun performHeavy() {
        if (attackCooldown > 0f || dashTime > 0f) return
        attackStep = 2; attackCooldown = 0.46f; attackPose = 0.36f
        val hits = damageEnemies(130f, 70f, fighter.attack * formPower() * 1.75f, 96f, true, .30f)
        if (hits > 0) { registerCombo(hits, 210); cameraShake = .18f; effects += Fx(playerX + facing * 75f, playerY - 35f, .28f, .28f, FxKind.HEAVY) }
    }

    private fun performSpecial() {
        if (energy < 100f || attackCooldown > 0f) return
        energy = 0f; attackStep = 2; attackCooldown = .82f; attackPose = .72f; invuln = max(invuln, .95f)
        val radius = 245f + form * 38f; var hits = 0
        enemies.toList().forEach { enemy ->
            if (hypot(enemy.x - playerX, (enemy.y - playerY) * 1.35f) <= radius) {
                hitEnemy(enemy, fighter.attack * formPower() * (2.55f + form * .32f), 155f, .85f); hits++
            }
        }
        effects += Fx(playerX, playerY - 30f, .72f, .72f, FxKind.SPECIAL)
        if (hits > 0) registerCombo(hits, 320)
        cameraShake = .44f
    }

    private fun damageEnemies(range: Float, laneRange: Float, damage: Float, knock: Float, onlyFront: Boolean, stun: Float = .12f): Int {
        var hits = 0
        enemies.toList().forEach { enemy ->
            val dx = enemy.x - playerX
            if ((!onlyFront || dx * facing >= -18f) && abs(dx) <= range && abs(enemy.y - playerY) <= laneRange) {
                hitEnemy(enemy, damage, knock, stun); hits++
            }
        }
        return hits
    }

    private fun hitEnemy(enemy: Enemy, damage: Float, knock: Float, stun: Float) {
        enemy.hp -= damage * if (enemy.kind == EnemyKind.BOSS) .84f else 1f
        enemy.hitFlash = .12f; enemy.stun = max(enemy.stun, stun); enemy.knockX += facing * knock
        energy = min(100f, energy + if (enemy.kind == EnemyKind.BOSS) 7f else 10f)
        evolution = min(100f, evolution + if (enemy.kind == EnemyKind.BOSS) 5f else 8f)
        effects += Fx(enemy.x, enemy.y - 54f, .18f, .18f, FxKind.HIT)
        if (enemy.hp <= 0f) killEnemy(enemy)
    }

    private fun registerCombo(hits: Int, pointsPerHit: Int) {
        combo += hits; maxCombo = max(maxCombo, combo); comboTimer = 2.1f
        score += pointsPerHit * hits * (1 + combo / 8)
    }

    private fun killEnemy(enemy: Enemy) {
        if (!enemies.remove(enemy)) return
        val bonus = when (enemy.kind) { EnemyKind.GRUNT -> 350; EnemyKind.ELITE -> 720; EnemyKind.BOSS -> 5000 }
        score += bonus * max(1, combo / 3)
        energy = min(100f, energy + if (enemy.kind == EnemyKind.BOSS) 30f else 15f)
        evolution = min(100f, evolution + if (enemy.kind == EnemyKind.BOSS) 30f else 18f)
        effects += Fx(enemy.x, enemy.y - 28f, .64f, .64f, FxKind.KO)
        cameraShake = max(cameraShake, if (enemy.kind == EnemyKind.BOSS) .55f else .18f)
        if (enemy.kind == EnemyKind.BOSS) {
            stageComplete = true; score += max(0, (180f - elapsed).toInt()) * 40
        } else {
            val roll = random.nextFloat()
            if (roll < .13f || (playerHp < fighter.maxHp * .35f && roll < .27f)) pickups += Pickup(nextId++, enemy.x, enemy.y, true)
            else if (roll < .31f) pickups += Pickup(nextId++, enemy.x, enemy.y, false)
        }
    }

    private fun updateEnemies(dt: Float) {
        enemies.toList().forEach { enemy ->
            enemy.hitFlash = max(0f, enemy.hitFlash - dt); enemy.attackCooldown = max(0f, enemy.attackCooldown - dt)
            enemy.attackPose = max(0f, enemy.attackPose - dt); enemy.stun = max(0f, enemy.stun - dt)
            if (abs(enemy.knockX) > 1f) { enemy.x += enemy.knockX * dt; enemy.knockX *= (1f - 7.5f * dt).coerceAtLeast(0f) }
            enemy.x = enemy.x.coerceIn(32f, WORLD_W - 32f); enemy.y = enemy.y.coerceIn(FLOOR_TOP, FLOOR_BOTTOM)
            if (enemy.stun > 0f) return@forEach
            val dx = playerX - enemy.x; val dy = playerY - enemy.y; val distance = hypot(dx, dy * 1.3f)
            enemy.facing = if (dx >= 0f) 1 else -1
            val range = when (enemy.kind) { EnemyKind.GRUNT -> 72f; EnemyKind.ELITE -> 88f; EnemyKind.BOSS -> 108f }
            if (distance > range) {
                val len = hypot(dx, dy).coerceAtLeast(1f)
                enemy.x += dx / len * enemy.speed * dt; enemy.y += dy / len * enemy.speed * .72f * dt
            } else if (enemy.attackCooldown <= 0f) enemyAttack(enemy)
        }
    }

    private fun enemyAttack(enemy: Enemy) {
        enemy.attackPose = .24f
        enemy.attackCooldown = when (enemy.kind) { EnemyKind.GRUNT -> 1.10f; EnemyKind.ELITE -> .92f; EnemyKind.BOSS -> .74f } / (1f + .12f * (difficulty - 1).coerceAtLeast(0))
        if (invuln > 0f) return
        var damage = enemy.damage * (1f + .18f * (difficulty - 1).coerceAtLeast(0)); damage *= 100f / (100f + fighter.defense + form * 4f)
        playerHp -= damage; invuln = .34f; combo = 0; comboTimer = 0f
        effects += Fx(playerX, playerY - 48f, .24f, .24f, FxKind.HURT)
        cameraShake = max(cameraShake, if (enemy.kind == EnemyKind.BOSS) .28f else .12f)
    }

    private fun updatePickups(dt: Float) {
        pickups.toList().forEach { pickup ->
            pickup.ttl -= dt
            if (pickup.ttl <= 0f) { pickups.remove(pickup); return@forEach }
            if (hypot(pickup.x - playerX, pickup.y - playerY) < 42f) {
                if (pickup.health) playerHp = min(fighter.maxHp, playerHp + fighter.maxHp * .22f) else energy = min(100f, energy + 32f)
                score += 180; pickups.remove(pickup)
            }
        }
    }

    private fun updateEffects(dt: Float) { effects.forEach { it.life -= dt }; effects.removeAll { it.life <= 0f } }

    private fun spawnNextWave() {
        if (wave < stage.waves) {
            wave++; val count = 2 + wave + (difficulty - 1).coerceAtLeast(0)
            repeat(count) { index -> spawnEnemy(if (wave >= 2 && index == count - 1) EnemyKind.ELITE else EnemyKind.GRUNT, index) }
        } else if (!bossSpawned) { bossSpawned = true; wave = stage.waves + 1; spawnEnemy(EnemyKind.BOSS, 0) }
    }

    private fun spawnEnemy(kind: EnemyKind, index: Int) {
        val x = 700f + random.nextFloat() * 245f
        val y = FLOOR_TOP + 35f + random.nextFloat() * (FLOOR_BOTTOM - FLOOR_TOP - 55f)
        val d = 1f + .24f * (difficulty - 1).coerceAtLeast(0); val w = 1f + .09f * max(0, wave - 1)
        val hp = when (kind) { EnemyKind.GRUNT -> 58f; EnemyKind.ELITE -> 118f; EnemyKind.BOSS -> if (stage.finalMission) 560f else 370f }
        val speed = when (kind) { EnemyKind.GRUNT -> 105f; EnemyKind.ELITE -> 90f; EnemyKind.BOSS -> 96f }
        val damage = when (kind) { EnemyKind.GRUNT -> 13f; EnemyKind.ELITE -> 21f; EnemyKind.BOSS -> 30f }
        val label = when (kind) { EnemyKind.GRUNT -> "SinANBU ${index + 1}"; EnemyKind.ELITE -> "RoobANBU"; EnemyKind.BOSS -> stage.bossName }
        val maxHp = hp * d * w
        enemies += Enemy(nextId++, kind, x, y, maxHp, maxHp, speed * (1f + .04f * (difficulty - 1)), damage, label)
    }
}
