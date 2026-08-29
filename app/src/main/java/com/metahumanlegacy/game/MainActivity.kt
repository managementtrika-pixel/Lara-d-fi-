package com.metahumanlegacy.game

import android.content.Context
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
        var campaign by remember { mutableStateOf(loadCampaignV4(context)) }
        var screen by remember { mutableStateOf(when {
            campaign?.needsAlias == true -> "ALIAS"
            campaign == null -> "HOME"
            else -> "DESTIN"
        }) }
        var hall by remember { mutableStateOf(loadHallV4(context)) }
        var outcome by remember { mutableStateOf<String?>(null) }
        var draft by remember { mutableStateOf(GameEngine.randomBlueprint(System.currentTimeMillis())) }

        fun start(blueprint: CharacterBlueprint) {
            val seed = System.currentTimeMillis()
            campaign = GameEngine.newCampaign(seed, blueprint)
            saveCampaignV4(context, campaign!!)
            outcome = null
            screen = "DESTIN"
        }

        fun restart() {
            clearCampaignV4(context)
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
                            val who = campaign!!.alias.ifBlank { campaign!!.name }
                            val entry = "$who|${GameEngine.legacyTitle(campaign!!)}|${GameEngine.legacyScore(campaign!!)}|${campaign!!.scope.label}"
                            hall = (listOf(entry) + hall).distinct().take(40)
                            saveHallV4(context, hall)
                            clearCampaignV4(context)
                            campaign = null
                            outcome = null
                            screen = "HOME"
                        },
                        onRestart = { restart() }
                    )
                    screen == "ALIAS" -> AliasScreen(
                        c = campaign!!,
                        onConfirm = { alias ->
                            campaign = GameEngine.setAlias(campaign!!, alias)
                            saveCampaignV4(context, campaign!!)
                            screen = "DESTIN"
                        }
                    )
                    screen == "HALL" -> HallScreen(hall) { screen = "DESTIN" }
                    else -> CareerShell(
                        c = campaign!!,
                        screen = screen,
                        outcome = outcome,
                        onScreen = { screen = it },
                        onContinue = {
                            outcome = null
                            if (campaign?.needsAlias == true) screen = "ALIAS"
                        },
                        onChoice = { event, choice ->
                            val resolution = GameEngine.resolve(campaign!!, event, choice)
                            campaign = resolution.campaign
                            outcome = resolution.outcome
                            saveCampaignV4(context, campaign!!)
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
        Text(
            "Tu commences à 18 ans comme une personne ordinaire. Tu ne choisis ni pouvoir, ni faiblesse, ni nom héroïque. Dix années de décisions construisent silencieusement ce qui finira par se révéler.",
            color = Muted, lineHeight = 22.sp
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(58.dp)) { Text("COMMENCER UNE VIE") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("IDENTITÉ ALÉATOIRE") }
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
            TextButton(onClick = { update(GameEngine.randomBlueprint(System.currentTimeMillis())) }) { Text("ALÉATOIRE") }
        }
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Gold, trackColor = Color(0xFF272C33))
        Spacer(Modifier.height(14.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> {
                    CreationTitle("IDENTITÉ CIVILE")
                    Text("Pour l'instant, tu n'es personne de connu. Seulement une personne de 18 ans avec une vie à construire.", color = Muted, lineHeight = 21.sp)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(value = d.firstName, onValueChange = { update(d.copy(firstName = it)) }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = d.lastName, onValueChange = { update(d.copy(lastName = it)) }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OptionStrip("PRONOM", GameEngine.pronouns, d.pronouns) { update(d.copy(pronouns = it)) }
                    OptionStrip("VILLE", GameEngine.cities, d.city) { update(d.copy(city = it)) }
                    OptionStrip("QUARTIER", GameEngine.districts, d.district) { update(d.copy(district = it)) }
                }
                1 -> {
                    CreationTitle("TA VIE AVANT TOUT ÇA")
                    Text("Ton environnement ne te donne pas un pouvoir. Il détermine seulement les outils humains, les habitudes et les liens avec lesquels tu affronteras les dix prochaines années.", color = Muted, lineHeight = 21.sp)
                    OptionStrip("CONTEXTE SOCIAL", GameEngine.socialBackgrounds, d.socialBackground) { update(d.copy(socialBackground = it)) }
                    OptionStrip("TRAJECTOIRE", GameEngine.civilianPaths, d.civilianPath) { update(d.copy(civilianPath = it)) }
                }
                else -> {
                    CreationTitle("QUI ES-TU À 18 ANS ?")
                    OptionStrip("CE QUI TE POUSSE", GameEngine.motivations, d.motivation) { update(d.copy(motivation = it)) }
                    OptionStrip("TEMPÉRAMENT", GameEngine.temperaments, d.temperament) { update(d.copy(temperament = it)) }
                    Spacer(Modifier.height(12.dp))
                    InfoCard(
                        d.fullName,
                        "${d.pronouns} · ${d.city}, ${d.district}\n${d.socialBackground}\n${d.civilianPath}\nMotivation : ${d.motivation}\nTempérament : ${d.temperament}"
                    )
                    Spacer(Modifier.height(12.dp))
                    InfoCard(
                        "TU NE CHOISIS PAS TON BUILD",
                        "Aucun pouvoir, aucune faiblesse et aucun alias ne sont définis ici. Les dix premières décisions, entre 18 et 27 ans, formeront secrètement ton affinité, la manière dont ton futur pouvoir s'exprime et le prix qu'il te fera payer."
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { if (step == 0) onBack() else step-- }, modifier = Modifier.weight(1f).height(52.dp)) { Text(if (step == 0) "RETOUR" else "PRÉCÉDENT") }
            Button(onClick = { if (step < 2) step++ else onStart(d) }, modifier = Modifier.weight(1f).height(52.dp)) { Text(if (step < 2) "SUIVANT" else "COMMENCER À 18 ANS") }
        }
    }
}

@Composable
private fun AliasScreen(c: Campaign, onConfirm: (String) -> Unit) {
    var alias by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(22.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        Text("APRÈS L'ÉVEIL", color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Text("LE MONDE N'A PAS ENCORE DE NOM POUR TOI", fontSize = 31.sp, fontWeight = FontWeight.Black, lineHeight = 33.sp)
        Spacer(Modifier.height(14.dp))
        Text("Tu connais enfin ce qui s'est manifesté. Maintenant seulement, tu peux décider du nom sous lequel tes actes pourront être racontés.", color = Muted, lineHeight = 22.sp)
        Spacer(Modifier.height(18.dp))
        InfoCard("POUVOIR RÉVÉLÉ · ${c.powerFamily}", "${c.powerRevealText}\n\nCoût principal : ${c.weakness}\n${c.powerCostText}")
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = alias,
            onValueChange = { if (it.length <= 28) alias = it },
            label = { Text("Nom de terrain / alias") },
            supportingText = { Text("Ce nom n'a pas créé ton pouvoir. Il vient après.") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onConfirm(alias) }, enabled = alias.trim().isNotBlank(), modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("PRENDRE CE NOM") }
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
                Text(c.alias.ifBlank { c.name }.uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("${c.age} ans · ${c.city} · ${c.phaseLabel}", color = Muted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (c.powerRevealed) Text("P ${c.prestige}", color = Gold, fontWeight = FontWeight.Bold)
                else Text("ORDINAIRE", color = Gold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
            Text(if (c.turn <= 10) "TA VIE CONTINUE" else "LE MONDE RÉPOND", color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text(outcome, fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(18.dp))
            if (c.turn <= 10) {
                InfoCard("RIEN N'EST ENCORE CHOISI", "Le jeu conserve certaines conséquences hors de ta vue. Tu construis une histoire, pas une fiche de pouvoir.")
            } else {
                InfoCard("CONSÉQUENCE PERSISTANTE", "Relations, souvenirs et fils narratifs peuvent évoluer longtemps après la décision qui les a créés.")
            }
            Spacer(Modifier.height(22.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(if (c.needsAlias) "CHOISIR CE QU'ON T'APPELLERA" else "CONTINUER LA DESTINÉE") }
        }
        return
    }

    val event = remember(c.seed, c.turn, c.flags, c.threads, c.lastCategory, c.lastApproach, c.powerFamily) { GameEngine.event(c) }
    val leftHeader = when {
        c.turn < 10 -> "AVANT LE MASQUE · ANNÉE ${c.turn + 1}/10"
        c.turn == 10 -> "ÉVEIL · 28 ANS"
        c.turn in 11..15 -> "PREMIERS PAS · ${c.turn - 10}/5"
        else -> "${event.category} · DÉCISION ${c.turn - 15}/140"
    }
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftHeader, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (c.turn >= 10) Text("ENJEU ${event.stakes}/3", color = if (event.stakes >= 3) Danger else Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (event.kind == "MAJOR" && event.threadStage > 1) {
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
                    if (choice.risk > 0 && event.kind != "AWAKENING") {
                        Spacer(Modifier.width(8.dp))
                        Text(riskLabel(choice.risk), color = if (choice.risk >= 7) Danger else Gold, fontSize = 9.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when (event.kind) {
                "FORMATIVE" -> "Ces décisions restent humaines. Leurs effets les plus importants sont volontairement invisibles."
                "AWAKENING" -> "Le pouvoir est déjà déterminé. Tu choisis seulement ta première réaction face à lui."
                else -> "Le monde se souvient de ce que tu fais, parfois plusieurs années avant d'en montrer le prix."
            },
            color = Color(0xFF707781), fontSize = 11.sp, lineHeight = 16.sp
        )
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
        InfoCard(c.name, "${c.pronouns} · ${c.city}, ${c.district}\n${c.socialBackground}\n${c.civilianPath}\nTempérament : ${c.temperament}")
        Spacer(Modifier.height(10.dp))
        if (!c.powerRevealed) {
            SectionTitle("CE QUE TU IGNORES ENCORE")
            InfoCard("Aucun pouvoir révélé", "Tu n'as pas choisi de pouvoir, de faiblesse ou de nom héroïque. À ${c.age} ans, ta vie est encore celle d'une personne ordinaire confrontée à des décisions qui auront un sens plus tard.")
        } else {
            SectionTitle("APRÈS L'ÉVEIL")
            InfoCard(c.alias.ifBlank { "Sans alias" }, "Origine comprise : ${c.origin}\nPouvoir : ${c.powerFamily}\nFaiblesse : ${c.weakness}\nSignature : ${c.powerSignature}")
            Spacer(Modifier.height(12.dp))
            SectionTitle("AXES MÉTAHUMAINS")
            StatLine("Puissance", qualitative(c.power), c.power)
            StatLine("Maîtrise", qualitative(c.control), c.control)
            StatLine("Exposition identité", qualitative(c.identityExposure), c.identityExposure)
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle("AXES DE VIE")
        StatLine("Moralité", c.moralLabel, (c.morality + 100) / 2)
        StatLine("Opinion", if (c.opinion >= 0) "${c.opinion}% favorable" else "${-c.opinion}% hostile", (c.opinion + 100) / 2)
        StatLine("Santé", qualitative(c.health), c.health)
        if (c.powerRevealed) StatLine("Peur", qualitative(c.fear), c.fear)
        Spacer(Modifier.height(10.dp))
        InfoCard(if (c.powerRevealed) "Prestige ${c.prestige}" else "Avant la légende", if (c.powerRevealed) "Influence ${c.influence} · Portée : ${c.scope.label}\nVictimes civiles attribuées : ${c.civilianCasualties}\nFils narratifs actifs : ${c.threads.size}" else "Aucun prestige surnaturel. Aucune célébrité métahumaine. Seulement dix années qui fabriquent la personne que tu seras.")
    }
}

@Composable
private fun WorldScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle(if (c.powerRevealed) "MONDE VIVANT" else "TA VILLE")
        if (!c.powerRevealed) {
            InfoCard("${c.city} · ${c.modifier}", "Tu vis encore dans ${c.district}. Le monde ne connaît aucun alias pour toi et aucune institution ne te traite comme un héros. Les incidents étranges existent, mais leur rapport avec toi reste ambigu.")
            Spacer(Modifier.height(12.dp))
            InfoCard("UNE VIE EN FORMATION", "Tes décisions civiles, tes proches, les blessures et les choses que tu décides d'observer ou d'ignorer sont déjà mémorisés. Tu ne sais simplement pas encore pourquoi.")
        } else {
            InfoCard("${c.city} · ${c.modifier}", "Tu as commencé dans ${c.district}. Les décisions prises avant ton éveil peuvent désormais revenir sous une autre forme.")
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
            c.threads.forEach { thread -> InfoCard(thread.id, "Chapitre atteint : ${thread.stage}\nDernière méthode : ${thread.lastApproach}\nIntensité : ${thread.intensity}/3"); Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun LinksScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("LIENS PERSISTANTS")
        StatLine("Famille / proches", bondLabel(c.familyBond), c.familyBond)
        Spacer(Modifier.height(12.dp))
        InfoCard("Tes proches", when {
            c.familyBond >= 75 -> "Ils occupent une place centrale dans tes décisions. Ce lien pourra devenir une force autant qu'une vulnérabilité."
            c.familyBond <= 25 -> "Les années qui passent ont déjà créé de la distance. L'éveil ne réparera pas automatiquement ce qui s'est cassé avant lui."
            else -> "Le lien tient, avec les tensions ordinaires d'une vie qui change."
        })
        if (c.powerRevealed) {
            Spacer(Modifier.height(12.dp))
            StatLine("Rival", signedLabel(c.rivalStanding), (c.rivalStanding + 100) / 2)
            InfoCard("Rivalité", if (c.rivalStanding <= -35) "Certains adversaires étudient désormais tes habitudes et ton passé." else "Cette relation peut encore devenir alliance, respect ou conflit personnel.")
        }
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
        Text(c.alias.ifBlank { c.name }.uppercase(), fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(GameEngine.legacyTitle(c), color = Gold, fontSize = 20.sp)
        Spacer(Modifier.height(20.dp))
        InfoCard("18 → ${c.age} ans", "Orientation : ${c.moralLabel}\nPortée : ${c.scope.label}\nPrestige : ${c.prestige}\nOpinion : ${c.opinion}\nPeur : ${c.fear}\nPuissance : ${c.power}\nLegacy Score : ${GameEngine.legacyScore(c)}\nArcs encore ouverts : ${c.threads.size}")
        Spacer(Modifier.height(18.dp))
        Text("Cette légende a commencé avant le pouvoir. Les dix années où personne ne connaissait ton nom font partie de la même histoire que tout ce qui est venu après.", color = Muted, lineHeight = 22.sp)
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
