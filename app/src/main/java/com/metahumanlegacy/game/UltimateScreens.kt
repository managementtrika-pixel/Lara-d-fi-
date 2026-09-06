package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun UltimateRootBackdrop(campaign: Campaign?, state: UltimateState?, scene: String, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05080D))
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        MhlProductionBackdrop(scene, campaign?.seed ?: 41L, Modifier.matchParentSize())
        if (campaign != null && state != null && scene in listOf("VILLE", "MONDE")) {
            UltimateCityArtwork(campaign, state, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .58f)))
        }
        content()
    }
}

@Composable
internal fun UltimateHomeScreen(
    campaign: Campaign?,
    state: UltimateState?,
    hallCount: Int,
    onContinue: () -> Unit,
    onNew: () -> Unit,
    onHall: () -> Unit,
    onSettings: () -> Unit
) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Commencer une nouvelle vie ?") },
            text = { Text("La destinée en cours sera abandonnée. Le Hall of Legacies restera intact.") },
            confirmButton = { TextButton(onClick = { confirm = false; onNew() }) { Text("NOUVELLE VIE") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("ANNULER") } }
        )
    }
    MhlSceneFrame("ultimate-home-${campaign?.seed}", MotionBoard.PANEL_TRANSITION, MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), UltimateGold) {
        Column(
            Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSettings) { Text("RÉGLAGES", color = UltimateMuted, fontSize = 10.sp) }
            }
            MhlProductionAsset("brand_hero", "Emblème MetaHuman Legacy", size = 112.dp, pulse = true)
            Text("METAHUMAN", color = UltimateMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 5.sp)
            Text("LEGACY", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 48.sp)
            Text("UNE VIE QUI LAISSE DES TRACES", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(15.dp))
            if (campaign != null && state != null) {
                UltimateHeroBanner(campaign, state, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                UltimatePanel(Modifier.fillMaxWidth(), accent = powerVisualProfile(campaign.powerFamily).accent) {
                    Text("TA DESTINÉE CONTINUE", color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text("${campaign.city} · ${state.cityArchetype} · ${state.climate}", color = UltimateIvory, fontWeight = FontWeight.Black)
                    Text("${state.mediaFrame} · ${state.legalStatus} · ${state.homeLabel()}", color = UltimateMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                MhlPrimaryButton("Continuer", onContinue, Modifier.fillMaxWidth())
                Spacer(Modifier.height(7.dp))
                MhlSecondaryButton("Nouvelle vie", { confirm = true }, Modifier.fillMaxWidth())
            } else {
                UltimatePanel(Modifier.fillMaxWidth(), accent = UltimateBlue) {
                    Text("À 18 ans, tu choisis une personne et une ville — pas un pouvoir.", color = UltimateIvory, fontWeight = FontWeight.Black)
                    Text("Apparence, style civil, environnement et personnalité donnent une identité au départ. Les dix premières décisions construisent ensuite secrètement l'éveil.", color = UltimateMuted, lineHeight = 19.sp)
                }
                Spacer(Modifier.height(12.dp))
                MhlPrimaryButton("Commencer une vie", onNew, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(7.dp))
            MhlSecondaryButton("Hall of Legacies · $hallCount", onHall, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun UltimateCreateScreen(
    draft: UltimateCreationDraft,
    onDraft: (UltimateCreationDraft) -> Unit,
    onRandomize: () -> Unit,
    onBack: () -> Unit,
    onStart: (UltimateCreationDraft) -> Unit
) {
    UltimateCharacterCreatorV2(
        draft = draft,
        onDraft = onDraft,
        onRandomize = onRandomize,
        onBack = onBack,
        onStart = onStart
    )
}

@Composable
private fun OptionStrip(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(11.dp))
    Text(title, color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    Spacer(Modifier.height(5.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option -> FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option, fontSize = 11.sp) }) }
    }
}

@Composable
internal fun UltimateAliasScreen(c: Campaign, state: UltimateState, onConfirm: (String, String, String, String) -> Unit) {
    var alias by remember { mutableStateOf("") }
    var presentation by remember { mutableStateOf(if (state.heroPresentation == "À découvrir") "Sobre" else state.heroPresentation) }
    var palette by remember { mutableStateOf(if (state.costumePalette == "Non définie") "Personnalisée au pouvoir" else state.costumePalette) }
    var mask by remember { mutableStateOf(if (state.maskStyle == "Aucun") "Masque minimal" else state.maskStyle) }
    val preview = state.copy(heroPresentation = presentation, costumePalette = palette, maskStyle = mask, costumeEra = 1)
    val profile = powerVisualProfile(c.powerFamily)
    MhlSceneFrame("ultimate-alias-${c.seed}-$presentation-$palette-$mask", MotionBoard.AWAKENING, MetahumanMotionLevel.MOTION_MAJOR, Modifier.fillMaxSize(), profile.accent) {
        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
            UltimateSectionHeader("Après l'éveil", "Construire une identité", "Tu choisis le nom et une première manière d'exister publiquement. Le costume continuera d'évoluer avec la carrière.", profile.accent)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                UltimatePortrait(c, preview, Modifier.width(188.dp).height(250.dp).clip(CutCornerShape(18.dp)), heroMode = true)
            }
            Spacer(Modifier.height(10.dp))
            UltimatePanel(accent = profile.accent) {
                Text("${c.powerFamily.uppercase()} · COÛT : ${c.weakness.uppercase()}", color = profile.accent, fontWeight = FontWeight.Black, fontSize = 10.sp)
                Text(c.powerRevealText, color = UltimateIvory, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(alias, { if (it.length <= 28) alias = it }, label = { Text("Alias / nom de terrain") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OptionStrip("PRÉSENCE", UltimateCatalog.heroPresentations, presentation) { presentation = it }
            OptionStrip("PALETTE", UltimateCatalog.costumePalettes, palette) { palette = it }
            OptionStrip("MASQUE", UltimateCatalog.maskStyles, mask) { mask = it }
            LibraryAliasCostumePresetStrip(presentation, palette, mask) { preset ->
                presentation = preset.presentation
                palette = preset.palette
                mask = preset.mask
            }
            Spacer(Modifier.height(12.dp))
            MhlPrimaryButton("Prendre cette identité", { onConfirm(alias.trim(), presentation, palette, mask) }, Modifier.fillMaxWidth(), alias.trim().isNotBlank())
        }
    }
}

@Composable
internal fun UltimateCareerShell(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    screen: String,
    outcome: String?,
    savePulse: Int,
    onScreen: (String) -> Unit,
    onContinue: () -> Unit,
    onChoice: (EventNode, Choice) -> Unit,
    onAction: (AnnualActionCard) -> AnnualActionResult?,
    onStateChange: (UltimateState) -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onRestart: () -> Unit
) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Recommencer cette destinée ?") },
            text = { Text("La carrière, la ville vivante et la personnalisation de cette vie seront effacées. Le Hall restera conservé.") },
            confirmButton = { TextButton(onClick = { confirm = false; onRestart() }) { Text("RECOMMENCER") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("ANNULER") } }
        )
    }
    Column(Modifier.fillMaxSize()) {
        UltimateCareerHeader(c, state, annual, savePulse, onHome, onSettings) { confirm = true }
        Box(Modifier.weight(1f)) {
            when (screen) {
                "ACTIONS" -> UltimateActionsScreen(c, state, annual, onAction)
                "PERSONNAGE" -> UltimateCharacterScreen(c, state, onStateChange)
                "VILLE" -> UltimateCityScreen(c, state)
                "LIENS" -> UltimateLinksScreen(c, state)
                "CHRONIQUE" -> UltimateChronicleScreen(c, state)
                else -> UltimateDestinyScreen(c, state, annual, outcome, onContinue, onChoice)
            }
        }
        UltimateNavigation(screen, annual.synced(c).remaining, onScreen)
    }
}

@Composable
private fun UltimateCareerHeader(c: Campaign, state: UltimateState, annual: AnnualActionState, savePulse: Int, onHome: () -> Unit, onSettings: () -> Unit, onReset: () -> Unit) {
    val accent = if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue
    Row(
        Modifier.fillMaxWidth().background(Color(0xF405090E)).border(1.dp, accent.copy(alpha = .35f)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MhlProductionAsset(if (c.powerRevealed) powerIcon(c.powerFamily) else civilProgressIcon(c.turn), "Identité", size = 43.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(c.alias.ifBlank { c.name }.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text("${c.age} ANS · ${c.phaseLabel} · ${if (c.powerRevealed) c.scope.label else "CIVIL"}", color = UltimateMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            if (c.powerRevealed) Text("${state.mediaFrame} · ${state.legalStatus}", color = accent, fontSize = 8.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (savePulse > 0) Text("SAUVEGARDÉ", color = UltimateGreen, fontSize = 7.sp, fontWeight = FontWeight.Black)
            Text("AGIR ${annual.synced(c).remaining}/$ANNUAL_ACTION_LIMIT", color = UltimateGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Row {
                TextButton(onClick = onHome, contentPadding = PaddingValues(2.dp)) { Text("ACCUEIL", fontSize = 8.sp) }
                TextButton(onClick = onSettings, contentPadding = PaddingValues(2.dp)) { Text("⚙", fontSize = 11.sp) }
                TextButton(onClick = onReset, contentPadding = PaddingValues(2.dp)) { Text("↻", color = UltimateRed, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun UltimateNavigation(screen: String, remaining: Int, onScreen: (String) -> Unit) {
    val items = listOf(
        Triple("DESTIN", "alt_01", "Destin"),
        Triple("ACTIONS", "alt_04", "Agir $remaining"),
        Triple("PERSONNAGE", "alt_02", "Perso"),
        Triple("VILLE", "scope_city", "Ville"),
        Triple("LIENS", "relation_family", "Liens"),
        Triple("CHRONIQUE", "alt_03", "Chronique")
    )
    NavigationBar(containerColor = Color(0xFF070B10), tonalElevation = 0.dp) {
        items.forEach { (id, icon, label) ->
            NavigationBarItem(
                selected = screen == id || (id == "DESTIN" && screen !in items.map { it.first }),
                onClick = { onScreen(id) },
                icon = { MhlProductionAsset(icon, label, size = 23.dp) },
                label = { Text(label, fontSize = 7.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0x333A8DFF), selectedTextColor = UltimateGold, unselectedTextColor = UltimateMuted)
            )
        }
    }
}

@Composable
private fun UltimateDestinyScreen(c: Campaign, state: UltimateState, annual: AnnualActionState, outcome: String?, onContinue: () -> Unit, onChoice: (EventNode, Choice) -> Unit) {
    val event = remember(c.seed, c.turn, state.hashCode(), annual.rescue, annual.investigation, annual.presence, annual.discipline) {
        if (outcome == null) UltimateGameEngine.event(c, state, annual) else null
    }
    val profile = powerVisualProfile(c.powerFamily)
    MhlSceneFrame("destiny-${c.seed}-${c.turn}-${outcome?.hashCode()}", if (outcome == null) MotionBoard.PANEL_TRANSITION else MotionBoard.OUTCOME_REVEAL, MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), profile.accent) {
        Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
            UltimateHeroBanner(c, state, Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                UltimatePill(state.legalStatus, if (state.legalStatus.contains("Recherché")) UltimateRed else UltimateBlue)
                UltimatePill(state.mediaFrame, UltimateGold)
                if (c.powerRevealed) UltimatePill("Surcharge ${state.powerStrain}%", if (state.powerStrain >= 70) UltimateRed else UltimateViolet)
                if (state.nemesis.isNotBlank()) UltimatePill("Némésis : ${state.nemesis}", UltimateRed)
            }
            Spacer(Modifier.height(10.dp))
            if (outcome != null) {
                UltimatePanel(accent = if (c.health <= 25) UltimateRed else UltimateGold) {
                    Text("CONSÉQUENCE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    Text(outcome, color = UltimateIvory, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(10.dp))
                MhlPrimaryButton(if (c.needsAlias) "Choisir ton identité" else "Continuer", onContinue, Modifier.fillMaxWidth())
            } else if (event != null) {
                if (!c.powerRevealed && c.turn < 10) {
                    UltimatePanel(accent = UltimateBlue) {
                        Text("DÉCISION FORMATIVE ${c.turn + 1}/10", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        Text("Le jeu ne montre toujours aucun calcul lié à ton futur pouvoir.", color = UltimateMuted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                UltimatePanel(accent = if (event.stakes >= 4) UltimateRed else profile.accent) {
                    Text(event.category.uppercase(), color = if (event.stakes >= 4) UltimateRed else profile.accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(event.title.uppercase(), color = UltimateIvory, fontSize = 23.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(7.dp))
                    Text(event.text, color = UltimateMuted, lineHeight = 19.sp)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        UltimatePill("Enjeu ${event.stakes}/5", if (event.stakes >= 4) UltimateRed else UltimateGold)
                        if (event.threadId != null) UltimatePill("Arc ${event.threadStage}", UltimateViolet)
                    }
                }
                Spacer(Modifier.height(10.dp))
                event.choices.forEachIndexed { index, choice ->
                    UltimateChoiceCard(index + 1, choice, event.stakes) { onChoice(event, choice) }
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}

@Composable
private fun UltimateChoiceCard(number: Int, choice: Choice, stakes: Int, onClick: () -> Unit) {
    val accent = when (choice.approach) {
        "CARE" -> UltimateGreen
        "ORDER" -> UltimateBlue
        "TRUTH" -> UltimateViolet
        "ASCEND" -> UltimateRed
        else -> UltimateGold
    }
    Column(
        Modifier.fillMaxWidth().clip(CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xF5141B25), accent.copy(alpha = .10f))))
            .border(1.dp, accent.copy(alpha = .62f), CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp))
            .padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(27.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                Text(number.toString(), color = accent, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(choice.label, color = UltimateIvory, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (choice.approach.isNotBlank()) UltimatePill(choice.approach, accent)
                    if (choice.risk > 0) UltimatePill("Risque ${choice.risk}", if (choice.risk >= 7) UltimateRed else UltimateMuted)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        MhlPrimaryButton("Choisir", onClick, Modifier.fillMaxWidth())
    }
}

@Composable
private fun UltimateActionsScreen(c: Campaign, state: UltimateState, annual: AnnualActionState, onAction: (AnnualActionCard) -> AnnualActionResult?) {
    val synced = annual.synced(c)
    var result by remember(c.turn) { mutableStateOf<AnnualActionResult?>(null) }
    var filter by remember(c.turn) { mutableStateOf<AnnualActionCategory?>(null) }
    val actions = remember(c.seed, c.turn, state.hashCode(), synced.usedIds, synced.used) { UltimateGameEngine.annualActions(c, state, synced) }
    val categories = actions.map { it.category }.distinct()
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Interludes", "Agir cette année", "${synced.remaining}/$ANNUAL_ACTION_LIMIT moments libres. Ces actions ne font jamais passer l'année.", UltimateGold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            UltimateMeter("Secours", synced.rescue, UltimateGreen, Modifier.weight(1f))
            UltimateMeter("Enquête", synced.investigation, UltimateBlue, Modifier.weight(1f))
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            UltimateMeter("Présence", synced.presence, UltimateGold, Modifier.weight(1f))
            UltimateMeter("Discipline", synced.discipline, UltimateViolet, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = if (state.powerStrain >= 70) UltimateRed else UltimateBlue) {
            Text("VIE ENTRE LES CHAPITRES", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("${state.credits} crédits · dette ${state.debt} · ${state.homeLabel()} · surcharge ${state.powerStrain}%", color = UltimateMuted, fontSize = 11.sp)
            Text("Sauvetages, entraînement, enquêtes, relations, récupération, coiffure, médias, finances et QG nourrissent les grandes histoires sans les remplacer.", color = UltimateIvory, fontSize = 11.sp, lineHeight = 16.sp)
        }
        result?.let {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = UltimateGreen) {
                Text(it.title.uppercase(), color = UltimateGreen, fontWeight = FontWeight.Black)
                Text(it.text, color = UltimateIvory, fontSize = 12.sp, lineHeight = 18.sp)
                Text("${it.state.remaining} moment(s) libre(s) restant(s).", color = UltimateMuted, fontSize = 10.sp)
            }
        }
        if (!c.powerRevealed && c.turn >= 10) {
            Spacer(Modifier.height(10.dp))
            UltimatePanel(accent = UltimateViolet) {
                Text("L'ÉVEIL PREND TOUTE LA PLACE", color = UltimateViolet, fontWeight = FontWeight.Black)
                Text("Aucun interlude ne peut s'insérer dans ce moment. Reviens au Destin.", color = UltimateMuted)
            }
            return@Column
        }
        if (synced.remaining <= 0) {
            Spacer(Modifier.height(10.dp))
            UltimatePanel(accent = UltimateGold) {
                Text("ANNÉE BIEN REMPLIE", color = UltimateGold, fontWeight = FontWeight.Black)
                Text("La grande décision du Destin fera avancer le temps et rouvrira trois nouveaux moments libres.", color = UltimateMuted)
            }
            return@Column
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Tout") })
            categories.forEach { cat -> FilterChip(selected = filter == cat, onClick = { filter = cat }, label = { Text(cat.label) }) }
        }
        Spacer(Modifier.height(8.dp))
        actions.filter { filter == null || it.category == filter }.forEach { card ->
            val accent = when (card.category) {
                AnnualActionCategory.INTERVENTION -> UltimateRed
                AnnualActionCategory.INVESTIGATION -> UltimateBlue
                AnnualActionCategory.RELATION -> UltimateGold
                AnnualActionCategory.TRAINING -> UltimateViolet
                AnnualActionCategory.RECOVERY -> UltimateGreen
                AnnualActionCategory.PUBLIC -> Color(0xFFFFA64D)
                else -> UltimateMuted
            }
            UltimatePanel(accent = accent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MhlProductionAsset(card.iconKey, card.title, size = 48.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(card.category.label.uppercase(), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Text(card.title, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(card.description, color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp)
                        Text("Développe · ${card.focus}", color = UltimateGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(7.dp))
                MhlPrimaryButton("Faire cette action", {
                    val r = onAction(card)
                    if (r != null) result = r
                }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun UltimateCharacterScreen(c: Campaign, state: UltimateState, onStateChange: (UltimateState) -> Unit) {
    val profile = powerVisualProfile(c.powerFamily)
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Dossier personnage", c.alias.ifBlank { c.name }, "Ton apparence vieillit, ton costume traverse plusieurs époques et tes blessures restent dans ton histoire.", profile.accent)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            UltimatePortrait(c, state, Modifier.width(210.dp).height(278.dp).clip(CutCornerShape(18.dp)), heroMode = c.powerRevealed)
        }
        Spacer(Modifier.height(9.dp))
        UltimatePanel(accent = profile.accent) {
            Text("APPARENCE ACTUELLE", color = profile.accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("${state.ageAppearance(c)} · ${state.bodyBuild} · ${state.stature} · ${state.skinTone}", color = UltimateIvory, fontWeight = FontWeight.Black)
            Text("${state.faceShape} · ${state.hair} ${state.hairColor.lowercase()} · ${state.facialHair} · yeux ${state.eyes.lowercase()}\nStyle civil : ${state.civilianStyle} · ${state.accessory}", color = UltimateMuted, fontSize = 11.sp, lineHeight = 17.sp)
            if (state.injuries.isNotEmpty()) Text("Cicatrices / séquelles : ${state.injuries.joinToString(" · ")}", color = UltimateRed, fontSize = 10.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("PERSONNALISATION CIVILE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            UltimateActionTile("Coiffure", state.hair, UltimateBlue, onClick = { onStateChange(state.copy(hair = cycle(UltimateCatalog.hairs, state.hair))) })
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.weight(1f)) { UltimateActionTile("Barbe", state.facialHair, UltimateMuted, onClick = { onStateChange(state.copy(facialHair = cycle(UltimateCatalog.facialHairs, state.facialHair))) }) }
            Box(Modifier.weight(1f)) { UltimateActionTile("Style", state.civilianStyle, UltimateMuted, onClick = { onStateChange(state.copy(civilianStyle = cycle(UltimateCatalog.civilianStyles, state.civilianStyle))) }) }
        }
        if (c.powerRevealed) {
            Spacer(Modifier.height(12.dp))
            Text("IDENTITÉ MÉTAHUMAINE", color = profile.accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Spacer(Modifier.height(5.dp))
            UltimatePanel(accent = profile.accent) {
                Text("ÈRE ${state.costumeEra.coerceAtLeast(1)} · ${state.heroPresentation.uppercase()}", color = profile.accent, fontWeight = FontWeight.Black)
                Text("${state.costumePalette} · ${state.maskStyle} · ${state.emblem}\nObjet signature : ${state.signatureItem}\nSpécialité : ${state.powerBranch} · Combat : ${state.combatStyle}", color = UltimateMuted, fontSize = 11.sp, lineHeight = 17.sp)
                if (state.techniques.isNotEmpty()) Text("Techniques : ${state.techniques.joinToString(" · ")}", color = UltimateGold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(6.dp))
            LibraryCostumePresetStrip(state, state.costumeEra.coerceAtLeast(1), onStateChange)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.weight(1f)) { UltimateActionTile("Présence", state.heroPresentation, profile.accent, onClick = { onStateChange(state.copy(heroPresentation = cycle(UltimateCatalog.heroPresentations, state.heroPresentation))) }) }
                Box(Modifier.weight(1f)) { UltimateActionTile("Palette", state.costumePalette, UltimateGold, onClick = { onStateChange(state.copy(costumePalette = cycle(UltimateCatalog.costumePalettes, state.costumePalette))) }) }
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.weight(1f)) { UltimateActionTile("Masque", state.maskStyle, UltimateViolet, onClick = { onStateChange(state.copy(maskStyle = cycle(UltimateCatalog.maskStyles, state.maskStyle))) }) }
                Box(Modifier.weight(1f)) { UltimateActionTile("Symbole", state.emblem, UltimateGold, enabled = state.costumeEra >= 2, onClick = { onStateChange(state.copy(emblem = cycle(UltimateCatalog.emblems, state.emblem))) }) }
            }
            Spacer(Modifier.height(5.dp))
            UltimateActionTile("Objet signature", state.signatureItem, UltimateBlue, enabled = state.costumeEra >= 2, onClick = { onStateChange(state.copy(signatureItem = cycle(UltimateCatalog.signatureItems, state.signatureItem))) })
        }
        Spacer(Modifier.height(12.dp))
        UltimatePanel(accent = if (state.powerStrain >= 70) UltimateRed else UltimateViolet) {
            Text("CORPS & POUVOIR", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp)
            if (c.powerRevealed) {
                UltimateMeter("Puissance", c.power, profile.accent)
                Spacer(Modifier.height(5.dp)); UltimateMeter("Maîtrise", c.control, UltimateBlue)
                Spacer(Modifier.height(5.dp)); UltimateMeter("Surcharge", state.powerStrain, if (state.powerStrain >= 70) UltimateRed else UltimateViolet)
            }
            Spacer(Modifier.height(5.dp)); UltimateMeter("Santé", c.health, UltimateGreen)
        }
    }
}

@Composable
private fun UltimateCityScreen(c: Campaign, state: UltimateState) {
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Monde vivant", c.city, "La ville se souvient de tes crises, de ses reconstructions et de la façon dont chaque quartier te voit.", UltimateBlue)
        Spacer(Modifier.height(8.dp))
        UltimateCityArtwork(c, state, Modifier.fillMaxWidth().height(220.dp).clip(CutCornerShape(18.dp)))
        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateGold) {
            Text("IDENTITÉ URBAINE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("${state.cityArchetype} · ${state.architecture}", color = UltimateIvory, fontWeight = FontWeight.Black)
            Text("${state.climate} · ${state.cityMood}\nÉtat général ${state.cityCondition}% · technologie ${state.cityTech}%", color = UltimateMuted, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            UltimateMeter("État de la ville", state.cityCondition, if (state.cityCondition >= 55) UltimateGreen else UltimateRed)
            Spacer(Modifier.height(5.dp)); UltimateMeter("Évolution technologique", state.cityTech, UltimateBlue)
        }
        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = if (state.legalStatus.contains("Recherché")) UltimateRed else UltimateBlue) {
            Text("INSTITUTIONS & MONDE", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("Statut : ${state.legalStatus}\nLoi : ${state.metaLaw}\nInternational : ${state.internationalAttention}%\nMédias : ${state.mediaFrame}\nJournaliste récurrent : ${state.journalist.ifBlank { "Aucun" }}", color = UltimateMuted, fontSize = 11.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text("QUARTIERS", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        state.districts.forEach { d ->
            UltimatePanel(accent = when { d.sentiment >= 35 -> UltimateGreen; d.sentiment <= -35 -> UltimateRed; else -> UltimateBlue }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(d.name.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black)
                    if (d.restricted) UltimatePill("Zone restreinte", UltimateRed)
                }
                Text("${if (d.faction == "Aucune") "Aucune faction dominante" else d.faction}${if (d.landmark.isNotBlank()) " · ${d.landmark}" else ""}", color = UltimateMuted, fontSize = 10.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(5.dp))
                UltimateMeter("Réputation locale", d.sentiment, if (d.sentiment >= 0) UltimateGreen else UltimateRed, rangeMin = -100, rangeMax = 100)
                Spacer(Modifier.height(4.dp)); UltimateMeter("Dégâts", d.damage, UltimateRed)
                Spacer(Modifier.height(4.dp)); UltimateMeter("Criminalité", d.crime, UltimateViolet)
                Spacer(Modifier.height(4.dp)); UltimateMeter("Reconstruction", d.reconstruction, UltimateGold)
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(5.dp))
        UltimatePanel(accent = UltimateGold) {
            Text("LOGEMENT / QG", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("${state.homeLabel()} · ${state.baseType}", color = UltimateIvory, fontWeight = FontWeight.Black)
            Text("Le refuge évolue avec l'exposition, les moyens et l'influence. Les souvenirs de carrière y deviennent progressivement des objets d'histoire.", color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun UltimateLinksScreen(c: Campaign, state: UltimateState) {
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Relations", "Les gens ont leur propre vie", "Une personne peut t'aimer, t'admirer et avoir peur de toi en même temps.", UltimateGold)
        Spacer(Modifier.height(8.dp))
        if (state.mentor.isNotBlank() || state.protege.isNotBlank() || state.romance.isNotBlank() || state.nemesis.isNotBlank()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (state.mentor.isNotBlank()) UltimatePill("Mentor · ${state.mentor}", UltimateBlue)
                if (state.protege.isNotBlank()) UltimatePill("Protégé · ${state.protege}", UltimateGreen)
                if (state.romance.isNotBlank()) UltimatePill("Relation · ${state.romance}", UltimateGold)
                if (state.nemesis.isNotBlank()) UltimatePill("Némésis · ${state.nemesis}", UltimateRed)
            }
            Spacer(Modifier.height(8.dp))
        }
        state.relations.sortedByDescending { it.trust + it.affection + it.admiration }.forEach { rel ->
            val accent = when (rel.id) { "rival" -> UltimateRed; "journalist" -> UltimateBlue; "mentor" -> UltimateViolet; "protege" -> UltimateGreen; else -> UltimateGold }
            UltimatePanel(accent = accent) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(rel.name.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black)
                        Text("${rel.role} · ${rel.status} · env. ${c.age + rel.ageOffset} ans", color = UltimateMuted, fontSize = 10.sp)
                    }
                    if (rel.knowsIdentity) UltimatePill("Connaît l'identité", UltimateRed)
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UltimateMeter("Confiance", rel.trust, UltimateBlue, Modifier.weight(1f))
                    UltimateMeter("Affection", rel.affection, UltimateGold, Modifier.weight(1f))
                }
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UltimateMeter("Peur", rel.fear, UltimateRed, Modifier.weight(1f))
                    UltimateMeter("Admiration", rel.admiration, UltimateGreen, Modifier.weight(1f))
                }
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UltimateMeter("Rancune", rel.grudge, UltimateViolet, Modifier.weight(1f))
                    UltimateMeter("Dépendance", rel.dependence, UltimateMuted, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun UltimateChronicleScreen(c: Campaign, state: UltimateState) {
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Archives", "Chronique de la destinée", "Événements, portraits d'époque, dossiers, techniques, blessures et objets iconiques composent une vraie biographie.", UltimateViolet)
        Spacer(Modifier.height(8.dp))
        if (state.snapshots.isNotEmpty()) {
            UltimatePanel(accent = UltimateGold) {
                Text("PORTRAITS D'ÉPOQUE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                state.snapshots.forEach { snap ->
                    val p = snap.split('|')
                    Text("${p.getOrElse(0) { "?" }} ans · ${p.getOrElse(1) { "Étape" }} · ${p.getOrElse(2) { "" }} · ${p.getOrElse(3) { "" }}", color = UltimateMuted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(7.dp))
        }
        if (state.cases.isNotEmpty()) {
            UltimatePanel(accent = UltimateBlue) {
                Text("ENQUÊTES", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                state.cases.forEach { x ->
                    Text("${if (x.solved) "✓" else "○"} ${x.title} · étape ${x.stage}/${x.maxStage} · preuves ${x.evidence}%${if (x.falseLead && x.solved) " · fausse piste corrigée" else ""}", color = if (x.solved) UltimateGreen else UltimateMuted, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
            Spacer(Modifier.height(7.dp))
        }
        if (state.techniques.isNotEmpty() || state.injuries.isNotEmpty() || state.iconicItems.isNotEmpty()) {
            UltimatePanel(accent = powerVisualProfile(c.powerFamily).accent) {
                if (state.techniques.isNotEmpty()) { Text("TECHNIQUES SIGNATURE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp); Text(state.techniques.joinToString(" · "), color = UltimateIvory, fontSize = 11.sp) }
                if (state.injuries.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text("SÉQUELLES", color = UltimateRed, fontWeight = FontWeight.Black, fontSize = 9.sp); Text(state.injuries.joinToString("\n"), color = UltimateMuted, fontSize = 10.sp) }
                if (state.iconicItems.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text("OBJETS / ÉPOQUES", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp); Text(state.iconicItems.joinToString("\n"), color = UltimateMuted, fontSize = 10.sp) }
            }
            Spacer(Modifier.height(7.dp))
        }
        if (state.rareMarks.isNotEmpty()) {
            UltimatePanel(accent = UltimateViolet) {
                Text("ANOMALIES RARES", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp)
                state.rareMarks.forEach { Text("• $it", color = UltimateMuted, fontSize = 10.sp, lineHeight = 15.sp) }
            }
            Spacer(Modifier.height(7.dp))
        }
        UltimatePanel(accent = UltimateBlue) {
            Text("HISTOIRE", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
            c.timeline.takeLast(100).reversed().forEach { line ->
                Text(line, color = UltimateMuted, fontSize = 10.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@Composable
internal fun UltimateFinalScreen(c: Campaign, state: UltimateState, onArchive: () -> Unit) {
    val profile = powerVisualProfile(c.powerFamily)
    MhlSceneFrame("ultimate-final-${c.seed}", MotionBoard.LEGACY, MetahumanMotionLevel.MOTION_LEGENDARY, Modifier.fillMaxSize(), profile.accent) {
        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
            UltimateSectionHeader("Legacy", GameEngine.legacyTitle(c), "${c.age} ans · ${c.scope.label} · score ${GameEngine.legacyScore(c)}", UltimateGold)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UltimatePortrait(c, state, Modifier.weight(1f).height(240.dp).clip(CutCornerShape(18.dp)), heroMode = c.powerRevealed)
                UltimateCityArtwork(c, state, Modifier.weight(1f).height(240.dp).clip(CutCornerShape(18.dp)))
            }
            Spacer(Modifier.height(9.dp))
            UltimatePanel(accent = UltimateGold) {
                Text("CE QUE LE MONDE GARDE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text(UltimateDirector.legacySummary(c, state), color = UltimateIvory, lineHeight = 20.sp)
                Spacer(Modifier.height(7.dp))
                Text(GameEngine.legacySummary(c), color = UltimateMuted, fontSize = 11.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = profile.accent) {
                Text("ARCHIVE DE CARRIÈRE", color = profile.accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text("Costume : ère ${state.costumeEra} · ${state.heroPresentation} · ${state.costumePalette}\nNémésis : ${state.nemesis.ifBlank { "Aucune" }} · Mentor : ${state.mentor.ifBlank { "Aucun" }} · Protégé : ${state.protege.ifBlank { "Aucun" }}\nTechniques : ${state.techniques.size} · blessures nommées : ${state.injuries.size} · dossiers résolus : ${state.cases.count { it.solved }}\nVille : ${state.cityCondition}% · attention internationale : ${state.internationalAttention}%", color = UltimateMuted, fontSize = 11.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.height(10.dp))
            MhlPrimaryButton("Archiver dans le Hall", onArchive, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun UltimateHallScreen(hall: List<String>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Musée personnel", "Hall of Legacies", "Chaque vie conserve au minimum son titre, sa portée, sa ville et la silhouette publique qu'elle a laissée.", UltimateGold)
        Spacer(Modifier.height(8.dp))
        if (hall.isEmpty()) {
            UltimatePanel(accent = UltimateBlue) { Text("Aucune destinée archivée pour l'instant.", color = UltimateMuted) }
        } else hall.forEachIndexed { index, raw ->
            val p = raw.split('|')
            UltimatePanel(accent = when { index == 0 -> UltimateGold; index % 3 == 1 -> UltimateBlue; else -> UltimateViolet }) {
                Text(p.getOrElse(0) { "Inconnu" }.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(p.getOrElse(1) { "Legacy" }, color = UltimateGold, fontWeight = FontWeight.Bold)
                Text("Score ${p.getOrElse(2) { "?" }} · ${p.getOrElse(3) { "?" }} · ${p.getOrElse(4) { "Ville inconnue" }}", color = UltimateMuted, fontSize = 10.sp)
                if (p.size > 5) Text("${p.getOrElse(5) { "" }} · Némésis : ${p.getOrElse(6) { "Aucune" }}", color = UltimateMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        MhlSecondaryButton("Retour", onBack, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun UltimateSettingsScreen(settings: MetahumanMotionSettings, onChange: (MetahumanMotionSettings) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Confort", "Réglages", "Le graphisme garde son énergie sans obliger les animations ou les vibrations.", UltimateBlue)
        Spacer(Modifier.height(10.dp))
        SettingToggle("Réduire les animations", "Réduit transitions, pulsations et mouvements longs.", settings.reduceMotion) { onChange(settings.copy(reduceMotion = it)) }
        SettingToggle("Haptics", "Vibrations légères sur les choix et moments majeurs.", settings.haptics) { onChange(settings.copy(haptics = it)) }
        SettingToggle("Contraste élevé", "Renforce fonds et lisibilité.", settings.highContrast) { onChange(settings.copy(highContrast = it)) }
        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateGold) {
            Text("VITESSE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = settings.speed == MetahumanMotionSpeed.NORMAL, onClick = { onChange(settings.copy(speed = MetahumanMotionSpeed.NORMAL)) }, label = { Text("Normale") })
                FilterChip(selected = settings.speed == MetahumanMotionSpeed.FAST, onClick = { onChange(settings.copy(speed = MetahumanMotionSpeed.FAST)) }, label = { Text("Rapide") })
            }
            Spacer(Modifier.height(6.dp))
            Text("TAILLE DU TEXTE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(90 to "Compact", 100 to "Normal", 115 to "Grand").forEach { (value, label) ->
                    FilterChip(selected = settings.textScalePercent == value, onClick = { onChange(settings.copy(textScalePercent = value)) }, label = { Text(label) })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        MhlSecondaryButton("Retour", onBack, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettingToggle(title: String, body: String, value: Boolean, onChange: (Boolean) -> Unit) {
    UltimatePanel(accent = if (value) UltimateGreen else UltimateMuted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black)
                Text(body, color = UltimateMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Switch(checked = value, onCheckedChange = onChange)
        }
    }
    Spacer(Modifier.height(7.dp))
}

private fun cycle(list: List<String>, current: String): String {
    if (list.isEmpty()) return current
    val index = list.indexOf(current)
    return list[(if (index < 0) 0 else index + 1) % list.size]
}
