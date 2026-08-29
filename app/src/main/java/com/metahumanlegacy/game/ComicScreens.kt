package com.metahumanlegacy.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ComicDestinyScreen(c: Campaign, outcome: String?, reduceMotion: Boolean, onContinue: () -> Unit, onChoice: (EventNode, Choice) -> Unit) {
    val haptics = LocalHapticFeedback.current
    if (outcome != null) {
        val title = outcome.substringBefore("\n\n").ifBlank { "CONSÉQUENCE" }
        val body = outcome.substringAfter("\n\n", outcome)
        val art = if (c.powerRevealed) powerIcon(c.powerFamily) else civilProgressIcon(c.turn)
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            AnimatedVisibility(visible = true, enter = if (reduceMotion) fadeIn(tween(1)) else fadeIn(tween(380)) + scaleIn(tween(380), initialScale = .965f)) {
                MhlComicPanel(accent = if (c.powerRevealed) MetahumanColors.Gold else MetahumanColors.DeepBlue, fill = Color(0xFF101722)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MhlAsset(art, "Illustration de conséquence", size = if (c.powerRevealed) 92.dp else 74.dp); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(if (c.powerRevealed) "CASE SUIVANTE" else "APRÈS CE CHOIX", color = MetahumanColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp); Text(title.uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 23.sp) }
                    }
                    Spacer(Modifier.height(12.dp)); Text(body, color = MetahumanColors.Ivory, lineHeight = 24.sp, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(12.dp)); Text("Certaines conséquences restent hors champ. Les variables internes et routes narratives ne sont jamais affichées.", color = MetahumanColors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(18.dp)); MhlPrimaryButton(if (c.needsAlias) "Choisir ce qu'on t'appellera" else "Continuer", onContinue, Modifier.fillMaxWidth())
        }
        return
    }

    val event = remember(c.seed, c.turn, c.flags, c.threads, c.lastCategory, c.lastApproach, c.powerFamily) { GameEngine.event(c) }
    val phase = when { c.turn < 10 -> "AVANT LE MASQUE · ${c.turn + 1}/10"; c.turn == 10 -> "LE PREMIER IMPOSSIBLE"; c.turn in 11..15 -> "PREMIERS PAS · ${c.turn - 10}/5"; event.kind == "MAJOR" -> "CHAPITRE ${event.threadStage.coerceAtLeast(1)}"; else -> event.category }
    val art = when { event.kind == "AWAKENING" -> "origin_unknown"; !c.powerRevealed -> civilProgressIcon(c.turn); event.stakes >= 3 -> dangerIcon(8); else -> powerIcon(c.powerFamily) }
    val awakening = event.kind == "AWAKENING"
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(phase, color = MetahumanColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f)); if (c.powerRevealed && event.stakes > 1) MhlAsset(dangerIcon(event.stakes * 2), "Niveau de danger", size = 28.dp) }
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(visible = true, enter = if (reduceMotion || !awakening) fadeIn(tween(1)) else fadeIn(tween(700)) + scaleIn(tween(700), initialScale = .82f)) {
            MhlComicPanel(accent = eventAccent(c, event), fill = if (awakening) Color(0xFF171125) else Color(0xFF0F1620)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MhlAsset(art, if (awakening) "Symbole d'éveil" else "Illustration de l'événement", size = if (awakening) 110.dp else 78.dp); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { if (event.kind == "MAJOR") Text("ARC NARRATIF", color = MetahumanColors.Red, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp); if (awakening) Text("MA VIE VIENT DE CHANGER", color = MetahumanColors.Violet, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp); Text(event.title.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 26.sp) }
                }
                Spacer(Modifier.height(12.dp)); Text(event.text, color = MetahumanColors.Ivory, fontSize = 16.sp, lineHeight = 24.sp)
            }
        }
        Spacer(Modifier.height(12.dp)); MhlSectionTitle("CHOIX", MetahumanColors.ElectricBlue); Spacer(Modifier.height(5.dp))
        event.choices.forEachIndexed { index, choice -> ComicChoiceCard(event, choice, index + 1) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onChoice(event, choice) }; Spacer(Modifier.height(7.dp)) }
        Text(when (event.kind) { "FORMATIVE" -> "Ces choix restent humains. Rien ici ne révèle le pouvoir qu'ils contribuent à former."; "AWAKENING" -> "Le pouvoir est déjà déterminé hors de ta vue. Tu choisis seulement ta première réaction."; else -> "Choisis pour le sens de l'action. La couleur ne révèle jamais une route morale cachée." }, color = MetahumanColors.Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

private fun eventAccent(c: Campaign, event: EventNode): Color = when { event.kind == "AWAKENING" -> MetahumanColors.Violet; !c.powerRevealed && c.turn <= 3 -> MetahumanColors.DeepBlue; !c.powerRevealed && c.turn <= 6 -> Color(0xFF446D9C); !c.powerRevealed -> MetahumanColors.Violet.copy(alpha = .85f); event.stakes >= 3 -> MetahumanColors.Red; else -> MetahumanColors.ElectricBlue }

@Composable private fun ComicChoiceCard(event: EventNode, choice: Choice, index: Int, onClick: () -> Unit) {
    val risk = choice.risk; val border = if (risk >= 7) MetahumanColors.Red.copy(alpha = .72f) else Color(0xFF3B495A)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(Color(0xFF151C25), MetahumanButtonShape).border(1.dp, border, MetahumanButtonShape).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).background(Color(0xFF202B38), CutCornerShape(6.dp)), contentAlignment = Alignment.Center) { Text(index.toString(), color = MetahumanColors.Gold, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(10.dp)); Text(choice.label, color = MetahumanColors.Ivory, fontWeight = FontWeight.Bold, lineHeight = 19.sp, modifier = Modifier.weight(1f))
        if (risk > 0 && event.kind != "AWAKENING") { Spacer(Modifier.width(8.dp)); Column(horizontalAlignment = Alignment.CenterHorizontally) { MhlAsset(dangerIcon(risk), "Risque ${riskLabelComic(risk)}", size = 30.dp); Text(riskLabelComic(risk), color = MetahumanColors.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold) } }
    }
}
private fun riskLabelComic(risk: Int) = when { risk >= 8 -> "EXTRÊME"; risk >= 6 -> "ÉLEVÉ"; risk >= 4 -> "RISQUÉ"; risk >= 2 -> "INCERTAIN"; else -> "FAIBLE" }

@Composable internal fun ComicCharacterScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        MhlSectionTitle("CHARACTER DOSSIER", MetahumanColors.Gold); Spacer(Modifier.height(8.dp))
        MhlComicPanel(accent = if (c.powerRevealed) MetahumanColors.ElectricBlue else Color(0xFF586270)) {
            Row(verticalAlignment = Alignment.CenterVertically) { MhlAsset(if (c.powerRevealed) powerIcon(c.powerFamily) else civilProgressIcon(c.turn), "Identité du personnage", size = 88.dp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(c.name.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 22.sp); Text(if (c.powerRevealed) c.alias.ifBlank { "Sans alias" } else "PERSONNE ORDINAIRE", color = MetahumanColors.Gold, fontWeight = FontWeight.Bold); Text("${c.age} ans · ${c.city}, ${c.district}\n${c.socialBackground} · ${c.temperament}", color = MetahumanColors.Muted, lineHeight = 18.sp, fontSize = 12.sp) } }
        }
        Spacer(Modifier.height(12.dp))
        if (c.powerRevealed) { MhlSectionTitle("IDENTITÉ MÉTA", MetahumanColors.ElectricBlue); Spacer(Modifier.height(7.dp)); MhlComicPanel(accent = MetahumanColors.Violet) { Text(c.powerFamily.uppercase(), fontWeight = FontWeight.Black, fontSize = 19.sp); Text("Origine : ${c.origin}\nSignature : ${c.powerSignature}\nFaiblesse connue : ${c.weakness}\nExposition de l'identité : ${c.identityExposure}%", color = MetahumanColors.Muted, lineHeight = 20.sp) }; Spacer(Modifier.height(12.dp)) }
        MhlSectionTitle("AXES INDÉPENDANTS", MetahumanColors.Gold); Spacer(Modifier.height(7.dp))
        MhlStatBadge(moralIcon(c), "Moralité", c.moralLabel, (c.morality + 100) / 2, MetahumanColors.Gold); Spacer(Modifier.height(7.dp))
        MhlStatBadge(prestigeIcon(c), "Prestige", c.prestige.toString(), c.prestige, MetahumanColors.WarmGold); Spacer(Modifier.height(7.dp))
        MhlStatBadge("relation_media", "Opinion", if (c.opinion >= 0) "+${c.opinion}" else c.opinion.toString(), (c.opinion + 100) / 2, MetahumanColors.ElectricBlue)
        if (c.powerRevealed) { Spacer(Modifier.height(7.dp)); MhlStatBadge("public_fear", "Peur", c.fear.toString(), c.fear, MetahumanColors.Red); Spacer(Modifier.height(7.dp)); MhlStatBadge(powerIcon(c.powerFamily), "Puissance", c.power.toString(), c.power, MetahumanColors.Violet); Spacer(Modifier.height(7.dp)); MhlStatBadge(scopeIcon(c.scope), "Portée", c.scope.label, null, MetahumanColors.ElectricBlue); Spacer(Modifier.height(7.dp)); MhlStatBadge("alt_04", "Maîtrise", qualitativeComic(c.control), c.control, MetahumanColors.Green) }
        Spacer(Modifier.height(7.dp)); MhlStatBadge("relation_family", "Santé", qualitativeComic(c.health), c.health, MetahumanColors.Green)
    }
}

@Composable internal fun ComicWorldScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        MhlSectionTitle(if (c.powerRevealed) "MONDE VIVANT" else "VILLE / ENVIRONNEMENT", MetahumanColors.ElectricBlue); Spacer(Modifier.height(8.dp))
        MhlComicPanel(accent = MetahumanColors.ElectricBlue) { Row(verticalAlignment = Alignment.CenterVertically) { MhlAsset(scopeIcon(c.scope), "Échelle géographique ${c.scope.label}", size = 94.dp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(c.city.uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp); Text("${c.district} · ${c.modifier}", color = MetahumanColors.Muted); Text(if (c.powerRevealed) "Portée actuelle : ${c.scope.label}" else "Tu n'es encore qu'un civil dans cette ville.", color = MetahumanColors.Gold, fontWeight = FontWeight.Bold) } } }
        Spacer(Modifier.height(12.dp))
        if (!c.powerRevealed) MhlComicPanel(accent = Color(0xFF526073)) { Text("RIEN N'ANNONCE ENCORE UNE LÉGENDE", fontWeight = FontWeight.Black); Text("Le monde mémorise déjà tes proches, blessures et décisions, mais aucune institution ne te traite comme un héros ou un vilain.", color = MetahumanColors.Muted, lineHeight = 20.sp) }
        else { MhlSectionTitle("DOSSIERS D'INFLUENCE", MetahumanColors.Gold); Spacer(Modifier.height(7.dp)); MhlStatBadge("alt_05", "Gouvernement", signedLabelComic(c.governmentStanding), (c.governmentStanding + 100) / 2, MetahumanColors.DeepBlue); Spacer(Modifier.height(7.dp)); MhlStatBadge("alt_06", "Factions", signedLabelComic(c.factionStanding), (c.factionStanding + 100) / 2, MetahumanColors.Violet); Spacer(Modifier.height(7.dp)); MhlStatBadge("relation_media", "Médias", signedLabelComic(c.mediaStanding), (c.mediaStanding + 100) / 2, MetahumanColors.ElectricBlue); Spacer(Modifier.height(12.dp)); MhlSectionTitle("ARCS ACTIFS", MetahumanColors.Red); Spacer(Modifier.height(7.dp)); if (c.threads.isEmpty()) Text("Aucun fil narratif majeur n'est ouvert.", color = MetahumanColors.Muted); c.threads.forEach { thread -> MhlComicPanel(accent = if (thread.intensity >= 3) MetahumanColors.Red else MetahumanColors.DeepBlue) { Text(thread.id, fontWeight = FontWeight.Black); Text("Chapitre ${thread.stage} · Intensité ${thread.intensity}/3", color = MetahumanColors.Muted) }; Spacer(Modifier.height(7.dp)) } }
    }
}

@Composable internal fun ComicLinksScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        MhlSectionTitle("LIENS PERSISTANTS", MetahumanColors.Gold); Spacer(Modifier.height(8.dp)); MhlStatBadge("relation_family", "Famille / proches", bondLabelComic(c.familyBond), c.familyBond, MetahumanColors.Gold); Spacer(Modifier.height(8.dp))
        MhlComicPanel(accent = MetahumanColors.Gold) { Text("TES PROCHES", fontWeight = FontWeight.Black); Text(when { c.familyBond >= 75 -> "Ils occupent une place centrale dans ta vie. Ce lien peut devenir une force autant qu'une vulnérabilité."; c.familyBond <= 25 -> "La distance s'est installée. L'éveil ne réparera pas automatiquement ce qui s'est cassé avant lui."; else -> "Le lien tient, avec les tensions ordinaires d'une vie qui change." }, color = MetahumanColors.Muted, lineHeight = 20.sp) }
        if (c.powerRevealed) { Spacer(Modifier.height(8.dp)); MhlStatBadge("relation_rival", "Rival", signedLabelComic(c.rivalStanding), (c.rivalStanding + 100) / 2, MetahumanColors.Red) }
    }
}

@Composable internal fun ComicTimelineScreen(c: Campaign) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        MhlSectionTitle("COMIC TIMELINE / ARCHIVES", MetahumanColors.ElectricBlue); Spacer(Modifier.height(8.dp)); if (c.timeline.isEmpty()) Text("Aucune page n'a encore été écrite.", color = MetahumanColors.Muted)
        c.timeline.reversed().forEachIndexed { index, item -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { MhlAsset(if (item.startsWith("↳")) "alt_03" else if (c.powerRevealed) powerIcon(c.powerFamily) else civilProgressIcon((c.turn - index).coerceAtLeast(0)), "Entrée de chronique", size = 38.dp); Spacer(Modifier.width(9.dp)); MhlComicPanel(Modifier.weight(1f), accent = if (item.startsWith("↳")) Color(0xFF465160) else MetahumanColors.DeepBlue) { Text(item, color = if (item.startsWith("↳")) MetahumanColors.Muted else MetahumanColors.Ivory, fontSize = if (item.startsWith("↳")) 12.sp else 14.sp, lineHeight = 19.sp) } }; Spacer(Modifier.height(7.dp)) }
    }
}

@Composable internal fun ComicFinalScreen(c: Campaign, onArchive: () -> Unit, onRestart: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Nouvelle destinée ?") }, text = { Text("Cette vie ne sera pas archivée si tu recommences maintenant.") }, confirmButton = { TextButton(onClick = { confirm = false; onRestart() }) { Text("RECOMMENCER") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("ANNULER") } })
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        MhlAsset(if (c.prestige >= 75) "rank_legend" else moralIcon(c), "Emblème final", size = 150.dp); Text("DERNIÈRE PAGE", color = MetahumanColors.Gold, fontWeight = FontWeight.Black, letterSpacing = 3.sp); Text(c.alias.ifBlank { c.name }.uppercase(), fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 35.sp); Text(GameEngine.legacyTitle(c).uppercase(), color = MetahumanColors.Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(14.dp))
        MhlComicPanel(accent = MetahumanColors.Gold) { Text("LEGACY SCORE · ${GameEngine.legacyScore(c)}", fontWeight = FontWeight.Black, fontSize = 22.sp); Text("18 → ${c.age} ans · ${c.scope.label}\nMoralité : ${c.moralLabel}\nPrestige : ${c.prestige} · Opinion : ${c.opinion} · Peur : ${c.fear}\nPuissance : ${c.power} · Arcs encore ouverts : ${c.threads.size}", color = MetahumanColors.Muted, lineHeight = 20.sp) }
        Spacer(Modifier.height(14.dp)); MhlPrimaryButton("Archiver cette vie", onArchive, Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); MhlSecondaryButton("Recommencer", { confirm = true }, Modifier.fillMaxWidth())
    }
}

@Composable internal fun ComicHallScreen(hall: List<String>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("HALL OF LEGACIES", color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 25.sp); Text("MUSÉE DES DESTINS", color = MetahumanColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp) }; TextButton(onClick = onBack) { Text("RETOUR") } }
        Spacer(Modifier.height(12.dp)); if (hall.isEmpty()) MhlComicPanel(accent = Color(0xFF4B5664)) { Text("Aucune destinée achevée.", color = MetahumanColors.Muted) }
        hall.forEachIndexed { index, raw -> val p = raw.split('|'); val score = p.getOrElse(2) { "0" }.toIntOrNull() ?: 0; MhlComicPanel(accent = if (score >= 500) MetahumanColors.Gold else MetahumanColors.DeepBlue) { Row(verticalAlignment = Alignment.CenterVertically) { MhlAsset(if (score >= 500) "rank_legend" else if (index % 2 == 0) "rank_gold" else "rank_bronze", "Badge Legacy", size = 70.dp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(p.getOrElse(0) { "Inconnu" }.uppercase(), fontWeight = FontWeight.Black, fontSize = 19.sp); Text(p.getOrElse(1) { "Legacy" }, color = MetahumanColors.Gold, fontWeight = FontWeight.Bold); Text("${p.getOrElse(3) { "Rue" }} · Legacy Score $score", color = MetahumanColors.Muted) } } }; Spacer(Modifier.height(9.dp)) }
    }
}

@Composable internal fun ComicSettingsScreen(reduceMotion: Boolean, onReduceMotion: (Boolean) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("RÉGLAGES VISUELS", fontWeight = FontWeight.Black, fontSize = 24.sp); TextButton(onClick = onBack) { Text("RETOUR") } }; Spacer(Modifier.height(14.dp))
        MhlComicPanel(accent = MetahumanColors.ElectricBlue) { Row(verticalAlignment = Alignment.CenterVertically) { MhlAsset("alt_07", "Réglages visuels", size = 58.dp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("RÉDUIRE LES ANIMATIONS", fontWeight = FontWeight.Black); Text("Désactive les transitions marquées tout en conservant la hiérarchie graphique.", color = MetahumanColors.Muted, fontSize = 12.sp, lineHeight = 17.sp) }; Switch(checked = reduceMotion, onCheckedChange = onReduceMotion) } }
        Spacer(Modifier.height(10.dp)); MhlComicPanel(accent = MetahumanColors.Gold) { Text("KIT VISUEL ACTIF", fontWeight = FontWeight.Black, color = MetahumanColors.Gold); Text("50 éléments optimisés issus des 20 planches MetaHuman Legacy. Les assets sont chargés à la demande avec fallback graphique cohérent.", color = MetahumanColors.Muted, lineHeight = 20.sp) }
    }
}

private fun qualitativeComic(v: Int) = when { v >= 85 -> "Exceptionnel"; v >= 65 -> "Élevé"; v >= 40 -> "Modéré"; v >= 20 -> "Faible"; else -> "Critique" }
private fun signedLabelComic(v: Int) = when { v >= 50 -> "Allié"; v >= 20 -> "Favorable"; v > -20 -> "Neutre"; v > -50 -> "Hostile"; else -> "Ennemi" }
private fun bondLabelComic(v: Int) = when { v >= 80 -> "Très solide"; v >= 60 -> "Solide"; v >= 40 -> "Fragile"; v >= 20 -> "Très fragile"; else -> "Rupture proche" }
