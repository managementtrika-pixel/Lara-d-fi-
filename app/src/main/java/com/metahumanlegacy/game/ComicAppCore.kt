package com.metahumanlegacy.game

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ComicMetahumanLegacyApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("mhl_visual", Context.MODE_PRIVATE) }
    var reduceMotion by remember { mutableStateOf(prefs.getBoolean("reduce_motion", false)) }
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = MetahumanColors.Coal,
            surface = MetahumanColors.Panel,
            primary = MetahumanColors.Gold,
            secondary = MetahumanColors.ElectricBlue,
            error = MetahumanColors.Red,
            onBackground = MetahumanColors.Ivory,
            onSurface = MetahumanColors.Ivory,
            onPrimary = MetahumanColors.Black
        )
    ) {
        var campaign by remember { mutableStateOf(loadCampaignV4(context)) }
        var screen by remember { mutableStateOf("HOME") }
        var hall by remember { mutableStateOf(loadHallV4(context)) }
        var outcome by remember { mutableStateOf<String?>(null) }
        var draft by remember { mutableStateOf(GameEngine.randomBlueprint(System.currentTimeMillis())) }

        fun start(blueprint: CharacterBlueprint) {
            campaign = GameEngine.newCampaign(System.currentTimeMillis(), blueprint)
            saveCampaignV4(context, campaign!!)
            outcome = null
            screen = "DESTIN"
        }
        fun abandonAndCreate() {
            clearCampaignV4(context)
            campaign = null
            outcome = null
            draft = GameEngine.randomBlueprint(System.currentTimeMillis() xor 0x77L)
            screen = "CREATE"
        }

        MhlScreen {
            when {
                screen == "SETTINGS" -> ComicSettingsScreen(reduceMotion, {
                    reduceMotion = it
                    prefs.edit().putBoolean("reduce_motion", it).apply()
                }) { screen = if (campaign == null) "HOME" else "DESTIN" }
                screen == "HALL" -> ComicHallScreen(hall) { screen = "HOME" }
                screen == "HOME" -> ComicHomeScreen(campaign, hall.size,
                    onContinue = { screen = if (campaign?.needsAlias == true) "ALIAS" else "DESTIN" },
                    onNew = { abandonAndCreate() }, onHall = { screen = "HALL" }, onSettings = { screen = "SETTINGS" })
                campaign == null && screen == "CREATE" -> ComicCreateScreen(draft, { draft = it }, { screen = "HOME" }) { start(it) }
                campaign == null -> ComicHomeScreen(null, hall.size, {}, { abandonAndCreate() }, { screen = "HALL" }, { screen = "SETTINGS" })
                campaign!!.finished -> ComicFinalScreen(campaign!!, onArchive = {
                    val who = campaign!!.alias.ifBlank { campaign!!.name }
                    hall = (listOf("$who|${GameEngine.legacyTitle(campaign!!)}|${GameEngine.legacyScore(campaign!!)}|${campaign!!.scope.label}") + hall).distinct().take(40)
                    saveHallV4(context, hall)
                    clearCampaignV4(context)
                    campaign = null; outcome = null; screen = "HOME"
                }, onRestart = { abandonAndCreate() })
                screen == "ALIAS" -> ComicAliasScreen(campaign!!) { alias ->
                    campaign = GameEngine.setAlias(campaign!!, alias)
                    saveCampaignV4(context, campaign!!)
                    screen = "DESTIN"
                }
                else -> ComicCareerShell(campaign!!, screen, outcome, reduceMotion,
                    onScreen = { screen = it },
                    onContinue = { outcome = null; if (campaign?.needsAlias == true) screen = "ALIAS" },
                    onChoice = { event, choice ->
                        val resolution = GameEngine.resolve(campaign!!, event, choice)
                        campaign = resolution.campaign
                        outcome = resolution.outcome
                        saveCampaignV4(context, campaign!!)
                    },
                    onHome = { screen = "HOME" }, onSettings = { screen = "SETTINGS" }, onRestart = { abandonAndCreate() })
            }
        }
    }
}

@Composable
private fun ComicHomeScreen(campaign: Campaign?, hallCount: Int, onContinue: () -> Unit, onNew: () -> Unit, onHall: () -> Unit, onSettings: () -> Unit) {
    var confirmNew by remember { mutableStateOf(false) }
    if (confirmNew) AlertDialog(onDismissRequest = { confirmNew = false }, title = { Text("Commencer une nouvelle vie ?") },
        text = { Text("La carrière en cours sera abandonnée. Le Hall of Legacies sera conservé.") },
        confirmButton = { TextButton(onClick = { confirmNew = false; onNew() }) { Text("NOUVELLE VIE") } },
        dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("ANNULER") } })
    Column(Modifier.fillMaxSize().padding(MetahumanDimensions.Screen).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) { TextButton(onClick = onSettings) { Text("RÉGLAGES", color = MetahumanColors.Muted, fontSize = 11.sp) } }
        MhlAsset("brand_hero", "Emblème MetaHuman Legacy", size = 128.dp)
        Spacer(Modifier.height(8.dp))
        Text("METAHUMAN", color = MetahumanColors.Muted, letterSpacing = 5.sp, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("LEGACY", color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 52.sp, lineHeight = 49.sp)
        Text("TA VIE. TON ÉVEIL. TA SAGA.", color = MetahumanColors.Gold, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(18.dp))
        MhlComicPanel(accent = MetahumanColors.DeepBlue) {
            Text("À 18 ans, tu n'es encore personne de connu. Dix décisions humaines construiront silencieusement ce qui finira par se révéler.", color = MetahumanColors.Muted, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(18.dp))
        if (campaign != null) {
            MhlPrimaryButton("Continuer · ${campaign.alias.ifBlank { campaign.name }}", onContinue, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); MhlSecondaryButton("Nouvelle vie", { confirmNew = true }, Modifier.fillMaxWidth())
        } else MhlPrimaryButton("Commencer une vie", onNew, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp)); MhlSecondaryButton("Hall of Legacies · $hallCount", onHall, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ComicCreateScreen(initial: CharacterBlueprint, onDraft: (CharacterBlueprint) -> Unit, onBack: () -> Unit, onStart: (CharacterBlueprint) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var d by remember(initial) { mutableStateOf(initial) }
    fun update(n: CharacterBlueprint) { d = n; onDraft(n) }
    val visual = when (step) { 0 -> "scope_street"; 1 -> "scope_district"; else -> "scope_city" }
    Column(Modifier.fillMaxSize().padding(MetahumanDimensions.Screen)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("DOSSIER CIVIL", color = MetahumanColors.Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp); Text("PERSONNE ORDINAIRE · ${step + 1}/3", color = MetahumanColors.Muted, fontSize = 11.sp) }
            MhlAsset(visual, "Environnement civil", size = 58.dp)
        }
        Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth().height(5.dp), color = MetahumanColors.Gold, trackColor = Color(0xFF27303A)); Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> { ComicHeadline("IDENTITÉ CIVILE", "Tu n'as ni masque, ni pouvoir, ni légende. Seulement un nom et une ville.")
                    OutlinedTextField(d.firstName, { update(d.copy(firstName = it)) }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(8.dp))
                    OutlinedTextField(d.lastName, { update(d.copy(lastName = it)) }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    ComicOptionStrip("PRONOM", GameEngine.pronouns, d.pronouns) { update(d.copy(pronouns = it)) }; ComicOptionStrip("VILLE", GameEngine.cities, d.city) { update(d.copy(city = it)) }; ComicOptionStrip("QUARTIER", GameEngine.districts, d.district) { update(d.copy(district = it)) } }
                1 -> { ComicHeadline("TA VIE AVANT TOUT ÇA", "Ton environnement te donne des habitudes et des liens. Il ne te donne pas un pouvoir.")
                    ComicOptionStrip("CONTEXTE SOCIAL", GameEngine.socialBackgrounds, d.socialBackground) { update(d.copy(socialBackground = it)) }; ComicOptionStrip("TRAJECTOIRE", GameEngine.civilianPaths, d.civilianPath) { update(d.copy(civilianPath = it)) } }
                else -> { ComicHeadline("QUI ES-TU À 18 ANS ?", "Tes motivations comptent. Le futur, lui, reste invisible.")
                    ComicOptionStrip("CE QUI TE POUSSE", GameEngine.motivations, d.motivation) { update(d.copy(motivation = it)) }; ComicOptionStrip("TEMPÉRAMENT", GameEngine.temperaments, d.temperament) { update(d.copy(temperament = it)) }
                    Spacer(Modifier.height(12.dp)); MhlComicPanel(accent = MetahumanColors.ElectricBlue) { Text(d.fullName.uppercase(), fontWeight = FontWeight.Black, fontSize = 19.sp); Text("${d.pronouns} · ${d.city}, ${d.district}\n${d.socialBackground}\n${d.civilianPath}\n${d.motivation} · ${d.temperament}", color = MetahumanColors.Muted, lineHeight = 20.sp) }
                    Spacer(Modifier.height(10.dp)); MhlComicPanel(accent = MetahumanColors.Gold) { Text("AUCUN BUILD À CHOISIR", color = MetahumanColors.Gold, fontWeight = FontWeight.Black); Text("Les dix premières décisions formeront secrètement des affinités, une expression et un coût. Le jeu ne t'affichera jamais ces calculs.", color = MetahumanColors.Muted, lineHeight = 20.sp) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MhlSecondaryButton(if (step == 0) "Retour" else "Précédent", { if (step == 0) onBack() else step-- }, Modifier.weight(1f)); MhlPrimaryButton(if (step < 2) "Suivant" else "Commencer à 18 ans", { if (step < 2) step++ else onStart(d) }, Modifier.weight(1f)) }
    }
}

@Composable private fun ComicHeadline(title: String, body: String) { Text(title, color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 30.sp); Spacer(Modifier.height(8.dp)); Text(body, color = MetahumanColors.Muted, lineHeight = 21.sp); Spacer(Modifier.height(16.dp)) }
@Composable private fun ComicOptionStrip(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) { Spacer(Modifier.height(14.dp)); MhlSectionTitle(title, MetahumanColors.DeepBlue); Spacer(Modifier.height(6.dp)); Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { options.forEach { option -> FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option) }) } } }

@Composable
private fun ComicAliasScreen(c: Campaign, onConfirm: (String) -> Unit) {
    var alias by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MhlAsset(powerIcon(c.powerFamily), "Symbole du pouvoir révélé", size = 150.dp) }
        Text("APRÈS L'ÉVEIL", color = MetahumanColors.Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Text("LE MONDE N'A PAS ENCORE DE NOM POUR TOI", color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 31.sp, lineHeight = 33.sp)
        Spacer(Modifier.height(12.dp)); MhlComicPanel(accent = MetahumanColors.ElectricBlue) { Text("POUVOIR RÉVÉLÉ · ${c.powerFamily}", fontWeight = FontWeight.Black, color = MetahumanColors.WarmGold); Text(c.powerRevealText, color = MetahumanColors.Ivory, lineHeight = 21.sp); Spacer(Modifier.height(8.dp)); Text("COÛT · ${c.weakness}", color = MetahumanColors.Red, fontWeight = FontWeight.Bold); Text(c.powerCostText, color = MetahumanColors.Muted, lineHeight = 20.sp) }
        Spacer(Modifier.height(14.dp)); OutlinedTextField(alias, { if (it.length <= 28) alias = it }, label = { Text("Nom de terrain / alias") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(12.dp)); MhlPrimaryButton("Prendre ce nom", { onConfirm(alias) }, Modifier.fillMaxWidth(), alias.trim().isNotBlank())
    }
}

@Composable
private fun ComicCareerShell(c: Campaign, screen: String, outcome: String?, reduceMotion: Boolean, onScreen: (String) -> Unit, onContinue: () -> Unit, onChoice: (EventNode, Choice) -> Unit, onHome: () -> Unit, onSettings: () -> Unit, onRestart: () -> Unit) {
    var confirmRestart by remember { mutableStateOf(false) }
    if (confirmRestart) AlertDialog(onDismissRequest = { confirmRestart = false }, title = { Text("Recommencer cette destinée ?") }, text = { Text("La carrière actuelle sera abandonnée. Le Hall of Legacies sera conservé.") }, confirmButton = { TextButton(onClick = { confirmRestart = false; onRestart() }) { Text("RECOMMENCER") } }, dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("ANNULER") } })
    Column(Modifier.fillMaxSize()) {
        ComicCareerHeader(c, onHome, onSettings) { confirmRestart = true }
        Box(Modifier.weight(1f)) { when (screen) { "PERSONNAGE" -> ComicCharacterScreen(c); "MONDE" -> ComicWorldScreen(c); "LIENS" -> ComicLinksScreen(c); "CHRONIQUE" -> ComicTimelineScreen(c); else -> ComicDestinyScreen(c, outcome, reduceMotion, onContinue, onChoice) } }
        ComicNavigation(screen, onScreen)
    }
}

@Composable private fun ComicCareerHeader(c: Campaign, onHome: () -> Unit, onSettings: () -> Unit, onRestart: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xF2080B10)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        MhlAsset(if (c.powerRevealed) powerIcon(c.powerFamily) else civilProgressIcon(c.turn), "État de la destinée", size = 50.dp); Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) { Text(c.alias.ifBlank { c.name }.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 18.sp); Text("${c.age} ANS · ${c.phaseLabel} · ${if (c.powerRevealed) c.scope.label else "CIVIL"}", color = MetahumanColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        Column(horizontalAlignment = Alignment.End) { TextButton(onClick = onHome, contentPadding = PaddingValues(3.dp)) { Text("ACCUEIL", fontSize = 9.sp) }; Row { TextButton(onClick = onSettings, contentPadding = PaddingValues(3.dp)) { Text("VISUEL", fontSize = 9.sp) }; TextButton(onClick = onRestart, contentPadding = PaddingValues(3.dp)) { Text("RESET", fontSize = 9.sp, color = MetahumanColors.Red) } } }
    }
}

@Composable private fun ComicNavigation(screen: String, onScreen: (String) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0A0E14), tonalElevation = 0.dp) {
        listOf("DESTIN" to "alt_01", "PERSONNAGE" to "alt_02", "MONDE" to "scope_world", "LIENS" to "relation_family", "CHRONIQUE" to "alt_03").forEach { (item, icon) -> NavigationBarItem(selected = screen == item, onClick = { onScreen(item) }, icon = { MhlAsset(icon, item, size = 27.dp) }, label = { Text(item.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 9.sp) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0x332E83FF), selectedTextColor = MetahumanColors.Gold, unselectedTextColor = MetahumanColors.Muted)) }
    }
}
