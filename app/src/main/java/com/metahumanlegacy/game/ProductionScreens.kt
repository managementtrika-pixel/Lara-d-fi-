package com.metahumanlegacy.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun outcomeMotionBoard(event: EventNode?, text: String, c: Campaign): Int {
    val hay = "${event?.category.orEmpty()} ${event?.provocation.orEmpty()} $text".lowercase()
    return when {
        listOf("bless", "santé", "fatigue", "hôpital", "récup").any(hay::contains) -> MotionBoard.INJURY
        listOf("média", "journal", "tv", "opinion", "presse", "réseau").any(hay::contains) -> MotionBoard.MEDIA
        listOf("relation", "famille", "proche", "rival", "trah").any(hay::contains) -> MotionBoard.RELATION
        listOf("surcharge", "contrôle", "instable", "débord").any(hay::contains) -> MotionBoard.OVERLOAD
        event?.stakes?.let { it >= 3 } == true && c.powerRevealed -> MotionBoard.IMPACT
        else -> MotionBoard.OUTCOME_REVEAL
    }
}

private fun eventMotionBoard(event: EventNode, c: Campaign): Int = when {
    event.kind == "AWAKENING" -> MotionBoard.AWAKENING
    event.kind == "MAJOR" && event.stakes >= 3 -> MotionBoard.CLIMAX
    event.kind == "MAJOR" -> MotionBoard.CHAPTER
    !c.powerRevealed && c.turn in setOf(4, 7, 9) -> MotionBoard.PRE_AWAKENING
    event.category.contains("média", ignoreCase = true) -> MotionBoard.MEDIA
    event.category.contains("relation", ignoreCase = true) -> MotionBoard.RELATION
    else -> MotionBoard.PANEL_TRANSITION
}

@Composable
internal fun ProductionDestinyScreen(c: Campaign, outcome: String?, lastEvent: EventNode?, onContinue: () -> Unit, onChoice: (EventNode, Choice) -> Unit) {
    val settings = LocalMetahumanMotion.current.settings
    val haptic = rememberMetahumanHaptic()

    if (outcome != null) {
        val title = outcome.substringBefore("\n\n").ifBlank { "CONSÉQUENCE" }
        val body = outcome.substringAfter("\n\n", outcome)
        val board = outcomeMotionBoard(lastEvent, outcome, c)
        val profile = powerVisualProfile(c.powerFamily)
        val isCombat = c.powerRevealed && (lastEvent?.stakes ?: 0) >= 3
        val isClimax = isCombat && lastEvent?.kind == "MAJOR"
        var revealStage by remember(outcome) { mutableIntStateOf(if (settings.reduceMotion) 3 else 0) }

        LaunchedEffect(outcome, settings.reduceMotion, settings.speed) {
            if (!settings.reduceMotion) {
                revealStage = 0
                MetahumanAudioHooks.onImpact()
                delay(MetahumanMotionTokens.duration(90, settings).toLong())
                revealStage = 1
                if (isCombat) delay(55)
                delay(MetahumanMotionTokens.duration(150, settings).toLong())
                revealStage = 2
                delay(MetahumanMotionTokens.duration(150, settings).toLong())
                revealStage = 3
            }
        }

        val scene: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize()) {
                MhlBoardTexture(if (isClimax) MotionBoard.CLIMAX else board, Modifier.matchParentSize(), if (c.powerRevealed) profile.accent else MetahumanColors.DeepBlue, if (isCombat) .20f else .10f)
                if (isCombat) {
                    MhlImpactOverlay(outcome, profile.accent, if (isClimax) MetahumanMotionLevel.MOTION_LEGENDARY else MetahumanMotionLevel.MOTION_MAJOR, if (isClimax) MotionBoard.CLIMAX else MotionBoard.IMPACT)
                }
                Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                    AnimatedVisibility(visible = revealStage >= 1, enter = fadeIn(tween(MetahumanMotionTokens.duration(160, settings))), exit = fadeOut()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MhlProductionAsset(
                                key = when (board) {
                                    MotionBoard.INJURY -> "relation_family"
                                    MotionBoard.MEDIA -> "relation_media"
                                    MotionBoard.RELATION -> "relation_family"
                                    MotionBoard.OVERLOAD -> profile.iconKey
                                    else -> if (c.powerRevealed) profile.iconKey else civilProgressIcon(c.turn)
                                },
                                contentDescription = "Illustration de conséquence",
                                size = if (isCombat) 92.dp else 74.dp,
                                pulse = isCombat
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (c.powerRevealed) "CASE SUIVANTE" else "APRÈS CE CHOIX", color = MetahumanColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                                Text(title.uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 23.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    AnimatedVisibility(visible = revealStage >= 2, enter = fadeIn(tween(MetahumanMotionTokens.duration(230, settings)))) {
                        MhlComicPanel(accent = if (c.powerRevealed) profile.accent else MetahumanColors.DeepBlue, fill = Color(0xF5101722)) {
                            Text(body, color = MetahumanColors.Ivory, lineHeight = 24.sp, fontSize = 16.sp)
                        }
                    }
                    if (revealStage >= 3) {
                        Spacer(Modifier.height(12.dp))
                        Text("Les conséquences visibles sont racontées ici. Les variables internes et les routes cachées restent hors champ.", color = MetahumanColors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        Spacer(Modifier.height(18.dp))
                        MhlPrimaryButton(if (c.needsAlias) "Choisir ce qu'on t'appellera" else "Continuer", onContinue, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (isCombat) {
            MhlShakeContainer(outcome, if (isClimax) MhlShakeStrength.HEAVY else MhlShakeStrength.MEDIUM, Modifier.fillMaxSize()) { scene() }
        } else scene()
        return
    }

    val event = remember(c.seed, c.turn, c.flags, c.threads, c.lastCategory, c.lastApproach, c.powerFamily) { GameEngine.event(c) }
    val phase = when {
        c.turn < 10 -> "AVANT LE MASQUE · ${c.turn + 1}/10"
        c.turn == 10 -> "LE PREMIER IMPOSSIBLE"
        c.turn in 11..15 -> "PREMIERS PAS · ${c.turn - 10}/5"
        event.kind == "MAJOR" -> "CHAPITRE ${event.threadStage.coerceAtLeast(1)}"
        else -> event.category
    }
    val awakening = event.kind == "AWAKENING"
    val profile = powerVisualProfile(c.powerFamily)
    val board = eventMotionBoard(event, c)
    var chapterIntro by remember(event.id) { mutableStateOf(event.kind == "MAJOR" && event.threadStage > 0) }
    var awakeningStage by remember(event.id) { mutableIntStateOf(if (settings.reduceMotion || !awakening) 4 else 0) }
    var chosenIndex by remember(event.id) { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(event.id, awakening, settings.reduceMotion, settings.speed) {
        if (chapterIntro && !settings.reduceMotion) {
            MetahumanAudioHooks.onChapter()
            delay(MetahumanMotionTokens.duration(900, settings).toLong())
            chapterIntro = false
        } else if (settings.reduceMotion) chapterIntro = false

        if (awakening && !settings.reduceMotion) {
            MetahumanAudioHooks.onAwakening()
            awakeningStage = 0
            delay(MetahumanMotionTokens.duration(150, settings).toLong())
            awakeningStage = 1
            delay(MetahumanMotionTokens.duration(300, settings).toLong())
            awakeningStage = 2
            delay(MetahumanMotionTokens.duration(350, settings).toLong())
            awakeningStage = 3
            haptic(MetahumanMotionLevel.MOTION_MAJOR)
            delay(MetahumanMotionTokens.duration(350, settings).toLong())
            awakeningStage = 4
        }
    }

    Box(Modifier.fillMaxSize()) {
        MhlBoardTexture(
            board = when {
                awakening && awakeningStage <= 1 -> MotionBoard.PRE_AWAKENING
                awakening && awakeningStage == 2 -> MotionBoard.AWAKENING
                awakening && awakeningStage == 3 -> profile.auraBoard
                else -> board
            },
            modifier = Modifier.matchParentSize(),
            accent = if (awakening && c.powerResolved) profile.accent else MetahumanColors.DeepBlue,
            alpha = when {
                awakening -> .18f
                !c.powerRevealed && c.turn in setOf(4, 7, 9) -> .055f
                event.kind == "MAJOR" -> .12f
                else -> .06f
            }
        )

        MhlSceneFrame(event.id, board, if (awakening) MetahumanMotionLevel.MOTION_MAJOR else MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), if (awakening && c.powerResolved) profile.accent else eventAccentProduction(c, event)) {
            Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(phase, color = MetahumanColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
                    if (c.powerRevealed && event.stakes > 1) MhlProductionAsset(dangerIcon(event.stakes * 2), "Niveau de danger", size = 28.dp)
                }
                Spacer(Modifier.height(8.dp))
                MhlComicPanel(accent = if (awakening && c.powerResolved) profile.accent else eventAccentProduction(c, event), fill = if (awakening) Color(0xF5171125) else Color(0xF50F1620)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MhlProductionAsset(
                            key = when {
                                awakening && c.powerResolved && awakeningStage >= 2 -> profile.iconKey
                                awakening -> "origin_unknown"
                                !c.powerRevealed -> civilProgressIcon(c.turn)
                                event.stakes >= 3 -> dangerIcon(8)
                                else -> profile.iconKey
                            },
                            contentDescription = if (awakening) "Manifestation de l'éveil" else "Illustration de l'événement",
                            size = if (awakening) 110.dp else 78.dp,
                            pulse = awakening && awakeningStage >= 2
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            if (event.kind == "MAJOR") Text("ARC NARRATIF", color = MetahumanColors.Red, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            if (awakening) {
                                Text(when (awakeningStage) { 0 -> "QUELQUE CHOSE CLOCHE"; 1 -> "LE MONDE HÉSITE"; 2 -> "PREMIÈRE MANIFESTATION"; else -> "MA VIE VIENT DE CHANGER" }, color = if (c.powerResolved) profile.accent else MetahumanColors.Violet, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            }
                            Text(event.title.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 26.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(event.text, color = MetahumanColors.Ivory, fontSize = 16.sp, lineHeight = 24.sp)
                    if (awakening && !settings.reduceMotion && awakeningStage < 4) {
                        Spacer(Modifier.height(8.dp))
                        Text("TOUCHER POUR ACCÉLÉRER", color = MetahumanColors.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { awakeningStage = 4 })
                    }
                }

                Spacer(Modifier.height(12.dp))
                MhlSectionTitle("CHOIX", MetahumanColors.ElectricBlue)
                Spacer(Modifier.height(5.dp))
                event.choices.forEachIndexed { index, choice ->
                    ProductionChoiceCard(event, choice, index + 1, chosenIndex == index, chosenIndex >= 0 && chosenIndex != index, chosenIndex < 0) {
                        if (chosenIndex < 0) {
                            chosenIndex = index
                            haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                            MetahumanAudioHooks.onChoice()
                            scope.launch {
                                delay(MetahumanMotionTokens.duration(150, settings).toLong())
                                onChoice(event, choice)
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }
                Text(
                    when (event.kind) {
                        "FORMATIVE" -> "Ces choix restent humains. Rien ici ne révèle le pouvoir qu'ils contribuent à former."
                        "AWAKENING" -> "Le pouvoir est déjà déterminé hors de ta vue. Tu choisis seulement ta première réaction."
                        else -> "Choisis pour le sens de l'action. La couleur ne révèle jamais une route morale cachée."
                    },
                    color = MetahumanColors.Muted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }

        if (chapterIntro) {
            Box(Modifier.fillMaxSize().background(Color(0xF205070A)).clickable { chapterIntro = false }, contentAlignment = Alignment.Center) {
                MhlBoardTexture(MotionBoard.CHAPTER, Modifier.matchParentSize(), MetahumanColors.Gold, .20f)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    MhlProductionAsset("alt_07", "Carte de chapitre", size = 92.dp, pulse = true)
                    Text("CHAPITRE ${event.threadStage.coerceAtLeast(1)}", color = MetahumanColors.Gold, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text(event.title.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 30.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("TOUCHER POUR PASSER", color = MetahumanColors.Muted, fontSize = 9.sp)
                }
            }
        }
    }
}

private fun eventAccentProduction(c: Campaign, event: EventNode): Color = when {
    event.kind == "AWAKENING" -> if (c.powerResolved) powerVisualProfile(c.powerFamily).accent else MetahumanColors.Violet
    !c.powerRevealed && c.turn <= 3 -> MetahumanColors.DeepBlue
    !c.powerRevealed && c.turn <= 6 -> Color(0xFF446D9C)
    !c.powerRevealed -> MetahumanColors.Violet.copy(alpha = .85f)
    event.stakes >= 3 -> MetahumanColors.Red
    else -> MetahumanColors.ElectricBlue
}

@Composable
private fun ProductionChoiceCard(event: EventNode, choice: Choice, index: Int, selected: Boolean, dimmed: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val risk = choice.risk
    val border = if (risk >= 7) MetahumanColors.Red.copy(alpha = .72f) else Color(0xFF3B495A)
    MhlChoiceMotion(selected, dimmed, Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).background(if (selected) Color(0xFF1D2936) else Color(0xFF151C25), MetahumanButtonShape).border(if (selected) 2.dp else 1.dp, if (selected) MetahumanColors.Gold else border, MetahumanButtonShape).clickable(enabled = enabled, onClick = onClick).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(30.dp).background(Color(0xFF202B38), CutCornerShape(6.dp)), contentAlignment = Alignment.Center) { Text(index.toString(), color = MetahumanColors.Gold, fontWeight = FontWeight.Black) }
            Spacer(Modifier.size(10.dp))
            Text(choice.label, color = MetahumanColors.Ivory, fontWeight = FontWeight.Bold, lineHeight = 19.sp, modifier = Modifier.weight(1f))
            if (risk > 0 && event.kind != "AWAKENING") {
                Spacer(Modifier.size(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MhlProductionAsset(dangerIcon(risk), "Risque ${riskLabelProduction(risk)}", size = 30.dp)
                    Text(riskLabelProduction(risk), color = MetahumanColors.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun riskLabelProduction(risk: Int) = when { risk >= 8 -> "EXTRÊME"; risk >= 6 -> "ÉLEVÉ"; risk >= 4 -> "RISQUÉ"; risk >= 2 -> "INCERTAIN"; else -> "FAIBLE" }

@Composable
private fun ProductionStatBadge(icon: String, label: String, value: String, percent: Int?, accent: Color, modifier: Modifier = Modifier) {
    val settings = LocalMetahumanMotion.current.settings
    val target = (percent ?: 0).coerceIn(0, 100) / 100f
    val animated by animateFloatAsState(target, tween(MetahumanMotionTokens.duration(MetahumanMotionTokens.NORMAL, settings), easing = MetahumanMotionTokens.Standard), label = "stat-$label")
    MhlComicPanel(modifier, accent, Color(0xF518212C)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MhlProductionAsset(icon, label, size = 46.dp, pulse = percent != null)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), color = MetahumanColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                Text(value, color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 16.sp)
                if (percent != null) {
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(progress = { if (settings.reduceMotion) target else animated }, modifier = Modifier.fillMaxWidth().height(4.dp), color = accent, trackColor = Color(0xFF29313A))
                }
            }
        }
    }
}

@Composable
internal fun ProductionCharacterScreen(c: Campaign) {
    val profile = powerVisualProfile(c.powerFamily)
    MhlSceneFrame("character-${c.turn}", MotionBoard.STAT_CHANGE, MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), if (c.powerRevealed) profile.accent else MetahumanColors.DeepBlue) {
        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
            MhlSectionTitle("CHARACTER DOSSIER", MetahumanColors.Gold)
            Spacer(Modifier.height(8.dp))
            MhlComicPanel(accent = if (c.powerRevealed) profile.accent else Color(0xFF586270)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MhlProductionAsset(if (c.powerRevealed) profile.iconKey else civilProgressIcon(c.turn), "Identité du personnage", size = 88.dp, pulse = c.powerRevealed)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text(if (c.powerRevealed) c.alias.ifBlank { "Sans alias" } else "PERSONNE ORDINAIRE", color = MetahumanColors.Gold, fontWeight = FontWeight.Bold)
                        Text("${c.age} ans · ${c.city}, ${c.district}\n${c.socialBackground} · ${c.temperament}", color = MetahumanColors.Muted, lineHeight = 18.sp, fontSize = 12.sp)
                    }
                }
            }
            if (c.powerRevealed) {
                Spacer(Modifier.height(12.dp))
                MhlSectionTitle("IDENTITÉ MÉTA", profile.accent)
                Spacer(Modifier.height(7.dp))
                MhlComicPanel(accent = profile.accent) {
                    Text(c.powerFamily.uppercase(), fontWeight = FontWeight.Black, fontSize = 19.sp)
                    Text("Origine : ${c.origin}\nSignature : ${c.powerSignature}\nFaiblesse connue : ${c.weakness}\nExposition de l'identité : ${c.identityExposure}%", color = MetahumanColors.Muted, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            MhlSectionTitle("AXES INDÉPENDANTS", MetahumanColors.Gold)
            Spacer(Modifier.height(7.dp))
            ProductionStatBadge(moralIcon(c), "Moralité", c.moralLabel, (c.morality + 100) / 2, MetahumanColors.Gold)
            Spacer(Modifier.height(7.dp))
            ProductionStatBadge(prestigeIcon(c), "Prestige", c.prestige.toString(), c.prestige, MetahumanColors.WarmGold)
            Spacer(Modifier.height(7.dp))
            ProductionStatBadge("relation_media", "Opinion", signedNumber(c.opinion), (c.opinion + 100) / 2, MetahumanColors.ElectricBlue)
            if (c.powerRevealed) {
                Spacer(Modifier.height(7.dp)); ProductionStatBadge("public_fear", "Peur", c.fear.toString(), c.fear, MetahumanColors.Red)
                Spacer(Modifier.height(7.dp)); ProductionStatBadge(profile.iconKey, "Puissance", c.power.toString(), c.power, profile.accent)
                Spacer(Modifier.height(7.dp)); ProductionStatBadge(scopeIcon(c.scope), "Portée", c.scope.label, null, MetahumanColors.ElectricBlue)
                Spacer(Modifier.height(7.dp)); ProductionStatBadge("alt_04", "Maîtrise", qualitativeProduction(c.control), c.control, MetahumanColors.Green)
            }
            Spacer(Modifier.height(7.dp))
            ProductionStatBadge("relation_family", "Santé", qualitativeProduction(c.health), c.health, if (c.health < 35) MetahumanColors.Red else MetahumanColors.Green)
            if (c.health < 50) {
                Spacer(Modifier.height(10.dp))
                MhlComicPanel(accent = MetahumanColors.Red, fill = Color(0xD5181115)) {
                    Text("ÉTAT PHYSIQUE MARQUÉ", color = MetahumanColors.Red, fontWeight = FontWeight.Black)
                    Text("La fatigue et les blessures changent la façon dont la carrière se lit, sans interrompre la logique du moteur.", color = MetahumanColors.Muted, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
internal fun ProductionWorldScreen(c: Campaign) {
    val profile = powerVisualProfile(c.powerFamily)
    MhlSceneFrame("world-${c.scope}-${c.turn}", MotionBoard.PANEL_TRANSITION, MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), MetahumanColors.ElectricBlue) {
        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
            MhlSectionTitle(if (c.powerRevealed) "MONDE VIVANT" else "VILLE / ENVIRONNEMENT", MetahumanColors.ElectricBlue)
            Spacer(Modifier.height(8.dp))
            MhlComicPanel(accent = MetahumanColors.ElectricBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MhlProductionAsset(scopeIcon(c.scope), "Échelle géographique ${c.scope.label}", size = 94.dp, pulse = c.powerRevealed)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.city.uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("${c.district} · ${c.modifier}", color = MetahumanColors.Muted)
                        Text(if (c.powerRevealed) "Portée actuelle : ${c.scope.label}" else "Tu n'es encore qu'un civil dans cette ville.", color = MetahumanColors.Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!c.powerRevealed) {
                MhlComicPanel(accent = Color(0xFF526073)) {
                    Text("RIEN N'ANNONCE ENCORE UNE LÉGENDE", fontWeight = FontWeight.Black)
                    Text("Le monde mémorise déjà tes proches, blessures et décisions, mais aucune institution ne te traite comme un héros ou un vilain.", color = MetahumanColors.Muted, lineHeight = 20.sp)
                }
            } else {
                MhlBoardTexture(MotionBoard.MEDIA, Modifier.fillMaxWidth().height(54.dp), profile.accent, .08f)
                MhlSectionTitle("DOSSIERS D'INFLUENCE", MetahumanColors.Gold)
                Spacer(Modifier.height(7.dp))
                ProductionStatBadge("alt_05", "Gouvernement", standingProduction(c.governmentStanding), (c.governmentStanding + 100) / 2, MetahumanColors.DeepBlue)
                Spacer(Modifier.height(7.dp)); ProductionStatBadge("alt_06", "Factions", standingProduction(c.factionStanding), (c.factionStanding + 100) / 2, MetahumanColors.Violet)
                Spacer(Modifier.height(7.dp)); ProductionStatBadge("relation_media", "Médias", standingProduction(c.mediaStanding), (c.mediaStanding + 100) / 2, MetahumanColors.ElectricBlue)
                Spacer(Modifier.height(12.dp)); MhlSectionTitle("ARCS ACTIFS", MetahumanColors.Red); Spacer(Modifier.height(7.dp))
                if (c.threads.isEmpty()) Text("Aucun fil narratif majeur n'est ouvert.", color = MetahumanColors.Muted)
                c.threads.forEach { thread ->
                    MhlComicPanel(accent = if (thread.intensity >= 3) MetahumanColors.Red else MetahumanColors.DeepBlue) {
                        Text(thread.id, fontWeight = FontWeight.Black)
                        Text("Chapitre ${thread.stage} · Intensité ${thread.intensity}/3", color = MetahumanColors.Muted)
                    }
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}

@Composable
internal fun ProductionLinksScreen(c: Campaign) {
    MhlSceneFrame("links-${c.turn}-${c.familyBond}", MotionBoard.RELATION, MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), MetahumanColors.Gold) {
        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
            MhlSectionTitle("LIENS PERSISTANTS", MetahumanColors.Gold)
            Spacer(Modifier.height(8.dp))
            ProductionStatBadge("relation_family", "Famille / proches", bondProduction(c.familyBond), c.familyBond, MetahumanColors.Gold)
            Spacer(Modifier.height(8.dp))
            MhlComicPanel(accent = MetahumanColors.Gold) {
                Text("TES PROCHES", fontWeight = FontWeight.Black)
                Text(when { c.familyBond >= 75 -> "Ils occupent une place centrale dans ta vie. Ce lien peut devenir une force autant qu'une vulnérabilité."; c.familyBond <= 25 -> "La distance s'est installée. L'éveil ne réparera pas automatiquement ce qui s'est cassé avant lui."; else -> "Le lien tient, avec les tensions ordinaires d'une vie qui change." }, color = MetahumanColors.Muted, lineHeight = 20.sp)
            }
            if (c.powerRevealed) {
                Spacer(Modifier.height(8.dp))
                ProductionStatBadge("relation_rival", "Rival", standingProduction(c.rivalStanding), (c.rivalStanding + 100) / 2, MetahumanColors.Red)
            }
        }
    }
}

@Composable
internal fun ProductionTimelineScreen(c: Campaign) {
    val settings = LocalMetahumanMotion.current.settings
    val entries = c.timeline.reversed()
    var visibleCount by remember(entries.size) { mutableIntStateOf(if (settings.reduceMotion) entries.size else 0) }
    LaunchedEffect(entries.size, settings.reduceMotion, settings.speed) {
        if (settings.reduceMotion) visibleCount = entries.size else {
            visibleCount = 0
            entries.take(16).forEachIndexed { index, _ -> delay(MetahumanMotionTokens.duration(45, settings).toLong()); visibleCount = index + 1 }
            visibleCount = entries.size
        }
    }
    MhlSceneFrame("timeline-${entries.size}", MotionBoard.PANEL_TRANSITION, MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), MetahumanColors.ElectricBlue) {
        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
            MhlSectionTitle("COMIC TIMELINE / ARCHIVES", MetahumanColors.ElectricBlue)
            Spacer(Modifier.height(8.dp))
            if (entries.isEmpty()) Text("Aucune page n'a encore été écrite.", color = MetahumanColors.Muted)
            entries.forEachIndexed { index, item ->
                key(index, item) {
                    AnimatedVisibility(visible = index < visibleCount, enter = fadeIn(tween(MetahumanMotionTokens.duration(160, settings)))) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            MhlProductionAsset(if (item.startsWith("↳")) "alt_03" else if (c.powerRevealed) powerVisualProfile(c.powerFamily).iconKey else civilProgressIcon((c.turn - index).coerceAtLeast(0)), "Entrée de chronique", size = 38.dp)
                            Spacer(Modifier.size(9.dp))
                            MhlComicPanel(Modifier.weight(1f), accent = if (item.startsWith("↳")) Color(0xFF465160) else MetahumanColors.DeepBlue) {
                                Text(item, color = if (item.startsWith("↳")) MetahumanColors.Muted else MetahumanColors.Ivory, fontSize = if (item.startsWith("↳")) 12.sp else 14.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}

@Composable
internal fun ProductionFinalScreen(c: Campaign, onArchive: () -> Unit, onRestart: () -> Unit) {
    val settings = LocalMetahumanMotion.current.settings
    val profile = powerVisualProfile(c.powerFamily)
    val haptic = rememberMetahumanHaptic()
    var confirm by remember { mutableStateOf(false) }
    var legacyStage by remember(c.seed) { mutableIntStateOf(if (settings.reduceMotion) 5 else 0) }
    if (confirm) {
        AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Nouvelle destinée ?") }, text = { Text("Cette vie ne sera pas archivée si tu recommences maintenant.") }, confirmButton = { TextButton(onClick = { confirm = false; onRestart() }) { Text("RECOMMENCER") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("ANNULER") } })
    }
    LaunchedEffect(c.seed, settings.reduceMotion, settings.speed) {
        if (!settings.reduceMotion) {
            MetahumanAudioHooks.onLegacy(); legacyStage = 0
            repeat(5) { step -> delay(MetahumanMotionTokens.duration(600, settings).toLong()); legacyStage = step + 1 }
            haptic(MetahumanMotionLevel.MOTION_SUBTLE)
        }
    }
    Box(Modifier.fillMaxSize().clickable(enabled = legacyStage < 5) { legacyStage = 5 }) {
        MhlBoardTexture(MotionBoard.LEGACY, Modifier.matchParentSize(), if (c.prestige >= 70) MetahumanColors.Gold else profile.accent, .16f)
        Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val icon = when { legacyStage <= 0 -> "relation_family"; legacyStage == 1 -> scopeIcon(c.scope); legacyStage == 2 -> profile.iconKey; legacyStage == 3 -> if (c.familyBond >= 50) "relation_family" else "relation_rival"; else -> if (c.prestige >= 75) "rank_legend" else moralIcon(c) }
            MhlProductionAsset(icon, "Image finale de la carrière", size = 150.dp, pulse = legacyStage < 5)
            Text(when (legacyStage) { 0 -> "${c.age} ANS"; 1 -> c.scope.label.uppercase(); 2 -> if (c.powerRevealed) "UNE SIGNATURE RESTE" else "UNE VIE RESTE"; 3 -> if (c.familyBond >= 60) "DES LIENS ONT TENU" else "CERTAINS LIENS SE SONT DÉFAITS"; else -> "DERNIÈRE PAGE" }, color = MetahumanColors.Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            if (legacyStage < 5 && !settings.reduceMotion) Text("TOUCHER POUR ACCÉLÉRER", color = MetahumanColors.Muted, fontSize = 9.sp)
            Text(c.alias.ifBlank { c.name }.uppercase(), fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 35.sp)
            Text(GameEngine.legacyTitle(c).uppercase(), color = MetahumanColors.Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(14.dp))
            MhlComicPanel(accent = MetahumanColors.Gold, fill = Color(0xF5111720)) {
                Text("LEGACY SCORE · ${GameEngine.legacyScore(c)}", fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("8 → ${c.age} ans · ${c.scope.label}\nMoralité : ${c.moralLabel}\nPrestige : ${c.prestige} · Opinion : ${c.opinion} · Peur : ${c.fear}\nPuissance : ${c.power} · Arcs encore ouverts : ${c.threads.size}", color = MetahumanColors.Muted, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(14.dp))
            MhlPrimaryButton("Archiver cette vie", onArchive, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            MhlSecondaryButton("Nouvelle vie · destin différent", { confirm = true }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ProductionHallScreen(hall: List<String>, onBack: () -> Unit) {
    MhlSceneFrame("hall-${hall.size}", MotionBoard.LEGACY, MetahumanMotionLevel.MOTION_SUBTLE, Modifier.fillMaxSize(), MetahumanColors.Gold) {
        MhlBoardTexture(MotionBoard.LEGACY, Modifier.matchParentSize(), MetahumanColors.Gold, .08f)
        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("HALL OF LEGACIES", color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 25.sp); Text("MUSÉE DES DESTINS", color = MetahumanColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp) }
                TextButton(onClick = onBack) { Text("RETOUR") }
            }
            Spacer(Modifier.height(12.dp))
            if (hall.isEmpty()) MhlComicPanel(accent = Color(0xFF4B5664)) { Text("Aucune destinée achevée.", color = MetahumanColors.Muted) }
            hall.forEachIndexed { index, raw ->
                val p = raw.split('|'); val score = p.getOrElse(2) { "0" }.toIntOrNull() ?: 0
                MhlComicPanel(accent = if (score >= 500) MetahumanColors.Gold else MetahumanColors.DeepBlue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MhlProductionAsset(if (score >= 500) "rank_legend" else if (index % 2 == 0) "rank_gold" else "rank_bronze", "Badge Legacy", size = 70.dp, pulse = index == 0)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.getOrElse(0) { "Inconnu" }.uppercase(), fontWeight = FontWeight.Black, fontSize = 19.sp)
                            Text(p.getOrElse(1) { "Legacy" }, color = MetahumanColors.Gold, fontWeight = FontWeight.Bold)
                            Text("${p.getOrElse(3) { "Rue" }} · Legacy Score $score", color = MetahumanColors.Muted)
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
internal fun ProductionSettingsScreen(onBack: () -> Unit) {
    val controller = LocalMetahumanMotion.current
    val s = controller.settings
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("RÉGLAGES VISUELS", fontWeight = FontWeight.Black, fontSize = 24.sp)
            TextButton(onClick = onBack) { Text("RETOUR") }
        }
        Spacer(Modifier.height(14.dp))
        ProductionSettingToggle("alt_07", "RÉDUIRE LES ANIMATIONS", "Coupe parallax, shake et grands déplacements. Les retours essentiels restent visibles.", s.reduceMotion) { controller.update(s.copy(reduceMotion = it)) }
        Spacer(Modifier.height(10.dp))
        ProductionSettingToggle("alt_04", "HAPTICS", "Retours tactiles légers sur choix et moments majeurs.", s.haptics) { controller.update(s.copy(haptics = it)) }
        Spacer(Modifier.height(10.dp))
        ProductionSettingToggle("alt_05", "CONTRASTE RENFORCÉ", "Accentue fonds, texte et séparation des cadres.", s.highContrast) { controller.update(s.copy(highContrast = it)) }
        Spacer(Modifier.height(14.dp))
        MhlSectionTitle("VITESSE DES ANIMATIONS", MetahumanColors.ElectricBlue)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = s.speed == MetahumanMotionSpeed.NORMAL, onClick = { controller.update(s.copy(speed = MetahumanMotionSpeed.NORMAL)) }, label = { Text("NORMALE") })
            FilterChip(selected = s.speed == MetahumanMotionSpeed.FAST, onClick = { controller.update(s.copy(speed = MetahumanMotionSpeed.FAST)) }, label = { Text("RAPIDE") })
        }
        Spacer(Modifier.height(14.dp))
        MhlSectionTitle("TAILLE DU TEXTE", MetahumanColors.Gold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(90 to "COMPACT", 100 to "NORMAL", 115 to "GRAND").forEach { (value, label) -> FilterChip(selected = s.textScalePercent == value, onClick = { controller.update(s.copy(textScalePercent = value)) }, label = { Text(label) }) }
        }
        Spacer(Modifier.height(14.dp))
        MhlComicPanel(accent = MetahumanColors.Gold) {
            Text("PRODUCTION ASSET PASS", fontWeight = FontWeight.Black, color = MetahumanColors.Gold)
            Text("Les 80 planches ont été auditées. Les sources motion 61–70 ont été retrouvées dans l'atlas numéroté, et 71–80 dans leurs planches dédiées. Les mouvements runtime sont natifs et centralisés afin de rester fluides et accessibles.", color = MetahumanColors.Muted, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun ProductionSettingToggle(icon: String, title: String, body: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    MhlComicPanel(accent = MetahumanColors.ElectricBlue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MhlProductionAsset(icon, title, size = 58.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Black); Text(body, color = MetahumanColors.Muted, fontSize = 12.sp, lineHeight = 17.sp) }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

private fun qualitativeProduction(v: Int) = when { v >= 85 -> "Exceptionnel"; v >= 65 -> "Élevé"; v >= 40 -> "Modéré"; v >= 20 -> "Faible"; else -> "Critique" }
private fun standingProduction(v: Int) = when { v >= 50 -> "Allié"; v >= 20 -> "Favorable"; v > -20 -> "Neutre"; v > -50 -> "Hostile"; else -> "Ennemi" }
private fun bondProduction(v: Int) = when { v >= 80 -> "Très solide"; v >= 60 -> "Solide"; v >= 40 -> "Fragile"; v >= 20 -> "Très fragile"; else -> "Rupture proche" }
private fun signedNumber(v: Int): String = if (v >= 0) "+$v" else v.toString()
