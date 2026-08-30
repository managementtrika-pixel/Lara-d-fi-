package com.metahumanlegacy.game

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SESSION_FILE = "mhl_production_session"
private const val PENDING_OUTCOME = "pending_outcome"

private fun loadPendingOutcome(context: Context): String? =
    context.getSharedPreferences(SESSION_FILE, Context.MODE_PRIVATE).getString(PENDING_OUTCOME, null)

private fun savePendingOutcome(context: Context, value: String?) {
    val edit = context.getSharedPreferences(SESSION_FILE, Context.MODE_PRIVATE).edit()
    if (value == null) edit.remove(PENDING_OUTCOME) else edit.putString(PENDING_OUTCOME, value)
    edit.apply()
}

@Composable
fun ProductionMetahumanLegacyApp(context: Context) {
    var motion by remember { mutableStateOf(MetahumanMotionPreferences.load(context)) }
    val controller = remember(motion) {
        MetahumanMotionController(motion) { next ->
            val clean = next.copy(textScalePercent = next.textScalePercent.coerceIn(90, 120))
            motion = clean
            MetahumanMotionPreferences.save(context, clean)
        }
    }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity.density, baseDensity.fontScale, motion.textScalePercent) {
        Density(baseDensity.density, baseDensity.fontScale * motion.textScalePercent / 100f)
    }
    val colors = darkColorScheme(
        background = if (motion.highContrast) Color.Black else MetahumanColors.Coal,
        surface = if (motion.highContrast) Color(0xFF080A0D) else MetahumanColors.Panel,
        primary = MetahumanColors.Gold,
        secondary = MetahumanColors.ElectricBlue,
        error = MetahumanColors.Red,
        onBackground = if (motion.highContrast) Color.White else MetahumanColors.Ivory,
        onSurface = if (motion.highContrast) Color.White else MetahumanColors.Ivory,
        onPrimary = MetahumanColors.Black
    )

    CompositionLocalProvider(LocalMetahumanMotion provides controller, LocalDensity provides scaledDensity) {
        MaterialTheme(colorScheme = colors) {
            var campaign by remember { mutableStateOf(loadCampaignV4(context)) }
            var annualState by remember { mutableStateOf(campaign?.let { AnnualActionPersistence.load(context, it) }) }
            var screen by remember { mutableStateOf("HOME") }
            var hall by remember { mutableStateOf(loadHallV4(context)) }
            var outcome by remember { mutableStateOf(loadPendingOutcome(context)) }
            var lastEvent by remember { mutableStateOf<EventNode?>(null) }
            var draft by remember { mutableStateOf(GameEngine.randomBlueprint(System.currentTimeMillis())) }
            var saveFeedbackKey by remember { mutableIntStateOf(0) }

            fun markSaved() { saveFeedbackKey++ }

            fun start(blueprint: CharacterBlueprint) {
                campaign = GameEngine.newCampaign(System.currentTimeMillis(), blueprint)
                annualState = AnnualActionState.fresh(campaign!!)
                AnnualActionPersistence.save(context, annualState!!)
                saveCampaignV4(context, campaign!!)
                outcome = null
                savePendingOutcome(context, null)
                lastEvent = null
                markSaved()
                screen = "DESTIN"
            }

            fun abandonAndCreate() {
                campaign?.seed?.let { AnnualActionPersistence.clear(context, it) }
                clearCampaignV4(context)
                savePendingOutcome(context, null)
                campaign = null
                annualState = null
                outcome = null
                lastEvent = null
                draft = GameEngine.randomBlueprint(System.currentTimeMillis() xor 0x77L)
                screen = "CREATE"
            }

            Box(Modifier.fillMaxSize()) {
                MhlProductionBackdrop(
                    scene = when {
                        campaign?.finished == true -> "LEGACY"
                        screen == "HOME" -> "HOME"
                        else -> screen
                    },
                    seed = campaign?.seed ?: 17L
                )

                when {
                    screen == "SETTINGS" -> ProductionSettingsScreen(onBack = {
                        screen = if (campaign == null) "HOME" else "DESTIN"
                    })

                    screen == "HALL" -> ProductionHallScreen(hall) { screen = "HOME" }

                    screen == "HOME" -> ProductionHomeScreen(
                        campaign = campaign,
                        hallCount = hall.size,
                        onContinue = { screen = if (campaign?.needsAlias == true) "ALIAS" else "DESTIN" },
                        onNew = { abandonAndCreate() },
                        onHall = { screen = "HALL" },
                        onSettings = { screen = "SETTINGS" }
                    )

                    campaign == null && screen == "CREATE" -> ProductionCreateScreen(
                        initial = draft,
                        onDraft = { draft = it },
                        onBack = { screen = "HOME" },
                        onStart = { start(it) }
                    )

                    campaign == null -> ProductionHomeScreen(
                        campaign = null,
                        hallCount = hall.size,
                        onContinue = { },
                        onNew = { abandonAndCreate() },
                        onHall = { screen = "HALL" },
                        onSettings = { screen = "SETTINGS" }
                    )

                    campaign!!.finished -> ProductionFinalScreen(
                        c = campaign!!,
                        onArchive = {
                            val who = campaign!!.alias.ifBlank { campaign!!.name }
                            hall = (listOf("$who|${GameEngine.legacyTitle(campaign!!)}|${GameEngine.legacyScore(campaign!!)}|${campaign!!.scope.label}") + hall).distinct().take(40)
                            saveHallV4(context, hall)
                            AnnualActionPersistence.clear(context, campaign!!.seed)
                            clearCampaignV4(context)
                            savePendingOutcome(context, null)
                            campaign = null
                            annualState = null
                            outcome = null
                            lastEvent = null
                            screen = "HOME"
                        },
                        onRestart = { abandonAndCreate() }
                    )

                    screen == "ALIAS" -> ProductionAliasScreen(campaign!!) { alias ->
                        campaign = GameEngine.setAlias(campaign!!, alias)
                        saveCampaignV4(context, campaign!!)
                        markSaved()
                        screen = "DESTIN"
                    }

                    else -> ProductionCareerShell(
                        c = campaign!!,
                        annualState = annualState ?: AnnualActionState.fresh(campaign!!),
                        screen = screen,
                        outcome = outcome,
                        lastEvent = lastEvent,
                        saveFeedbackKey = saveFeedbackKey,
                        onScreen = { screen = it },
                        onContinue = {
                            outcome = null
                            savePendingOutcome(context, null)
                            if (campaign?.needsAlias == true) screen = "ALIAS"
                        },
                        onChoice = { event, choice ->
                            lastEvent = event
                            val resolution = GameEngine.resolve(campaign!!, event, choice)
                            campaign = resolution.campaign
                            annualState = (annualState ?: AnnualActionState.fresh(campaign!!)).synced(campaign!!)
                            AnnualActionPersistence.save(context, annualState!!)
                            outcome = resolution.outcome
                            saveCampaignV4(context, campaign!!)
                            savePendingOutcome(context, resolution.outcome)
                            markSaved()
                        },
                        onAnnualAction = { card ->
                            val base = (annualState ?: AnnualActionPersistence.load(context, campaign!!)).synced(campaign!!)
                            val rawResolution = AnnualActionEngine.perform(campaign!!, base, card)
                            val resolution = rawResolution?.let {
                                it.copy(campaign = DepthDirector.afterAnnualAction(it.campaign, it.state, card))
                            }
                            if (resolution != null) {
                                campaign = resolution.campaign
                                annualState = resolution.state
                                saveCampaignV4(context, resolution.campaign)
                                AnnualActionPersistence.save(context, resolution.state)
                                markSaved()
                            }
                            resolution
                        },
                        onHome = { screen = "HOME" },
                        onSettings = { screen = "SETTINGS" },
                        onRestart = { abandonAndCreate() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductionHomeScreen(campaign: Campaign?, hallCount: Int, onContinue: () -> Unit, onNew: () -> Unit, onHall: () -> Unit, onSettings: () -> Unit) {
    var confirmNew by remember { mutableStateOf(false) }
    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text("Commencer une nouvelle vie ?") },
            text = { Text("La carrière en cours sera abandonnée. Le Hall of Legacies sera conservé.") },
            confirmButton = { TextButton(onClick = { confirmNew = false; onNew() }) { Text("NOUVELLE VIE") } },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("ANNULER") } }
        )
    }

    MhlSceneFrame("home", MotionBoard.PANEL_TRANSITION, MetahumanMotionLevel.MOTION_SUBTLE, Modifier.fillMaxSize(), MetahumanColors.DeepBlue) {
        MhlBoardTexture(MotionBoard.AFTERMATH, Modifier.matchParentSize(), MetahumanColors.ElectricBlue, .07f)
        Column(
            Modifier.fillMaxSize().padding(MetahumanDimensions.Screen).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                TextButton(onClick = onSettings) { Text("RÉGLAGES", color = MetahumanColors.Muted, fontSize = 11.sp) }
            }
            MhlProductionAsset("brand_hero", "Emblème MetaHuman Legacy", size = 132.dp, pulse = true)
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
                Spacer(Modifier.height(8.dp))
                MhlSecondaryButton("Nouvelle vie", { confirmNew = true }, Modifier.fillMaxWidth())
            } else {
                MhlPrimaryButton("Commencer une vie", onNew, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            MhlSecondaryButton("Hall of Legacies · $hallCount", onHall, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProductionCreateScreen(initial: CharacterBlueprint, onDraft: (CharacterBlueprint) -> Unit, onBack: () -> Unit, onStart: (CharacterBlueprint) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var d by remember(initial) { mutableStateOf(initial) }
    fun update(n: CharacterBlueprint) { d = n; onDraft(n) }
    val visual = when (step) { 0 -> "scope_street"; 1 -> "scope_district"; else -> "scope_city" }

    MhlSceneFrame("create-$step", MotionBoard.PANEL_TRANSITION, MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), MetahumanColors.DeepBlue) {
        Column(Modifier.fillMaxSize().padding(MetahumanDimensions.Screen)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("DOSSIER CIVIL", color = MetahumanColors.Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("PERSONNE ORDINAIRE · ${step + 1}/3", color = MetahumanColors.Muted, fontSize = 11.sp)
                }
                MhlProductionAsset(visual, "Environnement civil", size = 58.dp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth().height(5.dp), color = MetahumanColors.Gold, trackColor = Color(0xFF27303A))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (step) {
                    0 -> {
                        ProductionHeadline("IDENTITÉ CIVILE", "Tu n'as ni masque, ni pouvoir, ni légende. Seulement un nom et une ville.")
                        OutlinedTextField(d.firstName, { update(d.copy(firstName = it)) }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(d.lastName, { update(d.copy(lastName = it)) }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        ProductionOptionStrip("PRONOM", GameEngine.pronouns, d.pronouns) { update(d.copy(pronouns = it)) }
                        ProductionOptionStrip("VILLE", GameEngine.cities, d.city) { update(d.copy(city = it)) }
                        ProductionOptionStrip("QUARTIER", GameEngine.districts, d.district) { update(d.copy(district = it)) }
                    }
                    1 -> {
                        ProductionHeadline("TA VIE AVANT TOUT ÇA", "Ton environnement te donne des habitudes et des liens. Il ne te donne pas un pouvoir.")
                        ProductionOptionStrip("CONTEXTE SOCIAL", GameEngine.socialBackgrounds, d.socialBackground) { update(d.copy(socialBackground = it)) }
                        ProductionOptionStrip("TRAJECTOIRE", GameEngine.civilianPaths, d.civilianPath) { update(d.copy(civilianPath = it)) }
                    }
                    else -> {
                        ProductionHeadline("QUI ES-TU À 18 ANS ?", "Tes motivations comptent. Le futur, lui, reste invisible.")
                        ProductionOptionStrip("CE QUI TE POUSSE", GameEngine.motivations, d.motivation) { update(d.copy(motivation = it)) }
                        ProductionOptionStrip("TEMPÉRAMENT", GameEngine.temperaments, d.temperament) { update(d.copy(temperament = it)) }
                        Spacer(Modifier.height(12.dp))
                        MhlComicPanel(accent = MetahumanColors.ElectricBlue) {
                            Text(d.fullName.uppercase(), fontWeight = FontWeight.Black, fontSize = 19.sp)
                            Text("${d.pronouns} · ${d.city}, ${d.district}\n${d.socialBackground}\n${d.civilianPath}\n${d.motivation} · ${d.temperament}", color = MetahumanColors.Muted, lineHeight = 20.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        MhlComicPanel(accent = MetahumanColors.Gold) {
                            Text("AUCUN BUILD À CHOISIR", color = MetahumanColors.Gold, fontWeight = FontWeight.Black)
                            Text("Les dix premières décisions formeront secrètement des affinités, une expression et un coût. Le jeu ne t'affichera jamais ces calculs.", color = MetahumanColors.Muted, lineHeight = 20.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MhlSecondaryButton(if (step == 0) "Retour" else "Précédent", { if (step == 0) onBack() else step-- }, Modifier.weight(1f))
                MhlPrimaryButton(if (step < 2) "Suivant" else "Commencer à 18 ans", { if (step < 2) step++ else onStart(d) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductionHeadline(title: String, body: String) {
    Text(title, color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 30.sp)
    Spacer(Modifier.height(8.dp))
    Text(body, color = MetahumanColors.Muted, lineHeight = 21.sp)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ProductionOptionStrip(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(14.dp))
    MhlSectionTitle(title, MetahumanColors.DeepBlue)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { option -> FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option) }) }
    }
}

@Composable
private fun ProductionAliasScreen(c: Campaign, onConfirm: (String) -> Unit) {
    var alias by remember { mutableStateOf("") }
    val profile = powerVisualProfile(c.powerFamily)
    MhlSceneFrame("alias-${c.seed}", MotionBoard.AWAKENING, MetahumanMotionLevel.MOTION_MAJOR, Modifier.fillMaxSize(), profile.accent) {
        MhlBoardTexture(MotionBoard.AURA, Modifier.matchParentSize(), profile.accent, .12f)
        Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MhlProductionAsset(profile.iconKey, "Symbole du pouvoir révélé", size = 150.dp, pulse = true)
            }
            Text("APRÈS L'ÉVEIL", color = MetahumanColors.Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("LE MONDE N'A PAS ENCORE DE NOM POUR TOI", color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 31.sp, lineHeight = 33.sp)
            Spacer(Modifier.height(12.dp))
            MhlComicPanel(accent = profile.accent) {
                Text("POUVOIR RÉVÉLÉ · ${c.powerFamily}", fontWeight = FontWeight.Black, color = MetahumanColors.WarmGold)
                Text(c.powerRevealText, color = MetahumanColors.Ivory, lineHeight = 21.sp)
                Spacer(Modifier.height(8.dp))
                Text("COÛT · ${c.weakness}", color = MetahumanColors.Red, fontWeight = FontWeight.Bold)
                Text(c.powerCostText, color = MetahumanColors.Muted, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(alias, { if (it.length <= 28) alias = it }, label = { Text("Nom de terrain / alias") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            MhlPrimaryButton("Prendre ce nom", { onConfirm(alias) }, Modifier.fillMaxWidth(), alias.trim().isNotBlank())
        }
    }
}

@Composable
private fun ProductionCareerShell(
    c: Campaign,
    annualState: AnnualActionState,
    screen: String,
    outcome: String?,
    lastEvent: EventNode?,
    saveFeedbackKey: Int,
    onScreen: (String) -> Unit,
    onContinue: () -> Unit,
    onChoice: (EventNode, Choice) -> Unit,
    onAnnualAction: (AnnualActionCard) -> AnnualActionResult?,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onRestart: () -> Unit
) {
    var confirmRestart by remember { mutableStateOf(false) }
    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Recommencer cette destinée ?") },
            text = { Text("La carrière actuelle sera abandonnée. Le Hall of Legacies sera conservé.") },
            confirmButton = { TextButton(onClick = { confirmRestart = false; onRestart() }) { Text("RECOMMENCER") } },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("ANNULER") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        ProductionCareerHeader(
            c = c,
            onHome = onHome,
            onSettings = onSettings,
            onRestart = { confirmRestart = true }
        )
        Box(Modifier.weight(1f)) {
            when (screen) {
                "PERSONNAGE" -> ProductionCharacterScreen(c)
                "MONDE" -> ProductionWorldScreen(c)
                "LIENS" -> ProductionLinksScreen(c)
                "CHRONIQUE" -> ProductionTimelineScreen(c)
                "ACTIONS" -> ProductionAnnualActionsScreen(c, annualState, onAnnualAction) { onScreen("DESTIN") }
                else -> ProductionDestinyScreen(c, outcome, lastEvent, onContinue, onChoice)
            }
            MhlStatChangePulse(c, Modifier.matchParentSize())
            if (saveFeedbackKey > 0) MhlSaveFeedback(saveFeedbackKey, Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
        ProductionNavigation(
            screen = screen,
            c = c,
            annualState = annualState,
            actionsEnabled = outcome == null,
            onScreen = onScreen
        )
    }
}

@Composable
private fun ProductionCareerHeader(
    c: Campaign,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onRestart: () -> Unit
) {
    Row(Modifier.fillMaxWidth().background(Color(0xF2080B10)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        MhlProductionAsset(if (c.powerRevealed) powerVisualProfile(c.powerFamily).iconKey else civilProgressIcon(c.turn), "État de la destinée", size = 50.dp)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(c.alias.ifBlank { c.name }.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("${c.age} ANS · ${c.phaseLabel} · ${if (c.powerRevealed) c.scope.label else "CIVIL"}", color = MetahumanColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            TextButton(onClick = onHome, contentPadding = PaddingValues(3.dp)) { Text("ACCUEIL", fontSize = 9.sp) }
            Row {
                TextButton(onClick = onSettings, contentPadding = PaddingValues(3.dp)) { Text("RÉGLAGES", fontSize = 9.sp) }
                TextButton(onClick = onRestart, contentPadding = PaddingValues(3.dp)) { Text("RESET", fontSize = 9.sp, color = MetahumanColors.Red) }
            }
        }
    }
}

@Composable
private fun ProductionNavigation(
    screen: String,
    c: Campaign,
    annualState: AnnualActionState,
    actionsEnabled: Boolean,
    onScreen: (String) -> Unit
) {
    val remaining = annualState.synced(c).remaining
    NavigationBar(containerColor = Color(0xFF0A0E14), tonalElevation = 0.dp) {
        listOf(
            Triple("DESTIN", "alt_01", "Destin"),
            Triple("ACTIONS", "alt_04", "Agir $remaining"),
            Triple("PERSONNAGE", "alt_02", "Perso"),
            Triple("MONDE", "scope_world", "Monde"),
            Triple("LIENS", "relation_family", "Liens"),
            Triple("CHRONIQUE", "alt_03", "Chronique")
        ).forEach { (item, icon, label) ->
            NavigationBarItem(
                selected = screen == item,
                enabled = item != "ACTIONS" || actionsEnabled,
                onClick = { onScreen(item) },
                icon = { MhlProductionAsset(icon, label, size = 24.dp) },
                label = { Text(label, fontSize = 8.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0x332E83FF),
                    selectedTextColor = MetahumanColors.Gold,
                    unselectedTextColor = MetahumanColors.Muted,
                    disabledTextColor = MetahumanColors.Muted.copy(alpha = .45f),
                    disabledIconColor = MetahumanColors.Muted.copy(alpha = .35f)
                )
            )
        }
    }
}
