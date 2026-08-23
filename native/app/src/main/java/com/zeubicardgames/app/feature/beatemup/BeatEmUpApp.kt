package com.zeubicardgames.app.feature.beatemup

import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeubicardgames.app.core.designsystem.AssetImage
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class BeatScreen { HOME, CAMPAIGN, GAME }

private val Gold = Color(0xFFD9C9A0)
private val Cyan = Color(0xFF3CE0D5)
private val Ink = Color(0xFF071013)
private val Purple = Color(0xFF8E3FC8)

@Composable
fun BeatEmUpApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val progress = remember { RiftProgress(context) }
    val stages = remember { riftStages() }
    val fighter = remember { FighterSpec() }
    var screen by remember { mutableStateOf(BeatScreen.HOME) }
    var stageIndex by remember { mutableIntStateOf(0) }
    var difficulty by remember { mutableIntStateOf(1) }
    var runKey by remember { mutableIntStateOf(0) }

    Surface(Modifier.fillMaxSize(), color = Ink) {
        when (screen) {
            BeatScreen.HOME -> RiftHome(
                onCampaign = { screen = BeatScreen.CAMPAIGN },
                onQuickFight = { stageIndex = (progress.unlocked() - 1).coerceIn(0, stages.lastIndex); runKey++; screen = BeatScreen.GAME },
            )
            BeatScreen.CAMPAIGN -> CampaignScreen(
                stages = stages,
                progress = progress,
                selected = stageIndex,
                difficulty = difficulty,
                onSelect = { stageIndex = it },
                onDifficulty = { difficulty = it },
                onBack = { screen = BeatScreen.HOME },
                onFight = { runKey++; screen = BeatScreen.GAME },
            )
            BeatScreen.GAME -> GameScreen(
                key = runKey,
                fighter = fighter,
                stage = stages[stageIndex],
                stageIndex = stageIndex,
                difficulty = difficulty,
                progress = progress,
                onMenu = { screen = BeatScreen.HOME },
                onReplay = { runKey++ },
                onNext = {
                    if (stageIndex < stages.lastIndex) stageIndex++
                    runKey++
                },
            )
        }
    }
}

private fun riftStages() = listOf(
    StageSpec("ruelles", "LES RUELLES DE KUROKAWA", "Une nuit calme qui ne l’est déjà plus.", 2, "RoobANBU"),
    StageSpec("rift", "LE RIFT RÉVEILLÉ", "Le chakra ancien traverse les ruelles.", 3, "RoobANBU"),
    StageSpec("clans", "LES CLANS DE L’OMBRE", "Les élites sortent de leurs repaires.", 3, "RoobANBU"),
    StageSpec("destin", "DESTIN ENTRELACÉ", "Au cœur du village, le Rift prend forme.", 4, "Roobkatsuki", finalMission = true),
)

private class RiftProgress(context: Context) {
    private val p = context.getSharedPreferences("rift_brawl_ch1", Context.MODE_PRIVATE)
    fun unlocked() = p.getInt("unlocked", 1).coerceIn(1, 4)
    fun stars(id: String) = p.getInt("stars_$id", 0).coerceIn(0, 3)
    fun score(id: String) = p.getInt("score_$id", 0)
    fun record(stage: StageSpec, index: Int, score: Int, stars: Int) {
        p.edit()
            .putInt("unlocked", max(unlocked(), (index + 2).coerceAtMost(4)))
            .putInt("score_${stage.id}", max(score, score(stage.id)))
            .putInt("stars_${stage.id}", max(stars, stars(stage.id)))
            .apply()
    }
}

@Composable
private fun RiftHome(onCampaign: () -> Unit, onQuickFight: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink)) {
        AssetImage("rift/menu_rift_brawl.webp", "Rift Brawl", Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .20f)))
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 38.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text("ZEUBICARDGAMES", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("RIFT BRAWL", color = Color(0xFFF0D5A0), fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("CHAPITRE I — L’OMBRE DES NINJAS", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiftMenuButton("CAMPAGNE", Cyan, onCampaign)
                RiftMenuButton("COMBAT RAPIDE", Color(0xFF8F6C35), onQuickFight)
            }
        }
    }
}

@Composable
private fun RiftMenuButton(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.height(48.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xDD0B1518))
            .border(1.dp, accent.copy(alpha = .9f), RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Gold, fontWeight = FontWeight.Black, fontSize = 11.sp) }
}

@Composable
private fun CampaignScreen(
    stages: List<StageSpec>,
    progress: RiftProgress,
    selected: Int,
    difficulty: Int,
    onSelect: (Int) -> Unit,
    onDifficulty: (Int) -> Unit,
    onBack: () -> Unit,
    onFight: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AssetImage("rift/bg_kurokawa.webp", null, Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color(0xA9070C0E)))
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", Modifier.clickable(onClick = onBack).padding(8.dp), color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Column {
                    Text("CHAPITRE I", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("L’OMBRE DES NINJAS", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                stages.forEachIndexed { index, stage ->
                    val locked = index >= progress.unlocked()
                    val active = index == selected
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xD20B1215))
                            .border(if (active) 2.dp else 1.dp, if (active) Cyan else Gold.copy(alpha = .38f), RoundedCornerShape(6.dp))
                            .clickable(enabled = !locked) { onSelect(index) }
                            .padding(12.dp),
                    ) {
                        Column(Modifier.align(Alignment.BottomStart)) {
                            Text("${index + 1}".padStart(2, '0'), color = if (locked) Color.Gray else Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text(if (locked) "VERROUILLÉ" else stage.name, color = if (locked) Color.Gray else Gold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            if (!locked) {
                                Text(stage.subtitle, color = Color.White.copy(alpha = .65f), fontSize = 8.sp, lineHeight = 10.sp)
                                Spacer(Modifier.height(7.dp))
                                Text("${"★".repeat(progress.stars(stage.id))}${"☆".repeat(3 - progress.stars(stage.id))}", color = Color(0xFFE6B75D), fontSize = 13.sp)
                                Text("BEST ${progress.score(stage.id)}", color = Color.White.copy(alpha = .55f), fontSize = 7.sp)
                            }
                        }
                        if (!locked && stage.finalMission) Text("RIFT", Modifier.align(Alignment.TopEnd), color = Purple, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1 to "NORMAL", 2 to "HARD", 3 to "OMBRE").forEach { (value, label) ->
                        FilterChip(selected = difficulty == value, onClick = { onDifficulty(value) }, label = { Text(label, fontSize = 8.sp) })
                    }
                }
                Button(onClick = onFight, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E615F))) {
                    Text("LANCER LA MISSION", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun GameScreen(
    key: Int,
    fighter: FighterSpec,
    stage: StageSpec,
    stageIndex: Int,
    difficulty: Int,
    progress: RiftProgress,
    onMenu: () -> Unit,
    onReplay: () -> Unit,
    onNext: () -> Unit,
) {
    val atlas = rememberRiftAtlas()
    val engine = remember(key, stage.id, difficulty) { BeatEmUpEngine(fighter, stage, difficulty) }
    var snapshot by remember(engine) { mutableStateOf(engine.snapshot()) }
    var recorded by remember(engine) { mutableStateOf(false) }

    LaunchedEffect(engine) {
        var last = 0L
        while (isActive) withFrameNanos { now ->
            if (last == 0L) last = now
            val dt = (now - last) / 1_000_000_000f
            last = now
            snapshot = engine.update(dt)
        }
    }
    LaunchedEffect(snapshot.stageComplete) {
        if (snapshot.stageComplete && !recorded) {
            progress.record(stage, stageIndex, snapshot.score, stars(snapshot, difficulty)); recorded = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        AssetImage("rift/bg_kurokawa.webp", null, Modifier.fillMaxSize(), ContentScale.Crop)
        if (stageIndex >= 1) Box(Modifier.fillMaxSize().background(Purple.copy(alpha = .04f * stageIndex)))
        Arena(snapshot, stage, atlas, engine.shakeAmount())
        RiftHud(snapshot, stage, atlas)
        RiftControls(
            modifier = Modifier.align(Alignment.BottomCenter),
            atlas = atlas,
            evolutionReady = snapshot.evolution >= 100f && snapshot.form < 2,
            onMove = engine::setMove,
            light = engine::pressLight,
            heavy = engine::pressHeavy,
            dash = engine::pressDash,
            special = engine::pressSpecial,
            evolve = engine::pressEvolution,
        )
        if (snapshot.stageComplete || snapshot.playerDown) RiftResult(snapshot, difficulty, stageIndex < 3, onReplay, onNext, onMenu)
    }
}

private fun stars(s: GameSnapshot, difficulty: Int): Int {
    if (!s.stageComplete) return 0
    var value = 1
    if (s.playerHp >= s.playerMaxHp * .45f) value++
    if (s.elapsed <= 155f || difficulty >= 2) value++
    return value.coerceIn(1, 3)
}

@Composable
private fun Arena(s: GameSnapshot, stage: StageSpec, atlas: RiftAtlas?, shake: Float) = Canvas(Modifier.fillMaxSize()) {
    val ready = atlas ?: return@Canvas
    val sx = size.width / BeatEmUpEngine.WORLD_W
    val sy = size.height / BeatEmUpEngine.WORLD_H
    val ox = if (shake > 0) sin(s.elapsed * 73f) * shake * 12 else 0f
    val oy = if (shake > 0) cos(s.elapsed * 61f) * shake * 6 else 0f
    fun p(x: Float, y: Float) = Offset(x * sx + ox, y * sy + oy)

    drawRect(Color(0x38000000), p(0f, BeatEmUpEngine.FLOOR_TOP), Size(size.width, size.height - BeatEmUpEngine.FLOOR_TOP * sy))
    s.pickups.forEach { q -> drawCircle(if (q.health) Color(0xFF63E0A0) else Cyan, 12f * sx, p(q.x, q.y - 14f)) }

    data class Actor(val depth: Float, val enemy: EnemyView?)
    val actors = buildList {
        s.enemies.forEach { add(Actor(it.y, it)) }
        add(Actor(s.playerY, null))
    }.sortedBy { it.depth }

    actors.forEach { actor ->
        val enemy = actor.enemy
        if (enemy == null) {
            val prefix = when (s.form) { 1 -> "rubinobi"; 2 -> "roobkage"; else -> "zaim" }
            val frame = when {
                s.attacking -> "${prefix}_attack${if (s.attackStep % 2 == 0) 2 else 1}"
                s.moving -> "${prefix}_walk1"
                else -> "${prefix}_idle"
            }
            drawOval(Color.Black.copy(alpha = .35f), Offset(p(s.playerX, s.playerY).x - 28f * sx, p(s.playerX, s.playerY).y - 7f * sy), Size(56f * sx, 13f * sy))
            drawRiftSprite(ready, frame, p(s.playerX, s.playerY), (116f + s.form * 5f) * sy, s.facing, if (s.invulnerable) .72f else 1f)
        } else {
            val prefix = when (enemy.kind) {
                EnemyKind.GRUNT -> "sinanbu"
                EnemyKind.ELITE -> "roobandu"
                EnemyKind.BOSS -> if (stage.finalMission) "roobkatsuki" else "roobandu"
            }
            val frame = when {
                enemy.attacking && ready.frames.containsKey("${prefix}_attack1") -> "${prefix}_attack1"
                else -> "${prefix}_idle"
            }
            val h = when (enemy.kind) { EnemyKind.GRUNT -> 106f; EnemyKind.ELITE -> 116f; EnemyKind.BOSS -> 145f }
            val pos = p(enemy.x, enemy.y)
            drawOval(Color.Black.copy(alpha = .35f), Offset(pos.x - 27f * sx, pos.y - 6f * sy), Size(54f * sx, 12f * sy))
            drawRiftSprite(ready, frame, pos, h * sy, enemy.facing, if (enemy.hitFlash > 0f) .60f else 1f)
            val barWidth = (if (enemy.kind == EnemyKind.BOSS) 94f else 55f) * sx
            val top = pos.y - (h + 12f) * sy
            drawRect(Color(0xD0101218), Offset(pos.x - barWidth / 2, top), Size(barWidth, 5f * sy))
            drawRect(if (enemy.kind == EnemyKind.BOSS) Color(0xFFC23A42) else Color(0xFF9C2731), Offset(pos.x - barWidth / 2, top), Size(barWidth * (enemy.hp / enemy.maxHp).coerceIn(0f, 1f), 5f * sy))
        }
    }

    s.effects.forEach { fx ->
        val t = 1f - (fx.life / fx.maxLife).coerceIn(0f, 1f)
        val frame = when (fx.kind) {
            FxKind.SPECIAL -> "fx_spiral"
            FxKind.EVOLUTION -> "fx_gold"
            FxKind.HEAVY -> "fx_slash"
            FxKind.HURT, FxKind.HIT -> "fx_slash"
            FxKind.KO -> "fx_rift"
        }
        val height = when (fx.kind) { FxKind.SPECIAL, FxKind.EVOLUTION -> 185f; else -> 95f }
        drawRiftSprite(ready, frame, p(fx.x, fx.y + 40f), height * sy * (1f + .2f * t), 1, 1f - t * .75f)
    }
}

@Composable
private fun RiftHud(s: GameSnapshot, stage: StageSpec, atlas: RiftAtlas?) {
    val formName = when (s.form) { 1 -> "RUBINOBI"; 2 -> "ROOBKAGE"; else -> "ZAIM SINJA" }
    Box(Modifier.fillMaxSize().padding(12.dp)) {
        Column(Modifier.align(Alignment.TopStart).width(330.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RiftFrameImage(atlas, when (s.form) { 1 -> "rubinobi_idle"; 2 -> "roobkage_idle"; else -> "zaim_idle" }, Modifier.size(46.dp))
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(formName, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Meter(s.playerHp / s.playerMaxHp, Color(0xFFB92E35), 250.dp, 8.dp)
                    Meter(s.energy / 100f, Cyan, 250.dp, 5.dp)
                    Meter(s.evolution / 100f, Color(0xFFD59B3E), 250.dp, 4.dp)
                }
            }
        }
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CHAPITRE I — L’OMBRE DES NINJAS", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(if (s.bossActive) stage.bossName.uppercase() else "VAGUE ${s.wave}/${s.totalWaves}", color = if (s.bossActive) Color(0xFFDB4B55) else Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
            if (s.combo >= 2) Text("${s.combo} COMBO", color = Color(0xFFE7C47B), fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
            Text("SCORE", color = Gold.copy(alpha = .7f), fontSize = 7.sp)
            Text(s.score.toString().padStart(6, '0'), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun Meter(value: Float, color: Color, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(width).height(height).background(Color(0xD20A1216), RoundedCornerShape(99.dp)).border(1.dp, Gold.copy(alpha = .3f), RoundedCornerShape(99.dp))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f, 1f)).background(color, RoundedCornerShape(99.dp)))
    }
}

@Composable
private fun RiftControls(
    modifier: Modifier,
    atlas: RiftAtlas?,
    evolutionReady: Boolean,
    onMove: (Float, Float) -> Unit,
    light: () -> Unit,
    heavy: () -> Unit,
    dash: () -> Unit,
    special: () -> Unit,
    evolve: () -> Unit,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        RiftStick(onMove)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            RiftAction(atlas, "hud_btn_dash", 58.dp, dash)
            RiftAction(atlas, "hud_btn_light", 68.dp, light)
            RiftAction(atlas, "hud_btn_heavy", 68.dp, heavy)
            RiftAction(atlas, "hud_btn_special", 64.dp, special)
            RiftAction(atlas, "hud_btn_evolution", 70.dp, evolve, enabled = evolutionReady)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RiftStick(move: (Float, Float) -> Unit) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier.size(116.dp).onSizeChanged { sizePx = it }.pointerInteropFilter { event ->
            val cx = sizePx.width / 2f; val cy = sizePx.height / 2f; val radius = min(sizePx.width, sizePx.height) * .36f
            val dx = event.x - cx; val dy = event.y - cy; val len = kotlin.math.hypot(dx, dy); val k = if (len > radius && len > 0f) radius / len else 1f
            val x = dx * k; val y = dy * k
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> { knob = Offset(x, y); move(if (radius > 0) x / radius else 0f, if (radius > 0) y / radius else 0f) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { knob = Offset.Zero; move(0f, 0f) }
            }
            true
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * .42f
            drawCircle(Color(0x7720373A), radius, center)
            drawCircle(Gold.copy(alpha = .42f), radius, center, style = Stroke(2f))
            val inputRadius = min(sizePx.width, sizePx.height) * .36f
            drawCircle(Cyan.copy(alpha = .78f), radius * .30f, Offset(center.x + if (inputRadius > 0f) knob.x / inputRadius * radius else 0f, center.y + if (inputRadius > 0f) knob.y / inputRadius * radius else 0f))
        }
    }
}

@Composable
private fun RiftAction(atlas: RiftAtlas?, frame: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier.size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RiftFrameImage(atlas, frame, Modifier.fillMaxSize())
        if (!enabled) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .58f), CircleShape))
    }
}

@Composable
private fun RiftResult(s: GameSnapshot, difficulty: Int, hasNext: Boolean, replay: () -> Unit, next: () -> Unit, menu: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xA8000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(390.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xF20A1215)).border(1.dp, Gold.copy(alpha = .65f), RoundedCornerShape(8.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val win = s.stageComplete
            Text(if (win) "MISSION TERMINÉE" else "K.O.", color = if (win) Gold else Color(0xFFE05259), fontSize = 28.sp, fontWeight = FontWeight.Black)
            if (win) Text("${"★".repeat(stars(s, difficulty))}${"☆".repeat(3 - stars(s, difficulty))}", color = Color(0xFFE6B75D), fontSize = 22.sp)
            Text("SCORE ${s.score}  •  COMBO MAX ${s.maxCombo}", color = Color.White.copy(alpha = .8f), fontSize = 10.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = menu) { Text("MENU") }
                Button(onClick = replay) { Text("REJOUER") }
                if (win && hasNext) Button(onClick = next) { Text("MISSION SUIVANTE") }
            }
        }
    }
}
