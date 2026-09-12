package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
internal fun GameplayRebuildCityScreen(c: Campaign, state: UltimateState, deep: DeepLifeState, onBack: () -> Unit) {
    val accent = if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(220.dp).background(Brush.verticalGradient(listOf(accent.copy(alpha = .22f), Color(0xFF07101A), Color(0xFF05080D))))) {
            Canvas(Modifier.fillMaxSize()) {
                val points = state.districts.take(5).mapIndexed { i, d ->
                    androidx.compose.ui.geometry.Offset(
                        size.width * (0.18f + (i % 3) * .31f),
                        size.height * (0.28f + (i / 3) * .37f + (d.damage.coerceIn(0, 100) / 1000f))
                    )
                }
                points.zipWithNext().forEach { (a, b) -> drawLine(accent.copy(alpha = .28f), a, b, strokeWidth = 4f) }
                points.forEachIndexed { i, point ->
                    val d = state.districts[i]
                    val heat = (d.crime + d.damage - d.reconstruction).coerceIn(0, 100)
                    drawCircle(if (heat >= 60) UltimateRed.copy(alpha = .68f) else accent.copy(alpha = .68f), 14f + heat / 8f, point)
                    drawCircle(Color(0xFF05080D), 7f, point)
                }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(c.city.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 28.sp)
                Text("Une ville qui continue sans toi", color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp)
                Text("${cityConditionLabel(state.cityCondition)} · ${state.metaLaw}", color = UltimateMuted, fontSize = 10.sp)
            }
        }

        Column(Modifier.padding(14.dp)) {
            Text("QUARTIERS VIVANTS", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(7.dp))
            if (state.districts.isEmpty()) {
                UltimatePanel(accent = accent) {
                    Text(c.district, color = UltimateIvory, fontWeight = FontWeight.Black)
                    Text("Le quartier n'a pas encore assez d'histoire pour être cartographié. Tes prochaines décisions vont le faire évoluer.", color = UltimateMuted, fontSize = 11.sp)
                }
            } else {
                state.districts.forEach { d ->
                    DistrictLifeCard(d)
                    Spacer(Modifier.height(7.dp))
                }
            }

            val urgent = deep.opportunities.sortedByDescending { it.urgency }.take(3)
            if (urgent.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text("CE QUI AVANCE SANS TOI", color = UltimateRed, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Spacer(Modifier.height(6.dp))
                urgent.forEach { opportunity ->
                    UltimatePanel(accent = if (opportunity.urgency >= 7) UltimateRed else UltimateGold) {
                        Text(opportunity.title, color = UltimateIvory, fontWeight = FontWeight.Black)
                        Text(opportunityTimingLine(c, opportunity), color = UltimateMuted, fontSize = 10.sp)
                        if (opportunity.ignoredPayload.isNotBlank()) Text("Si tu l'ignores : ${opportunity.ignoredPayload}", color = UltimateRed, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            MhlSecondaryButton("Retour à ta vie", onBack, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DistrictLifeCard(d: UltimateDistrict) {
    val danger = (d.crime + d.damage - d.reconstruction / 2).coerceIn(0, 100)
    val accent = when {
        danger >= 70 -> UltimateRed
        danger >= 45 -> UltimateGold
        else -> UltimateGreen
    }
    UltimatePanel(accent = accent) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(d.name.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(d.landmark.ifBlank { "Aucun lieu emblématique encore" }, color = UltimateMuted, fontSize = 10.sp)
            }
            Text(if (d.restricted) "FERMÉ" else when { danger >= 70 -> "CRISE"; danger >= 45 -> "TENDU"; else -> "CALME" }, color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
        }
        Spacer(Modifier.height(5.dp))
        Text(districtConditionLine(d), color = UltimateMuted, fontSize = 10.sp)
        Text("Contrôle : ${d.faction} · sentiment ${signedHuman(d.sentiment)}", color = UltimateIvory, fontSize = 10.sp)
    }
}

@Composable
internal fun GameplayRebuildLinksScreen(
    c: Campaign,
    annual: AnnualActionState,
    deep: DeepLifeState,
    onAction: (AnnualActionCard) -> AnnualActionResult?,
    onBack: () -> Unit
) {
    val synced = annual.synced(c)
    var result by remember(c.turn) { mutableStateOf<AnnualActionResult?>(null) }
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Text("LES GENS DE TA VIE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.3.sp)
        Text("À ${c.age} ans, personne n'est une jauge.", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 27.sp)
        Text(
            if (synced.remaining > 0) "Il te reste ${synced.remaining} moment${if (synced.remaining > 1) "s" else ""} libre${if (synced.remaining > 1) "s" else ""}. Appeler, venir, s'excuser ou demander de l'aide prend du temps réel."
            else "Tu n'as plus de temps libre cette année. Les liens restent là, mais tu ne peux pas tout réparer en une seule période.",
            color = UltimateMuted, fontSize = 12.sp, lineHeight = 17.sp
        )
        result?.let {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = UltimateGreen) {
                Text(it.title.uppercase(), color = UltimateGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text(it.text, color = UltimateIvory, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        deep.relationships.filter { it.alive }.forEach { person ->
            RelationshipLifeCard(
                person = person,
                enabled = synced.remaining > 0,
                usedIds = synced.usedIds,
                onAction = { card ->
                    val resolved = onAction(card)
                    if (resolved != null) result = resolved
                }
            )
            Spacer(Modifier.height(8.dp))
        }
        MhlSecondaryButton("Retour", onBack, Modifier.fillMaxWidth())
    }
}

@Composable
private fun RelationshipLifeCard(
    person: DeepRelationship,
    enabled: Boolean,
    usedIds: Set<String>,
    onAction: (AnnualActionCard) -> Unit
) {
    val accent = when (person.phase) {
        RelationshipPhase.HURT, RelationshipPhase.DISTANT, RelationshipPhase.RIVAL -> UltimateRed
        RelationshipPhase.CLOSE, RelationshipPhase.TRUSTED, RelationshipPhase.SUCCESSOR -> UltimateGold
        RelationshipPhase.MENTOR, RelationshipPhase.PROTEGE -> UltimateViolet
        else -> UltimateBlue
    }
    UltimatePanel(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(accent.copy(alpha = .14f)).border(1.dp, accent.copy(alpha = .45f), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                Text(person.name.take(1).uppercase(), color = accent, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Spacer(Modifier.size(9.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("${person.role} · ${relationshipPhrase(person)}", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (person.knowsIdentity) Text("Connaît ton identité", color = UltimateViolet, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(7.dp))
        if (person.core.values.isNotEmpty()) Text("Ce qui compte : ${person.core.values.take(2).joinToString(" · ").lowercase()}", color = UltimateIvory, fontSize = 10.sp)
        if (person.core.fears.isNotEmpty()) Text("Ce qu'iel redoute : ${person.core.fears.take(1).joinToString().lowercase().replace('_', ' ')}", color = UltimateMuted, fontSize = 10.sp)
        if (person.core.ambitions.isNotEmpty()) Text("Ce qu'iel veut : ${person.core.ambitions.take(1).joinToString().lowercase().replace('_', ' ')}", color = UltimateMuted, fontSize = 10.sp)
        person.memories.maxByOrNull { it.weight }?.let { memory ->
            Spacer(Modifier.height(6.dp))
            Text("SE SOUVIENT", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 8.sp)
            Text("« ${memory.summary} »", color = UltimateIvory, fontSize = 10.sp, lineHeight = 14.sp)
        }

        Spacer(Modifier.height(8.dp))
        if (enabled) {
            val actions = directRelationshipCards(person).filterNot { it.id in usedIds }
            actions.take(4).forEach { card ->
                MhlSecondaryButton(card.title, { onAction(card) }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(5.dp))
            }
            if (actions.isEmpty()) Text("Tu as déjà agi envers cette personne cette année.", color = UltimateMuted, fontSize = 9.sp)
        }
    }
}

private fun directRelationshipCards(person: DeepRelationship): List<AnnualActionCard> = buildList {
    add(AnnualActionCard(
        id = "direct_rel_visit_${person.id}", title = "Passer du temps avec ${person.name}",
        description = "Être présent sans attendre qu'une crise vous réunisse.", category = AnnualActionCategory.RELATION,
        iconKey = "relation_family", focus = "Lien · présence", outcome = "Tu as choisi cette personne avant une urgence.",
        familyBond = if (person.id == "family") 2 else 0, presence = 1
    ))
    if (person.phase in setOf(RelationshipPhase.HURT, RelationshipPhase.DISTANT, RelationshipPhase.RIVAL) || person.grudge >= 10) {
        add(AnnualActionCard(
            id = "direct_rel_apologize_${person.id}", title = "T'excuser auprès de ${person.name}",
            description = "Reconnaître précisément ce qui a blessé le lien sans exiger un pardon immédiat.", category = AnnualActionCategory.RELATION,
            iconKey = "relation_family", focus = "Lien · réparation", outcome = "Tu ne peux pas effacer l'histoire, mais tu cesses de faire comme si elle n'existait pas."
        ))
    }
    if (person.phase !in setOf(RelationshipPhase.DISTANT, RelationshipPhase.RIVAL)) {
        add(AnnualActionCard(
            id = "direct_rel_help_${person.id}", title = "Demander l'aide de ${person.name}",
            description = "Partager un problème au lieu de tout porter seul. Cela rapproche, mais crée aussi une forme de dépendance.", category = AnnualActionCategory.RELATION,
            iconKey = "relation_family", focus = "Lien · confiance", outcome = "Tu lui fais une place concrète dans ce que tu traverses."
        ))
    }
    if (person.phase !in setOf(RelationshipPhase.DISTANT, RelationshipPhase.RIVAL)) {
        add(AnnualActionCard(
            id = "direct_rel_distance_${person.id}", title = "Prendre tes distances avec ${person.name}",
            description = "Décider que ce lien ne peut pas continuer comme avant. Le coût n'est pas réversible en un clic.", category = AnnualActionCategory.RELATION,
            iconKey = "relation_family", focus = "Lien · rupture", outcome = "Tu crées volontairement une distance qui aura sa propre histoire."
        ))
    }
}

@Composable
internal fun GameplayRebuildActionsScreen(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    deep: DeepLifeState,
    onAction: (AnnualActionCard) -> AnnualActionResult?,
    onBack: () -> Unit
) {
    val synced = annual.synced(c)
    var result by remember(c.turn) { mutableStateOf<AnnualActionResult?>(null) }
    val actions = remember(c.seed, c.turn, state.hashCode(), synced.usedIds, synced.used) { UltimateGameEngine.annualActions(c, state, synced) }
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Text("TON TEMPS EST LIMITÉ", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.2.sp)
        Text("${synced.remaining} moment${if (synced.remaining > 1) "s" else ""} libre${if (synced.remaining > 1) "s" else ""} cette année.", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 27.sp)
        Text("Tout choisir est impossible. Ce que tu ignores peut continuer sans toi.", color = UltimateMuted, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(9.dp))

        deep.opportunities.sortedByDescending { it.urgency }.take(2).forEach { o ->
            UltimatePanel(accent = if (o.urgency >= 7) UltimateRed else UltimateGold) {
                Text("ÇA N'ATTENDRA PAS", color = if (o.urgency >= 7) UltimateRed else UltimateGold, fontWeight = FontWeight.Black, fontSize = 8.sp)
                Text(o.title, color = UltimateIvory, fontWeight = FontWeight.Black)
                Text(opportunityTimingLine(c, o), color = UltimateMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(6.dp))
        }

        result?.let {
            UltimatePanel(accent = UltimateGreen) {
                Text(it.title.uppercase(), color = UltimateGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text(it.text, color = UltimateIvory, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.height(7.dp))
        }

        if (synced.remaining > 0) {
            actions.take(8).forEach { card ->
                val accent = actionAccent(card.category)
                UltimatePanel(accent = accent) {
                    Text(card.category.label.uppercase(), color = accent, fontWeight = FontWeight.Black, fontSize = 8.sp)
                    Text(card.title, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(card.description, color = UltimateMuted, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(actionTradeoff(card), color = UltimateGold, fontSize = 9.sp, lineHeight = 13.sp)
                    Spacer(Modifier.height(7.dp))
                    MhlPrimaryButton("Y consacrer du temps", {
                        val r = onAction(card)
                        if (r != null) result = r
                    }, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(7.dp))
            }
        } else {
            UltimatePanel(accent = UltimateGold) {
                Text("TU AS CHOISI OÙ PASSER TON TEMPS", color = UltimateGold, fontWeight = FontWeight.Black)
                Text("Le reste de l'année continuera. Certaines choses que tu n'as pas faites pourront changer sans toi.", color = UltimateMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(7.dp))
        MhlSecondaryButton("Retour au Destin", onBack, Modifier.fillMaxWidth())
    }
}

private fun relationshipPhrase(person: DeepRelationship): String = when (person.phase) {
    RelationshipPhase.CLOSE -> "très proche"
    RelationshipPhase.TRUSTED -> "te fait confiance"
    RelationshipPhase.HURT -> "porte encore une blessure"
    RelationshipPhase.DISTANT -> "s'éloigne"
    RelationshipPhase.RIVAL -> "te défie"
    RelationshipPhase.MENTOR -> "essaie de te transmettre quelque chose"
    RelationshipPhase.EQUAL -> "te considère comme son égal"
    RelationshipPhase.PROTEGE -> "apprend de toi"
    RelationshipPhase.SUCCESSOR -> "pourrait continuer après toi"
    RelationshipPhase.FRIEND -> "ami"
    else -> "fait partie de ton histoire"
}

private fun cityConditionLabel(value: Int): String = when {
    value >= 80 -> "Ville stable"
    value >= 60 -> "Ville sous pression mais fonctionnelle"
    value >= 40 -> "Ville fragilisée"
    value >= 20 -> "Ville en crise"
    else -> "Ville au bord de l'effondrement"
}

private fun districtConditionLine(d: UltimateDistrict): String {
    val danger = (d.crime + d.damage - d.reconstruction / 2).coerceIn(0, 100)
    val safety = when {
        danger >= 75 -> "violence très présente"
        danger >= 50 -> "tensions régulières"
        danger >= 30 -> "incidents contenus"
        else -> "rues plutôt calmes"
    }
    val fabric = when {
        d.damage >= 65 -> "quartier lourdement marqué"
        d.damage >= 35 -> "traces visibles des crises"
        d.reconstruction >= 45 -> "reconstruction bien engagée"
        else -> "bâti encore stable"
    }
    return "$safety · $fabric"
}

private fun opportunityTimingLine(c: Campaign, opportunity: Opportunity): String {
    val remaining = (opportunity.expiresTurn - c.turn).coerceAtLeast(0)
    val kind = when (opportunity.category) {
        "RELATION" -> "Quelqu'un t'attend"
        "RECOVERY" -> "Ton corps réclame du temps"
        "INVESTIGATION" -> "Une piste reste ouverte"
        "INTERVENTION" -> "Le quartier attend une réponse"
        else -> "Occasion fragile"
    }
    val timing = when (remaining) {
        0 -> "dernière chance"
        1 -> "très urgent"
        2 -> "bientôt trop tard"
        else -> "encore un peu de temps"
    }
    return "$kind · $timing"
}

private fun signedHuman(value: Int): String = when {
    value >= 30 -> "favorable"
    value <= -30 -> "hostile"
    else -> "partagé"
}

private fun actionAccent(category: AnnualActionCategory): Color = when (category) {
    AnnualActionCategory.INTERVENTION -> UltimateRed
    AnnualActionCategory.INVESTIGATION -> UltimateBlue
    AnnualActionCategory.RELATION -> UltimateGold
    AnnualActionCategory.TRAINING -> UltimateViolet
    AnnualActionCategory.RECOVERY -> UltimateGreen
    AnnualActionCategory.PUBLIC -> Color(0xFFFFA64D)
    AnnualActionCategory.CIVIL -> UltimateMuted
}

private fun actionTradeoff(card: AnnualActionCard): String {
    val parts = buildList {
        if (card.health > 0) add("te remet sur pied")
        if (card.health < 0) add("te fatigue")
        if (card.familyBond > 0) add("nourrit un lien")
        if (card.control > 0) add("affine ton contrôle")
        if (card.investigation > 0) add("réduit ton incertitude")
        if (card.identityExposure > 0) add("laisse plus de traces")
        if (card.identityExposure < 0) add("protège ton secret")
    }
    return "Ce que ça change : " + parts.take(2).joinToString(" · ").ifBlank { card.focus.lowercase() }
}
