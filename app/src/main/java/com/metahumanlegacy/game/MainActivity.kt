package com.metahumanlegacy.game

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MetahumanLegacyApp(applicationContext) }
    }
}

private val Coal = Color(0xFF0B0D10)
private val Panel = Color(0xFF161A20)
private val Ivory = Color(0xFFF3EFE5)
private val Gold = Color(0xFFE7D7A5)
private val Muted = Color(0xFFAAB1BA)
private val Danger = Color(0xFFD98D84)

@Composable
fun MetahumanLegacyApp(context: Context) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = Coal, surface = Panel, primary = Gold, onBackground = Ivory, onSurface = Ivory)
    ) {
        var campaign by remember { mutableStateOf(loadCampaign(context)) }
        var screen by remember { mutableStateOf(if (campaign == null) "HOME" else "DESTIN") }
        var hall by remember { mutableStateOf(loadHall(context)) }
        var pendingOutcome by remember { mutableStateOf<String?>(null) }
        var showRestartDialog by remember { mutableStateOf(false) }

        fun startFreshCampaign(seed: Long = System.currentTimeMillis()) {
            campaign = GameEngine.newCampaign(seed)
            saveCampaign(context, campaign!!)
            pendingOutcome = null
            screen = "DESTIN"
        }

        Surface(Modifier.fillMaxSize(), color = Coal) {
            Box {
                SkylineBackdrop(campaign?.scope ?: Scope.STREET)
                when {
                    campaign == null -> HomeScreen(
                        hallCount = hall.size,
                        onNew = { startFreshCampaign() },
                        onSeeded = { startFreshCampaign(20490413L) },
                        onHall = { screen = "HALL" }
                    )
                    campaign!!.finished -> FinalScreen(
                        c = campaign!!,
                        onArchive = {
                            val entry = "${campaign!!.name}|${GameEngine.legacyTitle(campaign!!)}|${GameEngine.legacyScore(campaign!!)}|${campaign!!.scope.label}"
                            hall = (listOf(entry) + hall).distinct().take(30)
                            saveHall(context, hall)
                            clearCampaign(context)
                            campaign = null
                            pendingOutcome = null
                            screen = "HOME"
                        },
                        onRestart = { showRestartDialog = true }
                    )
                    screen == "HALL" -> HallScreen(hall) { screen = if (campaign == null) "HOME" else "DESTIN" }
                    else -> CareerShell(
                        c = campaign!!,
                        screen = screen,
                        pendingOutcome = pendingOutcome,
                        onScreen = { screen = it },
                        onRestart = { showRestartDialog = true },
                        onContinue = { pendingOutcome = null },
                        onChoice = { choice ->
                            val current = campaign!!
                            val resolution = GameEngine.resolve(current, GameEngine.event(current), choice)
                            campaign = resolution.campaign
                            pendingOutcome = resolution.outcome
                            saveCampaign(context, resolution.campaign)
                        }
                    )
                }

                if (showRestartDialog) {
                    AlertDialog(
                        onDismissRequest = { showRestartDialog = false },
                        title = { Text("RECOMMENCER ?") },
                        text = { Text("La destinée actuelle sera abandonnée. Une nouvelle vie, une nouvelle identité et une nouvelle suite d'événements seront générées.") },
                        confirmButton = {
                            Button(onClick = {
                                clearCampaign(context)
                                showRestartDialog = false
                                startFreshCampaign()
                            }) { Text("RECOMMENCER") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRestartDialog = false }) { Text("ANNULER") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkylineBackdrop(scope: Scope) {
    Canvas(Modifier.fillMaxSize()) {
        val horizon = size.height * .72f
        val step = size.width / 10f
        for (i in 0..10) {
            val height = (50 + ((i * 37 + scope.ordinal * 29) % 180)).dp.toPx()
            drawRect(Color(0x141F2A35), topLeft = Offset(i * step, horizon - height), size = androidx.compose.ui.geometry.Size(step - 3f, height))
        }
        val p = Path().apply {
            moveTo(0f, horizon)
            lineTo(size.width, horizon - scope.ordinal * 18.dp.toPx())
        }
        drawPath(p, Color(0x224F6575))
    }
}

@Composable
private fun HomeScreen(hallCount: Int, onNew: () -> Unit, onSeeded: () -> Unit, onHall: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("METAHUMAN", color = Muted, letterSpacing = 4.sp, fontSize = 13.sp)
        Text("LEGACY", color = Ivory, fontWeight = FontWeight.Black, fontSize = 52.sp, lineHeight = 48.sp)
        Spacer(Modifier.height(10.dp))
        Text("SIMULATEUR DE DESTINÉE", color = Gold, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text("Tu ne choisis pas seulement qui gagnera. Tu choisis qui tu deviendras, ce que le monde retiendra et ce que tes décisions auront détruit ou sauvé des années plus tard.", color = Muted, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onNew, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("NOUVELLE DESTINÉE ALÉATOIRE") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onSeeded, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("DESTINÉE DÉTERMINISTE") }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onHall, modifier = Modifier.fillMaxWidth()) { Text("HALL OF LEGACIES · $hallCount") }
    }
}

@Composable
private fun CareerShell(
    c: Campaign,
    screen: String,
    pendingOutcome: String?,
    onScreen: (String) -> Unit,
    onRestart: () -> Unit,
    onContinue: () -> Unit,
    onChoice: (Choice) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.alias.uppercase(), fontWeight = FontWeight.Black, fontSize = 23.sp)
                Text("${c.age} ans · ${c.scope.label} · ${c.moralLabel}", color = Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("P ${c.prestige}", color = Gold, fontWeight = FontWeight.Bold)
                TextButton(onClick = onRestart, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text("RECOMMENCER", fontSize = 10.sp, color = Muted)
                }
            }
        }
        HorizontalDivider(color = Color(0x223A4652))
        Box(Modifier.weight(1f)) {
            when (screen) {
                "PERSONNAGE" -> CharacterScreen(c)
                "MONDE" -> WorldScreen(c)
                "LIENS" -> LinksScreen(c)
                "CHRONIQUE" -> TimelineScreen(c)
                else -> if (pendingOutcome != null) OutcomeScreen(c, pendingOutcome, onContinue) else DestinyScreen(c, onChoice)
            }
        }
        NavigationBar(containerColor = Color(0xF2161A20)) {
            listOf("DESTIN", "PERSONNAGE", "MONDE", "LIENS", "CHRONIQUE").forEach { item ->
                NavigationBarItem(selected = screen == item, onClick = { onScreen(item) }, icon = { Text(item.take(1), fontWeight = FontWeight.Black) }, label = { Text(item.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 9.sp) })
            }
        }
    }
}

@Composable
private fun OutcomeScreen(c: Campaign, outcome: String, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("CONSÉQUENCE", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text("LE MONDE RÉPOND", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xF220252C)), shape = RoundedCornerShape(10.dp)) {
            Text(outcome, modifier = Modifier.padding(18.dp), color = Ivory, fontSize = 16.sp, lineHeight = 24.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("Opinion ${signed(c.opinion)} · Peur ${c.fear} · Prestige ${c.prestige} · Exposition ${c.identityExposure}%", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("CONTINUER") }
        Spacer(Modifier.height(8.dp))
        Text("Cette réaction est liée à la situation, à ton choix et à la réputation construite jusque-là.", color = Color(0xFF707781), fontSize = 11.sp)
    }
}

private fun signed(v: Int) = if (v >= 0) "+$v" else "$v"

@Composable
private fun DestinyScreen(c: Campaign, onChoice: (Choice) -> Unit) {
    val event = remember(c.seed, c.turn, c.opinion, c.fear, c.control, c.identityExposure) { GameEngine.event(c) }
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        Text("${event.category} · ÉVÉNEMENT ${c.turn + 1}/120", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(event.title.uppercase(), fontSize = 30.sp, fontWeight = FontWeight.Black, lineHeight = 31.sp)
        Spacer(Modifier.height(14.dp))
        Text(event.text, color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(22.dp))
        event.choices.forEachIndexed { index, choice ->
            ElevatedButton(
                onClick = { onChoice(choice) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = if (choice.risk >= 8) Color(0xFF242027) else Color(0xFF20252C), contentColor = Ivory),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(choice.label.uppercase(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(riskLabel(choice.risk), color = if (choice.risk >= 6) Danger else Gold, fontSize = 10.sp)
                }
            }
            if (index < event.choices.lastIndex) Spacer(Modifier.height(1.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Les conséquences cachées ne sont pas affichées. La même seed et les mêmes choix reproduisent le même résultat, mais ta réputation change la manière dont le monde réagit.", color = Color(0xFF707781), fontSize = 11.sp)
    }
}

private fun riskLabel(risk: Int) = when {
    risk >= 8 -> "EXTRÊME"
    risk >= 6 -> "TRÈS RISQUÉ"
    risk >= 4 -> "RISQUÉ"
    risk >= 2 -> "INCERTAIN"
    else -> "FAIBLE RISQUE"
}

@Composable
private fun CharacterScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("IDENTITÉ")
        InfoCard("${c.name} / ${c.alias}", "${c.origin} · ${c.powerFamily}\nLimite connue : ${c.weakness}")
        Spacer(Modifier.height(12.dp))
        SectionTitle("AXES")
        StatLine("Moralité", c.moralLabel, (c.morality + 100) / 2)
        StatLine("Opinion", if (c.opinion >= 0) "${c.opinion}% favorable" else "${-c.opinion}% hostile", (c.opinion + 100) / 2)
        StatLine("Peur", qualitative(c.fear), c.fear)
        StatLine("Puissance", qualitative(c.power), c.power)
        StatLine("Maîtrise", qualitative(c.control), c.control)
        StatLine("Exposition identité", qualitative(c.identityExposure), c.identityExposure)
        StatLine("Santé", qualitative(c.health), c.health)
        Spacer(Modifier.height(12.dp))
        InfoCard("Prestige ${c.prestige}", "Influence ${c.influence} · Portée actuelle : ${c.scope.label}\nVictimes civiles attribuées : ${c.civilianCasualties}")
    }
}

@Composable
private fun WorldScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("MONDE VIVANT")
        InfoCard("Contexte : ${c.modifier}", "Les surhumains agissent indépendamment. À chaque décision, les institutions, médias, factions et adversaires gagnent ou perdent de l'influence.")
        Spacer(Modifier.height(12.dp))
        InfoCard("Échelle ${c.scope.label}", when (c.scope) {
            Scope.STREET -> "Ton nom circule surtout autour de quelques rues. Les enjeux restent personnels et immédiats."
            Scope.DISTRICT -> "Des quartiers voisins commencent à anticiper tes interventions."
            Scope.CITY -> "Médias, police et factions métropolitaines ajustent leurs plans à ton existence."
            Scope.REGION -> "Ton influence dépasse la ville et crée des réactions politiques durables."
            Scope.COUNTRY -> "Tes choix pèsent désormais sur une nation entière."
            Scope.WORLD -> "Chaque intervention peut modifier l'équilibre mondial."
        })
        Spacer(Modifier.height(12.dp))
        SectionTitle("SIGNAUX")
        StatLine("Confiance surhumaine", if (c.opinion >= 0) "En hausse" else "Fragile", (c.opinion + 100) / 2)
        StatLine("Contrôle gouvernemental", if (c.fear > 55) "Élevé" else "Modéré", c.fear)
        StatLine("Tension médiatique", if (c.prestige > 300) "Nationale" else "Locale", minOf(100, c.prestige / 5))
    }
}

@Composable
private fun LinksScreen(c: Campaign) {
    val trust = (50 + c.opinion / 2 - c.fear / 4).coerceIn(0, 100)
    val resentment = (c.fear + c.civilianCasualties * 4).coerceIn(0, 100)
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("LIENS PERSISTANTS")
        InfoCard("Mara Vale · journaliste", "Confiance ${trust}/100 · Respect ${(40 + c.prestige / 20).coerceAtMost(100)}/100\nElle se souvient de la manière dont tu as traité les civils et les médias.")
        Spacer(Modifier.height(10.dp))
        InfoCard("Soren Kess · rival", "Rancune $resentment/100 · Peur ${(c.fear + 15).coerceAtMost(100)}/100\nUne humiliation ancienne peut revenir bien plus tard.")
        Spacer(Modifier.height(10.dp))
        InfoCard("Un protégé potentiel", "Ton style moral actuel influence déjà la personne qu'il pourrait devenir.")
    }
}

@Composable
private fun TimelineScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("CHRONIQUE")
        if (c.timeline.isEmpty()) Text("Aucune décision historique pour l'instant.", color = Muted)
        c.timeline.reversed().forEach { item ->
            Text(item, color = if (item.startsWith("↳")) Muted else Ivory, modifier = Modifier.padding(vertical = 7.dp), fontSize = if (item.startsWith("↳")) 12.sp else 14.sp, lineHeight = 19.sp)
            HorizontalDivider(color = Color(0x183A4652))
        }
    }
}

@Composable
private fun FinalScreen(c: Campaign, onArchive: () -> Unit, onRestart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        Text("LEGACY", color = Gold, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
        Text(c.name.uppercase(), fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(GameEngine.legacyTitle(c), color = Gold, fontSize = 20.sp)
        Spacer(Modifier.height(20.dp))
        InfoCard("18 → ${c.age} ans", "Orientation : ${c.moralLabel}\nPortée : ${c.scope.label}\nPrestige : ${c.prestige}\nOpinion : ${c.opinion}\nPeur : ${c.fear}\nPuissance : ${c.power}\nLegacy Score : ${GameEngine.legacyScore(c)}")
        Spacer(Modifier.height(18.dp))
        Text("${c.alias} n'est pas devenu une légende parce qu'une barre était pleine. Sa trace vient des choix répétés, des compromis acceptés et de l'échelle à laquelle le monde a fini par devoir compter avec lui.", color = Muted, lineHeight = 22.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onArchive, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("ARCHIVER CETTE VIE") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("RECOMMENCER") }
    }
}

@Composable
private fun HallScreen(hall: List<String>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("HALL OF LEGACIES", fontWeight = FontWeight.Black, fontSize = 24.sp)
            TextButton(onClick = onBack) { Text("RETOUR") }
        }
        Spacer(Modifier.height(14.dp))
        if (hall.isEmpty()) Text("Aucune destinée achevée.", color = Muted)
        hall.forEach { raw ->
            val p = raw.split('|')
            InfoCard(p.getOrElse(0) { "Inconnu" }, "${p.getOrElse(1) { "Legacy" }} · ${p.getOrElse(3) { "Rue" }}\nLegacy Score ${p.getOrElse(2) { "0" }}")
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 8.dp))

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xE6161A20)), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text(body, color = Muted, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, percent: Int) {
    Column(Modifier.padding(vertical = 7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Muted)
            Text(value, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(progress = { percent.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Gold, trackColor = Color(0xFF272C33))
    }
}

private fun qualitative(v: Int) = when {
    v >= 85 -> "Exceptionnel"
    v >= 65 -> "Élevé"
    v >= 40 -> "Modéré"
    v >= 20 -> "Faible"
    else -> "Critique"
}

private fun saveCampaign(context: Context, c: Campaign) {
    val t = c.timeline.joinToString("~") { it.replace("|", " ").replace("~", " ") }
    val raw = listOf(c.seed, c.name, c.alias, c.origin, c.powerFamily, c.weakness, c.modifier, c.turn, c.morality, c.prestige, c.opinion, c.fear, c.power, c.control, c.influence, c.health, c.civilianCasualties, c.identityExposure, t).joinToString("|")
    context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().putString("campaign", raw).apply()
}

private fun loadCampaign(context: Context): Campaign? {
    val raw = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).getString("campaign", null) ?: return null
    return runCatching {
        val p = raw.split('|')
        Campaign(p[0].toLong(), p[1], p[2], p[3], p[4], p[5], p[6], p[7].toInt(), p[8].toInt(), p[9].toInt(), p[10].toInt(), p[11].toInt(), p[12].toInt(), p[13].toInt(), p[14].toInt(), p[15].toInt(), p[16].toInt(), p[17].toInt(), p.getOrElse(18) { "" }.split('~').filter { it.isNotBlank() })
    }.getOrNull()
}

private fun clearCampaign(context: Context) = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().remove("campaign").apply()
private fun loadHall(context: Context): List<String> = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).getString("hall", "")!!.split(";;").filter { it.isNotBlank() }
private fun saveHall(context: Context, hall: List<String>) = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().putString("hall", hall.joinToString(";;")).apply()
