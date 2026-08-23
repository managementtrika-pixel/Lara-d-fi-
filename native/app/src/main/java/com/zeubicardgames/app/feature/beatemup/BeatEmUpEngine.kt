package com.zeubicardgames.app.feature.beatemup

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class FighterStyle { BALANCED, SPEED, POWER, ENERGY }
enum class EnemyKind { GRUNT, BRUISER, RANGED, BOSS }
enum class FxKind { HIT, HEAVY, SPECIAL, KO, HURT }

data class FighterSpec(
    val id: String,
    val name: String,
    val cardName: String,
    val setId: String,
    val style: FighterStyle,
    val maxHp: Float,
    val moveSpeed: Float,
    val attack: Float,
    val defense: Float,
    val specialName: String,
    val accentArgb: Long,
)

data class StageSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val setId: String,
    val boosterPath: String,
    val bossCardName: String,
    val accentArgb: Long,
    val waves: Int = 3,
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
)

data class PickupView(
    val id: Int,
    val x: Float,
    val y: Float,
    val health: Boolean,
)

data class FxView(
    val x: Float,
    val y: Float,
    val life: Float,
    val maxLife: Float,
    val kind: FxKind,
)

data class GameSnapshot(
    val playerX: Float,
    val playerY: Float,
    val playerHp: Float,
    val playerMaxHp: Float,
    val energy: Float,
    val facing: Int,
    val attackPose: Float,
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
    seed: Int = stage.id.hashCode() * 31 + fighter.id.hashCode(),
) {
    companion object {
        const val WORLD_W = 1000f
        const val WORLD_H = 560f
        const val FLOOR_TOP = 235f
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
        var stun: Float = 0f,
        var hitFlash: Float = 0f,
        var knockX: Float = 0f,
        var knockY: Float = 0f,
        var bossBurst: Float = 0f,
    )

    private data class Pickup(
        val id: Int,
        var x: Float,
        var y: Float,
        val health: Boolean,
        var ttl: Float = 10f,
    )

    private data class Fx(
        val x: Float,
        val y: Float,
        var life: Float,
        val maxLife: Float,
        val kind: FxKind,
    )

    private val random = Random(seed)
    private val enemies = mutableListOf<Enemy>()
    private val pickups = mutableListOf<Pickup>()
    private val effects = mutableListOf<Fx>()

    private var playerX = 190f
    private var playerY = 390f
    private var playerHp = fighter.maxHp
    private var energy = 20f
    private var facing = 1
    private var moveX = 0f
    private var moveY = 0f
    private var attackCooldown = 0f
    private var attackPose = 0f
    private var invuln = 0f
    private var dashTime = 0f
    private var dashDirX = 1f
    private var dashDirY = 0f
    private var lightQueued = false
    private var heavyQueued = false
    private var dashQueued = false
    private var specialQueued = false

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

    init {
        spawnNextWave()
    }

    fun setMove(x: Float, y: Float) {
        val len = hypot(x, y)
        if (len > 1f) {
            moveX = x / len
            moveY = y / len
        } else {
            moveX = x
            moveY = y
        }
        if (abs(moveX) > 0.08f) facing = if (moveX >= 0f) 1 else -1
    }

    fun pressLight() { lightQueued = true }
    fun pressHeavy() { heavyQueued = true }
    fun pressDash() { dashQueued = true }
    fun pressSpecial() { specialQueued = true }

    fun update(rawDt: Float): GameSnapshot {
        val dt = rawDt.coerceIn(0f, 0.034f)
        if (stageComplete || playerDown) return snapshot()
        elapsed += dt
        attackCooldown = max(0f, attackCooldown - dt)
        attackPose = max(0f, attackPose - dt)
        invuln = max(0f, invuln - dt)
        comboTimer = max(0f, comboTimer - dt)
        cameraShake = max(0f, cameraShake - dt)
        if (comboTimer <= 0f) {
            combo = 0
            comboStep = 0
        }

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
        } else {
            spawnDelay = 0.72f
        }

        if (playerHp <= 0f) {
            playerHp = 0f
            playerDown = true
            effects += Fx(playerX, playerY - 20f, 0.8f, 0.8f, FxKind.KO)
        }
        return snapshot()
    }

    fun snapshot(): GameSnapshot = GameSnapshot(
        playerX = playerX,
        playerY = playerY,
        playerHp = playerHp,
        playerMaxHp = fighter.maxHp,
        energy = energy,
        facing = facing,
        attackPose = attackPose,
        invulnerable = invuln > 0f,
        enemies = enemies.map {
            EnemyView(it.id, it.kind, it.x, it.y, it.hp, it.maxHp, it.facing, it.hitFlash, it.label)
        },
        pickups = pickups.map { PickupView(it.id, it.x, it.y, it.health) },
        effects = effects.map { FxView(it.x, it.y, it.life, it.maxLife, it.kind) },
        wave = wave,
        totalWaves = stage.waves + 1,
        score = score,
        combo = combo,
        maxCombo = maxCombo,
        comboTimer = comboTimer,
        stageComplete = stageComplete,
        playerDown = playerDown,
        bossActive = enemies.any { it.kind == EnemyKind.BOSS },
        elapsed = elapsed,
    )

    fun shakeAmount(): Float = cameraShake

    private fun startDash() {
        val len = hypot(moveX, moveY)
        if (len > 0.12f) {
            dashDirX = moveX / len
            dashDirY = moveY / len
        } else {
            dashDirX = facing.toFloat()
            dashDirY = 0f
        }
        dashTime = 0.17f
        invuln = max(invuln, 0.34f)
        attackCooldown = max(attackCooldown, 0.12f)
    }

    private fun updatePlayerMovement(dt: Float) {
        if (dashTime > 0f) {
            dashTime -= dt
            val dashSpeed = fighter.moveSpeed * 3.4f
            playerX += dashDirX * dashSpeed * dt
            playerY += dashDirY * dashSpeed * 0.72f * dt
        } else if (attackPose <= 0.16f) {
            val speed = fighter.moveSpeed
            playerX += moveX * speed * dt
            playerY += moveY * speed * 0.72f * dt
        }
        playerX = playerX.coerceIn(42f, WORLD_W - 42f)
        playerY = playerY.coerceIn(FLOOR_TOP, FLOOR_BOTTOM)
    }

    private fun performLight() {
        if (attackCooldown > 0f || dashTime > 0f) return
        comboStep = if (comboTimer > 0f) (comboStep % 3) + 1 else 1
        attackCooldown = if (fighter.style == FighterStyle.SPEED) 0.19f else 0.24f
        attackPose = 0.16f + comboStep * 0.025f
        val damage = fighter.attack * when (comboStep) {
            1 -> 0.82f
            2 -> 0.94f
            else -> 1.22f
        }
        val range = if (comboStep == 3) 105f else 88f
        val hits = damageEnemies(range, 52f, damage, knock = 26f + comboStep * 8f, onlyFront = true)
        if (hits > 0) registerCombo(hits, if (comboStep == 3) 130 else 90)
    }

    private fun performHeavy() {
        if (attackCooldown > 0f || dashTime > 0f) return
        attackCooldown = if (fighter.style == FighterStyle.POWER) 0.42f else 0.50f
        attackPose = 0.34f
        val hits = damageEnemies(
            range = if (fighter.style == FighterStyle.POWER) 138f else 120f,
            laneRange = 68f,
            damage = fighter.attack * if (fighter.style == FighterStyle.POWER) 1.95f else 1.72f,
            knock = 92f,
            onlyFront = true,
            stun = 0.28f,
        )
        if (hits > 0) {
            effects += Fx(playerX + facing * 72f, playerY - 35f, 0.24f, 0.24f, FxKind.HEAVY)
            registerCombo(hits, 190)
            cameraShake = 0.16f
        }
    }

    private fun performSpecial() {
        if (energy < 100f || attackCooldown > 0f) return
        energy = 0f
        attackCooldown = 0.85f
        attackPose = 0.72f
        invuln = max(invuln, 0.95f)
        val radius = when (fighter.style) {
            FighterStyle.ENERGY -> 285f
            FighterStyle.SPEED -> 230f
            FighterStyle.POWER -> 255f
            FighterStyle.BALANCED -> 245f
        }
        var hits = 0
        enemies.toList().forEach { enemy ->
            val d = hypot(enemy.x - playerX, (enemy.y - playerY) * 1.35f)
            if (d <= radius) {
                val damage = fighter.attack * when (fighter.style) {
                    FighterStyle.ENERGY -> 3.15f
                    FighterStyle.POWER -> 2.9f
                    FighterStyle.SPEED -> 2.45f
                    FighterStyle.BALANCED -> 2.7f
                }
                hitEnemy(enemy, damage, 150f, 0.85f)
                hits++
            }
        }
        effects += Fx(playerX, playerY - 30f, 0.72f, 0.72f, FxKind.SPECIAL)
        if (hits > 0) registerCombo(hits, 300)
        cameraShake = 0.44f
    }

    private fun damageEnemies(
        range: Float,
        laneRange: Float,
        damage: Float,
        knock: Float,
        onlyFront: Boolean,
        stun: Float = 0.12f,
    ): Int {
        var hits = 0
        enemies.toList().forEach { enemy ->
            val dx = enemy.x - playerX
            val dy = abs(enemy.y - playerY)
            val front = !onlyFront || dx * facing >= -18f
            if (front && abs(dx) <= range && dy <= laneRange) {
                hitEnemy(enemy, damage, knock, stun)
                hits++
            }
        }
        return hits
    }

    private fun hitEnemy(enemy: Enemy, damage: Float, knock: Float, stun: Float) {
        val dealt = damage * (if (enemy.kind == EnemyKind.BOSS) 0.84f else 1f)
        enemy.hp -= dealt
        enemy.hitFlash = 0.12f
        enemy.stun = max(enemy.stun, stun)
        enemy.knockX += facing * knock
        enemy.knockY += (enemy.y - playerY).coerceIn(-1f, 1f) * 12f
        energy = min(100f, energy + if (enemy.kind == EnemyKind.BOSS) 7f else 11f)
        effects += Fx(enemy.x, enemy.y - 54f, 0.18f, 0.18f, FxKind.HIT)
        if (enemy.hp <= 0f) killEnemy(enemy)
    }

    private fun registerCombo(hits: Int, pointsPerHit: Int) {
        combo += hits
        maxCombo = max(maxCombo, combo)
        comboTimer = 2.15f
        score += pointsPerHit * hits * (1 + combo / 8)
    }

    private fun killEnemy(enemy: Enemy) {
        if (!enemies.remove(enemy)) return
        val bonus = when (enemy.kind) {
            EnemyKind.GRUNT -> 350
            EnemyKind.BRUISER -> 600
            EnemyKind.RANGED -> 520
            EnemyKind.BOSS -> 4200
        }
        score += bonus * max(1, combo / 3)
        energy = min(100f, energy + if (enemy.kind == EnemyKind.BOSS) 30f else 14f)
        effects += Fx(enemy.x, enemy.y - 28f, 0.64f, 0.64f, FxKind.KO)
        cameraShake = max(cameraShake, if (enemy.kind == EnemyKind.BOSS) 0.55f else 0.18f)

        if (enemy.kind == EnemyKind.BOSS) {
            stageComplete = true
            score += max(0, (180f - elapsed).toInt()) * 40
        } else {
            val roll = random.nextFloat()
            if (roll < 0.12f || (playerHp < fighter.maxHp * 0.35f && roll < 0.25f)) {
                pickups += Pickup(nextId++, enemy.x, enemy.y, health = true)
            } else if (roll < 0.30f) {
                pickups += Pickup(nextId++, enemy.x, enemy.y, health = false)
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        enemies.toList().forEach { enemy ->
            enemy.hitFlash = max(0f, enemy.hitFlash - dt)
            enemy.attackCooldown = max(0f, enemy.attackCooldown - dt)
            enemy.stun = max(0f, enemy.stun - dt)
            enemy.bossBurst = max(0f, enemy.bossBurst - dt)

            if (abs(enemy.knockX) > 1f || abs(enemy.knockY) > 1f) {
                enemy.x += enemy.knockX * dt
                enemy.y += enemy.knockY * dt
                enemy.knockX *= (1f - 7.5f * dt).coerceAtLeast(0f)
                enemy.knockY *= (1f - 7.5f * dt).coerceAtLeast(0f)
            }
            enemy.x = enemy.x.coerceIn(32f, WORLD_W - 32f)
            enemy.y = enemy.y.coerceIn(FLOOR_TOP, FLOOR_BOTTOM)
            if (enemy.stun > 0f) return@forEach

            val dx = playerX - enemy.x
            val dy = playerY - enemy.y
            val d = hypot(dx, dy * 1.3f)
            enemy.facing = if (dx >= 0f) 1 else -1

            val attackRange = when (enemy.kind) {
                EnemyKind.RANGED -> 250f
                EnemyKind.BOSS -> 92f
                EnemyKind.BRUISER -> 82f
                EnemyKind.GRUNT -> 70f
            }
            if (d > attackRange) {
                val len = hypot(dx, dy).coerceAtLeast(1f)
                var chase = enemy.speed
                if (enemy.kind == EnemyKind.RANGED && d < 190f) chase = -enemy.speed * 0.55f
                enemy.x += dx / len * chase * dt
                enemy.y += dy / len * chase * 0.72f * dt
            } else if (enemy.attackCooldown <= 0f) {
                enemyAttack(enemy, d)
            }

            if (enemy.kind == EnemyKind.BOSS && random.nextFloat() < dt * 0.12f && enemy.bossBurst <= 0f) {
                enemy.bossBurst = 2.8f
                enemy.stun = 0.20f
            }
        }
    }

    private fun enemyAttack(enemy: Enemy, distance: Float) {
        val baseCooldown = when (enemy.kind) {
            EnemyKind.GRUNT -> 1.15f
            EnemyKind.BRUISER -> 1.55f
            EnemyKind.RANGED -> 1.70f
            EnemyKind.BOSS -> 0.92f
        }
        enemy.attackCooldown = baseCooldown / (1f + 0.12f * (difficulty - 1).coerceAtLeast(0))

        val canHit = when (enemy.kind) {
            EnemyKind.RANGED -> distance <= 270f
            EnemyKind.BOSS -> distance <= 112f
            EnemyKind.BRUISER -> distance <= 96f
            EnemyKind.GRUNT -> distance <= 82f
        }
        if (!canHit || invuln > 0f) return

        var damage = enemy.damage * (1f + 0.18f * (difficulty - 1).coerceAtLeast(0))
        if (enemy.kind == EnemyKind.BOSS && enemy.bossBurst > 2.55f) damage *= 1.65f
        damage *= 100f / (100f + fighter.defense)
        playerHp -= damage
        invuln = 0.34f
        combo = 0
        comboTimer = 0f
        effects += Fx(playerX, playerY - 48f, 0.24f, 0.24f, FxKind.HURT)
        cameraShake = max(cameraShake, if (enemy.kind == EnemyKind.BOSS) 0.28f else 0.12f)
    }

    private fun updatePickups(dt: Float) {
        pickups.toList().forEach { pickup ->
            pickup.ttl -= dt
            if (pickup.ttl <= 0f) {
                pickups.remove(pickup)
                return@forEach
            }
            val d = hypot(pickup.x - playerX, pickup.y - playerY)
            if (d < 42f) {
                if (pickup.health) playerHp = min(fighter.maxHp, playerHp + fighter.maxHp * 0.22f)
                else energy = min(100f, energy + 32f)
                score += 180
                pickups.remove(pickup)
            }
        }
    }

    private fun updateEffects(dt: Float) {
        effects.forEach { it.life -= dt }
        effects.removeAll { it.life <= 0f }
    }

    private fun spawnNextWave() {
        if (wave < stage.waves) {
            wave++
            val count = 2 + wave + (difficulty - 1).coerceAtLeast(0)
            repeat(count) { index ->
                val kind = when {
                    wave >= 3 && index == count - 1 -> EnemyKind.BRUISER
                    wave >= 2 && index % 3 == 2 -> EnemyKind.RANGED
                    else -> EnemyKind.GRUNT
                }
                spawnEnemy(kind, index)
            }
        } else if (!bossSpawned) {
            bossSpawned = true
            wave = stage.waves + 1
            spawnEnemy(EnemyKind.BOSS, 0)
        }
    }

    private fun spawnEnemy(kind: EnemyKind, index: Int) {
        val x = 730f + random.nextFloat() * 210f
        val y = FLOOR_TOP + 35f + random.nextFloat() * (FLOOR_BOTTOM - FLOOR_TOP - 55f)
        val difficultyScale = 1f + 0.25f * (difficulty - 1).coerceAtLeast(0)
        val waveScale = 1f + 0.10f * max(0, wave - 1)
        val (hp, speed, damage, label) = when (kind) {
            EnemyKind.GRUNT -> Quad(54f, 104f, 13f, "Sbire ${index + 1}")
            EnemyKind.BRUISER -> Quad(112f, 72f, 24f, "Brute")
            EnemyKind.RANGED -> Quad(68f, 82f, 18f, "Tireur")
            EnemyKind.BOSS -> Quad(470f, 92f, 31f, stage.bossCardName)
        }
        val maxHp = hp * difficultyScale * waveScale
        enemies += Enemy(
            id = nextId++,
            kind = kind,
            x = x,
            y = y,
            hp = maxHp,
            maxHp = maxHp,
            speed = speed * (1f + 0.05f * (difficulty - 1).coerceAtLeast(0)),
            damage = damage,
            label = label,
        )
    }

    private data class Quad(val hp: Float, val speed: Float, val damage: Float, val label: String)
}
