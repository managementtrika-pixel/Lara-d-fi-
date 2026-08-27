package com.metahumanlegacy.game

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
    MaterialTheme(colorScheme = darkColorScheme(background = Coal, surface = Panel, primary = Gold, onBackground = Ivory, onSurface = Ivory)) {
        var campaign by remember { mutableStateOf(loadCampaign(context)) }
        var screen by remember { mutableStateOf(if (campaign == null) "HOME" else "DESTIN") }
        var hall by remember { mutableStateOf(loadHall(context)) }
        var outcome by remember { mutableStateOf<String?>(null) }
        var draft by remember { mutableStateOf(GameEngine.randomBlueprint(System.currentTimeMillis())) }

        fun start(blueprint: CharacterBlueprint) {
            val seed = System.currentTimeMillis()
            campaign = GameEngine.newCampaign(seed, blueprint)
            saveCampaign(context, campaign!!)
            outcome = null
            screen = "DESTIN"
        }

        fun restart() {
            clearCampaign(context)
            campaign = null
            outcome = null
            draft = GameEngine.randomBlueprint(System.currentTimeMillis() xor 0x77L)
            screen = "CREATE"
        }

        Surface(Modifier.fillMaxSize(), color = Coal) {
            Box {
                SkylineBackdrop(campaign?.scope ?: Scope.STREET)
                when {
                    campaign == null && screen == "CREATE" -> CreateCharacterScreen(
                        initial = draft,
                        onDraft = { draft = it },
                        onBack = { screen = "HOME" },
                        onStart = { start(it) }
                    )
                    campaign == null && screen == "HALL" -> HallScreen(hall) { screen = "HOME" }
                    campaign == null -> HomeScreen(
                        hallCount = hall.size,
                        onCreate = { draft = GameEngine.randomBlueprint(System.currentTimeMillis()); screen = "CREATE" },
                        onRandom = { start(GameEngine.randomBlueprint(System.currentTimeMillis())) },
                        onHall = { screen = "HALL" }
                    )
                    campaign!!.finished -> FinalScreen(
                        c = campaign!!,
                        onArchive = {
                            val entry = "${campaign!!.name}|${GameEngine.legacyTitle(campaign!!)}|${GameEngine.legacyScore(campaign!!)}|${campaign!!.scope.label}"
                            hall = (listOf(entry) + hall).distinct().take(40)
                            saveHall(context, hall)
                            clearCampaign(context)
                            campaign = null
                            outcome = null
                            screen = "HOME"
                        },
                        onRestart = { restart() }
                    )
                    screen == "HALL" -> HallScreen(hall) { screen = "DESTIN" }
                    else -> CareerShell(
                        c = campaign!!,
                        screen = screen,
                        outcome = outcome,
                        onScreen = { screen = it },
                        onContinue = { outcome = null },
                        onChoice = { event, choice ->
                            val resolution = GameEngine.resolve(campaign!!, event, choice)
                            campaign = resolution.campaign
                            outcome = resolution.outcome
                            saveCampaign(context, campaign!!)
                        },
                        onRestart = { restart() }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(hallCount: Int, onCreate: () -> Unit, onRandom: () -> Unit, onHall: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        Text("METAHUMAN", color = Muted, letterSpacing = 4.sp, fontSize = 13.sp)
        Text("LEGACY", color = Ivory, fontWeight = FontWeight.Black, fontSize = 52.sp, lineHeight = 48.sp)
        Spacer(Modifier.height(10.dp))
        Text("SIMULATEUR DE DESTINÉE", color = Gold, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        Text("Crée un être, choisis ses limites et découvre ce que ses décisions deviennent plusieurs années plus tard. Le monde se souvient désormais de ta méthode, pas seulement de tes points.", color = Muted, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(58.dp)) { Text("CRÉER MON PERSONNAGE") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("TOUT ALÉATOIRE") }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onHall, modifier = Modifier.fillMaxWidth()) { Text("HALL OF LEGACIES · $hallCount") }
    }
}

@Composable
private fun CreateCharacterScreen(initial: CharacterBlueprint, onDraft: (CharacterBlueprint) -> Unit, onBack: () -> Unit, onStart: (CharacterBlueprint) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var d by remember(initial) { mutableStateOf(initial) }
    fun update(next: CharacterBlueprint) { d = next; onDraft(next) }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CRÉATION · ${step + 1}/3", color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            TextButton(onClick = {
                val random = GameEngine.randomBlueprint(System.currentTimeMillis())
                update(random)
            }) { Text("ALÉATOIRE") }
        }
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Gold, trackColor = Color(0xFF272C33))
        Spacer(Modifier.height(14.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> {
                    CreationTitle("IDENTITÉ CIVILE")
                    OutlinedTextField(value = d.firstName, onValueChange = { update(d.copy(firstName = it)) }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = d.lastName, onValueChange = { update(d.copy(lastName = it)) }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = d.alias, onValueChange = { update(d.copy(alias = it)) }, label = { Text("Alias / nom de terrain") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OptionStrip("PRONOM", GameEngine.pronouns, d.pronouns) { update(d.copy(pronouns = it)) }
                    OptionStrip("VILLE D'ORIGINE", GameEngine.cities, d.city) { update(d.copy(city = it)) }
                    OptionStrip("QUARTIER", GameEngine.districts, d.district) { update(d.copy(district = it)) }
                }
                1 -> {
                    CreationTitle("D'OÙ VIENT TON POUVOIR ?")
                    OptionStrip("CONTEXTE SOCIAL", GameEngine.socialBackgrounds, d.socialBackground) { update(d.copy(socialBackground = it)) }
                    OptionStrip("ORIGINE", GameEngine.origins, d.origin) { update(d.copy(origin = it)) }
                    OptionStrip("POUVOIR PRINCIPAL", GameEngine.powers, d.powerFamily) { update(d.copy(powerFamily = it)) }
                    OptionStrip("FAIBLESSE", GameEngine.weaknesses, d.weakness) { update(d.copy(weakness = it)) }
                    InfoCard("Une vraie limite", "La faiblesse choisie n'est pas décorative : les événements liés au pouvoir, aux rivaux et à l'identité pourront l'exploiter au cours de la carrière.")
                }
                else -> {
                    CreationTitle("QUI VEUX-TU DEVENIR ?")
                    OptionStrip("MOTIVATION", GameEngine.motivations, d.motivation) { update(d.copy(motivation = it)) }
                    OptionStrip("APPARENCE DE TERRAIN", GameEngine.visualStyles, d.visualStyle) { update(d.copy(visualStyle = it)) }
                    Spacer(Modifier.height(10.dp))
                    InfoCard("${d.fullName} / ${d.alias.ifBlank { "Sans alias" }}", "${d.pronouns} · ${d.city}, ${d.district}\n${d.socialBackground}\n${d.origin} → ${d.powerFamily}\nFaiblesse : ${d.weakness}\nMotivation : ${d.motivation}\nStyle : ${d.visualStyle}")
                    Spacer(Modifier.height(12.dp))
                    Text("Ces choix modifient les statistiques initiales et débloquent certaines décisions contextuelles. Une origine scientifique n'ouvre pas les mêmes solutions qu'un programme militaire ; un visage découvert ne vit pas l'identité secrète de la même façon.", color = Muted, lineHeight = 21.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { if (step == 0) onBack() else step-- }, modifier = Modifier.weight(1f).height(52.dp)) { Text(if (step == 0) "RETOUR" else "PRÉCÉDENT") }
            Button(onClick = { if (step < 2) step++ else onStart(d) }, modifier = Modifier.weight(1f).height(52.dp)) { Text(if (step < 2) "SUIVANT" else "COMMENCER") }
        }
    }
}

@Composable
private fun CreationTitle(text: String) {
    Text(text, fontSize = 27.sp, fontWeight = FontWeight.Black, lineHeight = 29.sp)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun OptionStrip(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(14.dp))
    Text(title, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun CareerShell(c: Campaign, screen: String, outcome: String?, onScreen: (String) -> Unit, onContinue: () -> Unit, onChoice: (EventNode, Choice) -> Unit, onRestart: () -> Unit) {
    var confirmRestart by remember { mutableStateOf(false) }
    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Recommencer cette destinée ?") },
            text = { Text("La carrière actuelle sera abandonnée. Le Hall of Legacies déjà enregistré sera conservé.") },
            confirmButton = { TextButton(onClick = { confirmRestart = false; onRestart() }) { Text("RECOMMENCER") } },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("ANNULER") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.alias.uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("${c.age} ans · ${c.city} · ${c.scope.label} · ${c.moralLabel}", color = Muted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("P ${c.prestige}", color = Gold, fontWeight = FontWeight.Bold)
                TextButton(onClick = { confirmRestart = true }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("RECOMMENCER", fontSize = 9.sp) }
            }
        }
        HorizontalDivider(color = Color(0x223A4652))
        Box(Modifier.weight(1f)) {
            when (screen) {
                "PERSONNAGE" -> CharacterScreen(c)
                "MONDE" -> WorldScreen(c)
                "LIENS" -> LinksScreen(c)
                "CHRONIQUE" -> TimelineScreen(c)
                else -> DestinyScreen(c, outcome, onContinue, onChoice)
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
private fun DestinyScreen(c: Campaign, outcome: String?, onContinue: () -> Unit, onChoice: (EventNode, Choice) -> Unit) {
    if (outcome != null) {
        Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Text("LE MONDE RÉPOND", color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text(outcome, fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(18.dp))
            InfoCard("CONSÉQUENCE PERSISTANTE", "Des relations, des drapeaux narratifs ou un fil à long terme peuvent avoir été modifiés. Certaines conséquences ne seront visibles que plusieurs événements plus tard.")
            Spacer(Modifier.height(22.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("CONTINUER LA DESTINÉE") }
        }
        return
    }

    val event = remember(c.seed, c.turn, c.threads, c.lastCategory, c.lastApproach) { GameEngine.event(c) }
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${event.category} · ${c.turn + 1}/140", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("ENJEU ${event.stakes}/3", color = if (event.stakes >= 3) Danger else Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (event.threadStage > 0) {
            Spacer(Modifier.height(7.dp))
            Text("FIL NARRATIF REPRIS · CHAPITRE ${event.threadStage}", color = Danger, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text(event.title.uppercase(), fontSize = 29.sp, fontWeight = FontWeight.Black, lineHeight = 31.sp)
        Spacer(Modifier.height(14.dp))
        Text(event.text, color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(20.dp))
        event.choices.forEach { choice ->
            ElevatedButton(
                onClick = { onChoice(event, choice) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = if (choice.stakes >= 3) Color(0xFF292127) else Color(0xFF20252C), contentColor = Ivory),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(choice.label.uppercase(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), lineHeight = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(riskLabel(choice.risk), color = if (choice.risk >= 7) Danger else Gold, fontSize = 9.sp)
                        if (choice.stakes >= 3) Text("MAJEUR", color = Danger, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Les chiffres ne racontent qu'une partie du résultat. Les fils narratifs peuvent revenir des années plus tard avec de nouvelles options dépendant de tes anciennes décisions.", color = Color(0xFF707781), fontSize = 11.sp, lineHeight = 16.sp)
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
        InfoCard("${c.name} / ${c.alias}", "${c.pronouns} · ${c.city}, ${c.district}\n${c.socialBackground}\nStyle : ${c.visualStyle}")
        Spacer(Modifier.height(10.dp))
        SectionTitle("ORIGINE")
        InfoCard(c.origin, "Pouvoir : ${c.powerFamily}\nFaiblesse : ${c.weakness}\nMotivation fondatrice : ${c.motivation}")
        Spacer(Modifier.height(12.dp))
        SectionTitle("AXES")
        StatLine("Moralité", c.moralLabel, (c.morality + 100) / 2)
        StatLine("Opinion", if (c.opinion >= 0) "${c.opinion}% favorable" else "${-c.opinion}% hostile", (c.opinion + 100) / 2)
        StatLine("Peur", qualitative(c.fear), c.fear)
        StatLine("Puissance", qualitative(c.power), c.power)
        StatLine("Maîtrise", qualitative(c.control), c.control)
        StatLine("Exposition identité", qualitative(c.identityExposure), c.identityExposure)
        StatLine("Santé", qualitative(c.health), c.health)
        Spacer(Modifier.height(10.dp))
        InfoCard("Prestige ${c.prestige}", "Influence ${c.influence} · Portée : ${c.scope.label}\nVictimes civiles attribuées : ${c.civilianCasualties}\nFils narratifs actifs : ${c.threads.size}")
    }
}

@Composable
private fun WorldScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("MONDE VIVANT")
        InfoCard("${c.city} · ${c.modifier}", "Tu as commencé dans ${c.district}. Tes choix locaux peuvent maintenant créer des précédents qui influencent factions, institutions et adversaires à une autre échelle.")
        Spacer(Modifier.height(12.dp))
        InfoCard("Échelle ${c.scope.label}", when (c.scope) {
            Scope.STREET -> "Ton nom circule dans quelques rues. Les conséquences restent proches, donc personnelles."
            Scope.DISTRICT -> "Plusieurs quartiers commencent à anticiper tes méthodes."
            Scope.CITY -> "Médias, police et factions métropolitaines ajustent leurs plans à ton existence."
            Scope.REGION -> "Tes décisions créent des réactions politiques durables au-delà de la ville."
            Scope.COUNTRY -> "Tes choix peuvent devenir des précédents nationaux."
            Scope.WORLD -> "Chaque doctrine que tu imposes peut désormais modifier l'équilibre mondial."
        })
        Spacer(Modifier.height(12.dp))
        SectionTitle("RELATIONS DE POUVOIR")
        StatLine("Gouvernement", signedLabel(c.governmentStanding), (c.governmentStanding + 100) / 2)
        StatLine("Factions", signedLabel(c.factionStanding), (c.factionStanding + 100) / 2)
        StatLine("Médias", signedLabel(c.mediaStanding), (c.mediaStanding + 100) / 2)
        Spacer(Modifier.height(12.dp))
        SectionTitle("FILS ACTIFS")
        if (c.threads.isEmpty()) Text("Aucun arc à long terme n'est actuellement ouvert.", color = Muted)
        c.threads.forEach { thread -> InfoCard(thread.id, "Chapitre atteint : ${thread.stage}\nDernière méthode : ${thread.lastApproach}\nIntensité : ${thread.intensity}/3") ; Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun LinksScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("LIENS PERSISTANTS")
        StatLine("Famille", bondLabel(c.familyBond), c.familyBond)
        StatLine("Rival", signedLabel(c.rivalStanding), (c.rivalStanding + 100) / 2)
        Spacer(Modifier.height(12.dp))
        InfoCard("Famille", when {
            c.familyBond >= 75 -> "Ils te font confiance, ce qui rend chaque futur mensonge plus lourd."
            c.familyBond <= 25 -> "La prochaine crise familiale pourrait devenir une rupture définitive."
            else -> "Le lien tient, mais ta double vie continue d'en fixer les limites."
        })
        Spacer(Modifier.height(10.dp))
        InfoCard("Rival", when {
            c.rivalStanding >= 35 -> "Le respect rend possible une vraie coopération — et une trahison beaucoup plus douloureuse."
            c.rivalStanding <= -35 -> "La rivalité est personnelle. Il étudie désormais tes habitudes, pas seulement ta puissance."
            else -> "Ni allié ni ennemi juré : votre prochaine rencontre peut encore faire basculer la relation."
        })
    }
}

@Composable
private fun TimelineScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("CHRONIQUE")
        if (c.timeline.isEmpty()) Text("Aucune décision historique pour l'instant.", color = Muted)
        c.timeline.reversed().forEach { item ->
            Text(item, color = if (item.startsWith("↳")) Muted else Ivory, modifier = Modifier.padding(vertical = 7.dp), fontSize = if (item.startsWith("↳")) 12.sp else 14.sp, lineHeight = 18.sp)
            HorizontalDivider(color = Color(0x183A4652))
        }
    }
}

@Composable
private fun FinalScreen(c: Campaign, onArchive: () -> Unit, onRestart: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Nouvelle destinée ?") },
        text = { Text("Cette vie ne sera pas archivée si tu recommences maintenant.") },
        confirmButton = { TextButton(onClick = { confirm = false; onRestart() }) { Text("RECOMMENCER") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("ANNULER") } }
    )
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        Text("LEGACY", color = Gold, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
        Text(c.name.uppercase(), fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(GameEngine.legacyTitle(c), color = Gold, fontSize = 20.sp)
        Spacer(Modifier.height(20.dp))
        InfoCard("18 → ${c.age} ans", "Orientation : ${c.moralLabel}\nPortée : ${c.scope.label}\nPrestige : ${c.prestige}\nOpinion : ${c.opinion}\nPeur : ${c.fear}\nPuissance : ${c.power}\nLegacy Score : ${GameEngine.legacyScore(c)}\nArcs encore ouverts : ${c.threads.size}")
        Spacer(Modifier.height(18.dp))
        Text("${c.alias} n'est pas devenu ce qu'il est par une seule décision. Sa légende est faite des choix répétés, des relations conservées ou brisées et des conséquences qui ont survécu à l'événement qui les avait créées.", color = Muted, lineHeight = 22.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onArchive, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("ARCHIVER CETTE VIE") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("RECOMMENCER") }
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
private fun SkylineBackdrop(scope: Scope) {
    Canvas(Modifier.fillMaxSize()) {
        val horizon = size.height * .72f
        val step = size.width / 10f
        for (i in 0..10) {
            val height = (50 + ((i * 37 + scope.ordinal * 29) % 180)).dp.toPx()
            drawRect(Color(0x141F2A35), topLeft = Offset(i * step, horizon - height), size = androidx.compose.ui.geometry.Size(step - 3f, height))
        }
        val p = Path().apply { moveTo(0f, horizon); lineTo(size.width, horizon - scope.ordinal * 18.dp.toPx()) }
        drawPath(p, Color(0x224F6575))
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(value, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(progress = { percent.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Gold, trackColor = Color(0xFF272C33))
    }
}

private fun qualitative(v: Int) = when { v >= 85 -> "Exceptionnel"; v >= 65 -> "Élevé"; v >= 40 -> "Modéré"; v >= 20 -> "Faible"; else -> "Critique" }
private fun signedLabel(v: Int) = when { v >= 50 -> "Allié"; v >= 20 -> "Favorable"; v > -20 -> "Neutre"; v > -50 -> "Hostile"; else -> "Ennemi" }
private fun bondLabel(v: Int) = when { v >= 80 -> "Très solide"; v >= 60 -> "Solide"; v >= 40 -> "Fragile"; v >= 20 -> "Très fragile"; else -> "Rupture proche" }

private fun saveCampaign(context: Context, c: Campaign) {
    fun e(v: Any) = Uri.encode(v.toString())
    val flags = c.flags.joinToString(";")
    val threads = c.threads.joinToString(";") { listOf(it.id, it.openedTurn, it.lastTurn, it.stage, it.lastApproach, it.intensity).joinToString(",") }
    val timeline = c.timeline.joinToString("\n")
    val fields = listOf(
        c.seed, c.name, c.alias, c.origin, c.powerFamily, c.weakness, c.modifier, c.pronouns, c.city, c.district, c.socialBackground, c.motivation, c.visualStyle,
        c.turn, c.morality, c.prestige, c.opinion, c.fear, c.power, c.control, c.influence, c.health, c.civilianCasualties, c.identityExposure,
        c.familyBond, c.rivalStanding, c.governmentStanding, c.factionStanding, c.mediaStanding, flags, threads, c.lastCategory, c.lastApproach, timeline
    ).joinToString("|") { e(it) }
    context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().putString("campaign", "V3|$fields").apply()
}

private fun loadCampaign(context: Context): Campaign? {
    val raw = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).getString("campaign", null) ?: return null
    return runCatching {
        if (raw.startsWith("V3|")) {
            val p = raw.removePrefix("V3|").split('|').map(Uri::decode)
            val flags = p.getOrElse(29) { "" }.split(';').filter { it.isNotBlank() }.toSet()
            val threads = p.getOrElse(30) { "" }.split(';').mapNotNull { item ->
                val t = item.split(',')
                if (t.size < 6) null else StoryThread(t[0], t[1].toInt(), t[2].toInt(), t[3].toInt(), t[4], t[5].toInt())
            }
            Campaign(
                seed = p[0].toLong(), name = p[1], alias = p[2], origin = p[3], powerFamily = p[4], weakness = p[5], modifier = p[6],
                pronouns = p[7], city = p[8], district = p[9], socialBackground = p[10], motivation = p[11], visualStyle = p[12],
                turn = p[13].toInt(), morality = p[14].toInt(), prestige = p[15].toInt(), opinion = p[16].toInt(), fear = p[17].toInt(), power = p[18].toInt(), control = p[19].toInt(), influence = p[20].toInt(), health = p[21].toInt(), civilianCasualties = p[22].toInt(), identityExposure = p[23].toInt(),
                familyBond = p[24].toInt(), rivalStanding = p[25].toInt(), governmentStanding = p[26].toInt(), factionStanding = p[27].toInt(), mediaStanding = p[28].toInt(),
                flags = flags, threads = threads, lastCategory = p.getOrElse(31) { "" }, lastApproach = p.getOrElse(32) { "" }, timeline = p.getOrElse(33) { "" }.lines().filter { it.isNotBlank() }
            )
        } else {
            val p = raw.split('|')
            Campaign(
                seed = p[0].toLong(), name = p[1], alias = p[2], origin = p[3], powerFamily = p[4], weakness = p[5], modifier = p[6],
                turn = p.getOrElse(7) { "0" }.toInt(), morality = p.getOrElse(8) { "0" }.toInt(), prestige = p.getOrElse(9) { "0" }.toInt(), opinion = p.getOrElse(10) { "0" }.toInt(), fear = p.getOrElse(11) { "0" }.toInt(), power = p.getOrElse(12) { "28" }.toInt(), control = p.getOrElse(13) { "25" }.toInt(), influence = p.getOrElse(14) { "0" }.toInt(), health = p.getOrElse(15) { "100" }.toInt(), civilianCasualties = p.getOrElse(16) { "0" }.toInt(), identityExposure = p.getOrElse(17) { "0" }.toInt(), timeline = p.getOrElse(18) { "" }.split('~').filter { it.isNotBlank() }
            )
        }
    }.getOrNull()
}

private fun clearCampaign(context: Context) = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().remove("campaign").apply()
private fun loadHall(context: Context): List<String> = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).getString("hall", "")!!.split(";;").filter { it.isNotBlank() }
private fun saveHall(context: Context, hall: List<String>) = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().putString("hall", hall.joinToString(";;")).apply()
