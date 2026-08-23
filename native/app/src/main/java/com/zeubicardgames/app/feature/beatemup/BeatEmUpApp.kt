package com.zeubicardgames.app.feature.beatemup

import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeubicardgames.app.core.data.CatalogLoader
import com.zeubicardgames.app.core.designsystem.AssetImage
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.ExtensionDefinition
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class BeatScreen { HOME, FIGHTER, STAGE, GALLERY, GAME }

@Composable
fun BeatEmUpApp() {
    val context = LocalContext.current
    val (cards, extensions) = remember {
        runCatching { CatalogLoader(context).load() }
            .getOrElse { emptyList<CardDefinition>() to emptyList<ExtensionDefinition>() }
    }
    val fighters = remember { fighters() }
    val stages = remember(extensions) { stages(extensions) }
    val progress = remember { BeatProgress(context) }
    var screen by remember { mutableStateOf(BeatScreen.HOME) }
    var fighter by remember { mutableStateOf(fighters.first()) }
    var stage by remember { mutableStateOf(stages.first()) }
    var difficulty by remember { mutableIntStateOf(1) }
    var runKey by remember { mutableIntStateOf(0) }

    Surface(Modifier.fillMaxSize(), color = Color(0xFF07090E)) {
        when (screen) {
            BeatScreen.HOME -> Home(cards.size, { screen = BeatScreen.FIGHTER }, { screen = BeatScreen.GALLERY })
            BeatScreen.FIGHTER -> FighterSelect(fighters, cards, fighter, { fighter = it }, { screen = BeatScreen.HOME }) { screen = BeatScreen.STAGE }
            BeatScreen.STAGE -> StageSelect(stages, progress, stage, difficulty, { stage = it }, { difficulty = it }, { screen = BeatScreen.FIGHTER }) {
                runKey++
                screen = BeatScreen.GAME
            }
            BeatScreen.GALLERY -> Gallery(cards) { screen = BeatScreen.HOME }
            BeatScreen.GAME -> Game(
                runKey, fighter, stage, stages.indexOf(stage).coerceAtLeast(0), cards, difficulty, progress,
                onMenu = { screen = BeatScreen.HOME },
                onReplay = { runKey++ },
                onNext = {
                    val i = stages.indexOf(stage)
                    if (i in 0 until stages.lastIndex) stage = stages[i + 1]
                    runKey++
                },
            )
        }
    }
}

private fun fighters() = listOf(
    FighterSpec("lara", "LARA RECRUE", "Lara Recrue", "cod", FighterStyle.BALANCED, 260f, 178f, 33f, 18f, "TIR DE SUPPRESSION", 0xFFF9B17AL),
    FighterSpec("zaim", "ZAIM SINJA", "Zaim Sinja", "ninja", FighterStyle.SPEED, 220f, 214f, 29f, 11f, "OMBRE ÉCLAIR", 0xFF9A7BFFL),
    FighterSpec("bafo", "BAFOLANTERN", "Bafolantern", "emerald", FighterStyle.ENERGY, 245f, 166f, 31f, 15f, "VOLONTÉ ÉMERAUDE", 0xFF4BE39AL),
    FighterSpec("rayanjin", "SUPER RAYANJIN", "Super Rayanjin", "dbz", FighterStyle.POWER, 300f, 154f, 39f, 24f, "RAYAN BLAST", 0xFFFF7B3DL),
)

private fun stages(ext: List<ExtensionDefinition>): List<StageSpec> {
    val bosses = mapOf("ninja" to "Roobkage", "emerald" to "Baforallax", "cod" to "Lara Prestige Master", "dbz" to "Trika Genkidama")
    val source = if (ext.isNotEmpty()) ext else listOf(
        ExtensionDefinition("ninja", "Roobkaruto", "L’Ombre des Ninjas", 0xFF9A7BFFL, "boosters/ninja.webp", 20),
        ExtensionDefinition("emerald", "Green Bafo", "Volonté Émeraude", 0xFF4BE39AL, "boosters/emerald.webp", 19),
        ExtensionDefinition("cod", "Call of Trikuss", "Opérations tactiques", 0xFFF9B17AL, "boosters/cod.webp", 30),
        ExtensionDefinition("dbz", "Trika Ball Z", "Puissance sans limite", 0xFFFF7B3DL, "boosters/dbz.webp", 30),
    )
    return source.mapIndexed { i, e -> StageSpec("stage_${e.id}", e.name, e.subtitle, e.id, e.boosterPath, bosses[e.id] ?: "Boss", e.accent, if (i == source.lastIndex) 4 else 3) }
}

private class BeatProgress(context: Context) {
    private val p = context.getSharedPreferences("zeubi_brawl_v1", Context.MODE_PRIVATE)
    fun unlocked() = p.getInt("unlocked", 1).coerceAtLeast(1)
    fun stars(id: String) = p.getInt("stars_$id", 0)
    fun score(id: String) = p.getInt("score_$id", 0)
    fun record(id: String, index: Int, score: Int, stars: Int) {
        p.edit().putInt("unlocked", max(unlocked(), index + 2)).putInt("score_$id", max(score, score(id))).putInt("stars_$id", max(stars, stars(id))).apply()
    }
}

@Composable
private fun Home(count: Int, play: () -> Unit, gallery: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AssetImage("boosters/dbz.webp", null, Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color(0xC907090E)))
        Column(Modifier.align(Alignment.CenterStart).padding(48.dp)) {
            Text("ZEUBI", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("BRAWL", color = Color(0xFFFFB347), fontSize = 62.sp, fontWeight = FontWeight.Black)
            Text("Le beat’em up de l’univers ZeubiCardGames", color = Color(0xFFD5DAE4))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuButton("JOUER", Color(0xFFFFA94D), play)
                MenuButton("GALERIE • $count CARTES", Color(0xFF252B36), gallery)
            }
        }
    }
}

@Composable
private fun MenuButton(text: String, color: Color, click: () -> Unit) = Box(
    Modifier.height(56.dp).clip(RoundedCornerShape(14.dp)).background(color).clickable(onClick = click).padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center,
) { Text(text, color = Color.White, fontWeight = FontWeight.Black) }

@Composable
private fun Header(title: String, back: () -> Unit) = Row(verticalAlignment = Alignment.CenterVertically) {
    Text("←", Modifier.clickable(onClick = back).padding(8.dp), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
}

@Composable
private fun FighterSelect(fs: List<FighterSpec>, cards: List<CardDefinition>, selected: FighterSpec, pick: (FighterSpec) -> Unit, back: () -> Unit, next: () -> Unit) {
    val map = remember(cards) { cards.associateBy { it.name.lowercase() } }
    Column(Modifier.fillMaxSize().background(Color(0xFF090C12)).padding(16.dp)) {
        Header("CHOISIS TON COMBATTANT", back)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            fs.forEach { f ->
                val path = map[f.cardName.lowercase()]?.variants?.firstOrNull()?.fullPath
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).border(if (f == selected) 3.dp else 1.dp, if (f == selected) Color(f.accentArgb) else Color(0xFF343B48), RoundedCornerShape(16.dp)).clickable { pick(f) }) {
                    AssetImage(path, f.name, Modifier.fillMaxSize(), ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color(0x66000000)))
                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                        Text(f.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(f.specialName, color = Color(f.accentArgb), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = next, colors = ButtonDefaults.buttonColors(containerColor = Color(selected.accentArgb))) { Text("CHOISIR LE MONDE →", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun StageSelect(ss: List<StageSpec>, progress: BeatProgress, selected: StageSpec, difficulty: Int, pick: (StageSpec) -> Unit, setDifficulty: (Int) -> Unit, back: () -> Unit, fight: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF090C12)).padding(16.dp)) {
        Header("CAMPAGNE", back)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ss.forEachIndexed { i, s ->
                val locked = i >= progress.unlocked()
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).border(if (s == selected) 3.dp else 1.dp, if (s == selected) Color(s.accentArgb) else Color(0xFF343B48), RoundedCornerShape(16.dp)).clickable(enabled = !locked) { pick(s) }) {
                    AssetImage(s.boosterPath, s.name, Modifier.fillMaxSize(), ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(if (locked) Color(0xD005070A) else Color(0x77000000)))
                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                        Text(if (locked) "🔒 VERROUILLÉ" else s.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        if (!locked) {
                            Text("BOSS • ${s.bossCardName}", color = Color(s.accentArgb), fontSize = 9.sp)
                            Text("${"★".repeat(progress.stars(s.id))}${"☆".repeat(3-progress.stars(s.id))}  BEST ${progress.score(s.id)}", color = Color.White.copy(alpha=.75f), fontSize=8.sp)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top=8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to "NORMAL", 2 to "HARD", 3 to "CHAOS").forEach { (d,n) -> FilterChip(selected = difficulty==d, onClick={setDifficulty(d)}, label={Text(n,fontSize=9.sp)}) }
            }
            Button(onClick=fight, colors=ButtonDefaults.buttonColors(containerColor=Color(selected.accentArgb))) { Text("FIGHT !", fontWeight=FontWeight.Black) }
        }
    }
}

@Composable
private fun Gallery(cards: List<CardDefinition>, back: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF090C12)).padding(14.dp)) {
        Header("GALERIE • ${cards.size} CARTES", back)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(cards.chunked(6)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { c ->
                        Column(Modifier.weight(1f).height(145.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF171C25)).padding(4.dp)) {
                            AssetImage(c.variants.firstOrNull()?.thumbPath ?: c.variants.firstOrNull()?.fullPath, c.name, Modifier.weight(1f).fillMaxWidth(), ContentScale.Crop)
                            Text(c.name, color=Color.White, fontSize=8.sp, maxLines=1, fontWeight=FontWeight.Bold)
                            Text("${c.setId.uppercase()} • ${c.rarity}", color=Color(0xFF8E98A9), fontSize=6.sp)
                        }
                    }
                    repeat(6-row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Game(key: Int, fighter: FighterSpec, stage: StageSpec, stageIndex: Int, cards: List<CardDefinition>, difficulty: Int, progress: BeatProgress, onMenu: () -> Unit, onReplay: () -> Unit, onNext: () -> Unit) {
    val engine = remember(key, fighter.id, stage.id, difficulty) { BeatEmUpEngine(fighter, stage, difficulty) }
    var snap by remember(engine) { mutableStateOf(engine.snapshot()) }
    var recorded by remember(engine) { mutableStateOf(false) }
    val map = remember(cards) { cards.associateBy { it.name.lowercase() } }
    val fighterPath = map[fighter.cardName.lowercase()]?.variants?.firstOrNull()?.thumbPath
    val bossPath = map[stage.bossCardName.lowercase()]?.variants?.firstOrNull()?.thumbPath

    LaunchedEffect(engine) {
        var last=0L
        while (isActive) withFrameNanos { now ->
            if (last==0L) last=now
            val dt=(now-last)/1_000_000_000f
            last=now
            snap=engine.update(dt)
        }
    }
    LaunchedEffect(snap.stageComplete) {
        if (snap.stageComplete && !recorded) {
            progress.record(stage.id, stageIndex, snap.score, stars(snap,difficulty)); recorded=true
        }
    }

    Box(Modifier.fillMaxSize()) {
        AssetImage(stage.boosterPath, null, Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color(0xB00A0D13)))
        Arena(snap, fighter, stage, engine.shakeAmount())
        Hud(snap, fighter, stage, fighterPath, bossPath)
        Controls(Modifier.align(Alignment.BottomCenter), engine::setMove, engine::pressLight, engine::pressHeavy, engine::pressDash, engine::pressSpecial)
        if (snap.stageComplete || snap.playerDown) Result(snap, difficulty, onReplay, onNext, onMenu)
    }
}

private fun stars(s: GameSnapshot, difficulty: Int): Int {
    if (!s.stageComplete) return 0
    var result = 1
    if (s.playerHp >= s.playerMaxHp * .45f) result++
    if (s.elapsed <= 150f || difficulty >= 2) result++
    return result.coerceIn(1, 3)
}

@Composable
private fun Arena(s: GameSnapshot, f: FighterSpec, stage: StageSpec, shake: Float) = Canvas(Modifier.fillMaxSize()) {
    val sx=size.width/BeatEmUpEngine.WORLD_W; val sy=size.height/BeatEmUpEngine.WORLD_H
    val ox=if(shake>0) sin(s.elapsed*73f)*shake*12 else 0f; val oy=if(shake>0) cos(s.elapsed*61f)*shake*6 else 0f
    fun p(x:Float,y:Float)=Offset(x*sx+ox,y*sy+oy)
    drawRect(Color(0x44000000),p(0f,BeatEmUpEngine.FLOOR_TOP),Size(size.width,size.height-BeatEmUpEngine.FLOOR_TOP*sy))
    s.pickups.forEach { q -> drawCircle(if(q.health) Color(0xFF50E58A) else Color(0xFFFFC247),14*sx,p(q.x,q.y-20)) }
    val actors=buildList<Pair<Float,EnemyView?>> { s.enemies.forEach { add(it.y to it) }; add(s.playerY to null) }.sortedBy{it.first}
    actors.forEach { (_,e) ->
        if(e==null) drawGuy(p(s.playerX,s.playerY),sx,Color(f.accentArgb),s.facing,s.attackPose,s.invulnerable,false)
        else {
            val c=when(e.kind){EnemyKind.GRUNT->Color(0xFFE8505B);EnemyKind.BRUISER->Color(0xFFFF8748);EnemyKind.RANGED->Color(0xFFD35CFF);EnemyKind.BOSS->Color(stage.accentArgb)}
            val mul=if(e.kind==EnemyKind.BOSS)1.35f else if(e.kind==EnemyKind.BRUISER)1.15f else 1f
            drawGuy(p(e.x,e.y),sx*mul,if(e.hitFlash>0)Color.White else c,e.facing,0f,false,e.kind==EnemyKind.BOSS)
            val w=(if(e.kind==EnemyKind.BOSS)90 else 56)*sx; val top=p(e.x,e.y-if(e.kind==EnemyKind.BOSS)112 else 86)
            drawRect(Color(0xCC151921),Offset(top.x-w/2,top.y),Size(w,6*sx)); drawRect(c,Offset(top.x-w/2,top.y),Size(w*(e.hp/e.maxHp).coerceIn(0f,1f),6*sx))
        }
    }
    s.effects.forEach { fx ->
        val t=1-(fx.life/fx.maxLife).coerceIn(0f,1f); val c=p(fx.x,fx.y)
        val col=when(fx.kind){FxKind.SPECIAL->Color(f.accentArgb);FxKind.HURT->Color.Red;FxKind.HEAVY->Color(0xFFFFD06A);else->Color.White}
        drawCircle(col.copy(alpha=(1-t)*.55f), (12+90*t)*sx,c,style=Stroke(width=4*sx))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGuy(c:Offset, sc:Float, color:Color, facing:Int, attack:Float, flash:Boolean, boss:Boolean) {
    val body=if(flash) color.copy(alpha=.55f) else color
    drawOval(Color.Black.copy(alpha=.4f),Offset(c.x-32*sc,c.y-7*sc),Size(64*sc,16*sc))
    val top=c.y-67*sc; val w=if(boss)44f else 36f
    drawRoundRect(body,Offset(c.x-w/2*sc,top),Size(w*sc,50*sc),androidx.compose.ui.geometry.CornerRadius(9*sc))
    drawCircle(Color(0xFFFFD2B5),(if(boss)15 else 13)*sc,Offset(c.x,top-12*sc))
    drawLine(Color(0xFF202532),Offset(c.x-10*sc,c.y-20*sc),Offset(c.x-14*sc,c.y),8*sc,StrokeCap.Round)
    drawLine(Color(0xFF202532),Offset(c.x+10*sc,c.y-20*sc),Offset(c.x+14*sc,c.y),8*sc,StrokeCap.Round)
    val punch=if(attack>0)38f else 20f
    drawLine(body,Offset(c.x+facing*11*sc,c.y-50*sc),Offset(c.x+facing*punch*sc,c.y-42*sc),9*sc,StrokeCap.Round)
}

@Composable
private fun Hud(s: GameSnapshot, f:FighterSpec, stage:StageSpec, fighterPath:String?, bossPath:String?) {
    Row(Modifier.fillMaxWidth().padding(start=12.dp,end=12.dp,top=9.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.Top) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            AssetImage(fighterPath,f.name,Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)),ContentScale.Crop); Spacer(Modifier.width(7.dp))
            Column { Text(f.name,color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Black); Meter(s.playerHp/s.playerMaxHp,Color(0xFF4FE08A),205.dp); Meter(s.energy/100f,Color(f.accentArgb),205.dp,5.dp) }
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) { Text(stage.name.uppercase(),color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Black); Text(if(s.bossActive)"BOSS" else "VAGUE ${s.wave}/${s.totalWaves}",color=Color(stage.accentArgb),fontSize=11.sp,fontWeight=FontWeight.Black); Text("SCORE ${s.score}",color=Color.White,fontSize=8.sp); if(s.combo>=2) Text("${s.combo} HITS",color=Color(0xFFFFC247),fontSize=16.sp,fontWeight=FontWeight.Black) }
        if(s.bossActive) Row(verticalAlignment=Alignment.CenterVertically) { val b=s.enemies.firstOrNull{it.kind==EnemyKind.BOSS}; Column(horizontalAlignment=Alignment.End){Text(stage.bossCardName.uppercase(),color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Black);Meter(if(b==null)0f else b.hp/b.maxHp,Color(stage.accentArgb),160.dp)};Spacer(Modifier.width(7.dp));AssetImage(bossPath,stage.bossCardName,Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)),ContentScale.Crop) }
    }
}

@Composable
private fun Meter(v:Float,c:Color,w:androidx.compose.ui.unit.Dp,h:androidx.compose.ui.unit.Dp=8.dp)=Box(Modifier.width(w).height(h).background(Color(0xCC262C37),RoundedCornerShape(99.dp))){Box(Modifier.fillMaxHeight().fillMaxWidth(v.coerceIn(0f,1f)).background(c,RoundedCornerShape(99.dp)))}

@Composable
private fun Controls(mod:Modifier, move:(Float,Float)->Unit, light:()->Unit, heavy:()->Unit, dash:()->Unit, special:()->Unit)=Row(mod.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom){Stick(move);Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.Bottom){Action("↯","DASH",Color(0xFF586174),54.dp,dash);Action("B","LOURD",Color(0xFFFF815F),62.dp,heavy);Action("A","FRAPPE",Color(0xFF6B7BFF),68.dp,light);Action("S","SPÉ",Color(0xFFFFC247),60.dp,special)}}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Stick(move:(Float,Float)->Unit){var knob by remember{mutableStateOf(Offset.Zero)};var sz by remember{mutableStateOf(IntSize.Zero)};Box(Modifier.size(124.dp).onSizeChanged{sz=it}.pointerInteropFilter{e->val cx=sz.width/2f;val cy=sz.height/2f;val r=min(sz.width,sz.height)*.36f;val dx=e.x-cx;val dy=e.y-cy;val len=kotlin.math.hypot(dx,dy);val k=if(len>r&&len>0)r/len else 1f;val x=dx*k;val y=dy*k;when(e.actionMasked){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{knob=Offset(x,y);move(if(r>0)x/r else 0f,if(r>0)y/r else 0f)};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{knob=Offset.Zero;move(0f,0f)}};true},contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize()){val r=size.minDimension*.42f;drawCircle(Color(0x552A3140),r,center);drawCircle(Color.White.copy(alpha=.18f),r,center,style=Stroke(2f));val ir=min(sz.width,sz.height)*.36f;drawCircle(Color(0xCCFFFFFF),r*.35f,Offset(center.x+if(ir>0)knob.x/ir*r else 0f,center.y+if(ir>0)knob.y/ir*r else 0f))}}}

@Composable
private fun Action(t:String,sub:String,c:Color,s:androidx.compose.ui.unit.Dp,click:()->Unit)=Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(s).background(c,CircleShape).border(2.dp,Color.White.copy(alpha=.5f),CircleShape).clickable(onClick=click),contentAlignment=Alignment.Center){Text(t,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Black)};Text(sub,color=Color.White.copy(alpha=.7f),fontSize=6.sp,fontWeight=FontWeight.Bold)}

@Composable
private fun Result(s:GameSnapshot,d:Int,replay:()->Unit,next:()->Unit,menu:()->Unit){Box(Modifier.fillMaxSize().background(Color(0x99000000)),contentAlignment=Alignment.Center){Card(colors=CardDefaults.cardColors(containerColor=Color(0xF31A1F29))){Column(Modifier.width(350.dp).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){val win=s.stageComplete;Text(if(win)"VICTOIRE" else "K.O.",color=if(win)Color(0xFFFFC247)else Color(0xFFFF6D73),fontSize=32.sp,fontWeight=FontWeight.Black);if(win)Text("${"★".repeat(stars(s,d))}${"☆".repeat(3-stars(s,d))}",color=Color(0xFFFFC247),fontSize=24.sp);Text("SCORE ${s.score}  •  MAX COMBO ${s.maxCombo}",color=Color.White,fontSize=12.sp);Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){Button(onClick=replay){Text("REJOUER")};if(win)Button(onClick=next){Text("SUIVANT")};OutlinedButton(onClick=menu){Text("MENU")}}}}}}
