package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@Composable
internal fun DeepLifeChronicleScreen(c: Campaign, u: UltimateState, d: DeepLifeState) {
    val accent = if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Chronique vivante", "La personne derrière la légende", "Pas seulement des statistiques : ce que tu es devenu, ce que les autres retiennent et ce que tu laisses derrière toi.", accent)
        Spacer(Modifier.height(10.dp))
        UltimateHeroBanner(c, u, Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))

        UltimatePanel(accent = accent) {
            Text("IDENTITÉ ACTUELLE", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("${c.alias.ifBlank { c.name }} · ${c.age} ans · ${c.scope.label}", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("${u.mediaFrame} · ${u.legalStatus}", color = UltimateMuted, fontSize = 11.sp)
            if (u.retirementIntent != "Indécis") Text("Après : ${u.retirementIntent}", color = UltimateGold, fontSize = 11.sp)
        }

        Spacer(Modifier.height(8.dp))
        val traits = d.personality.entries.sortedByDescending { it.value }.take(6)
        UltimatePanel(accent = UltimateViolet) {
            Text("CE QUE TES CHOIX ONT FAIT DE TOI", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp)
            if (traits.isEmpty()) Text("Ton caractère est encore en train de se dessiner.", color = UltimateMuted)
            traits.forEach { (name, value) ->
                UltimateMeter(name.lowercase().replaceFirstChar { it.uppercase() }, value.coerceIn(-100, 100), UltimateViolet, rangeMin = -100, rangeMax = 100)
                Spacer(Modifier.height(5.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateGold) {
            Text("PERCEPTIONS — PAS UNE MORALE UNIVERSELLE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            val p = d.perception
            Text("Quartier ${signed(p.district)} · ville ${signed(p.city)} · national ${signed(p.national)}", color = UltimateIvory, fontSize = 11.sp)
            Text("Gouvernement ${signed(p.government)} · police ${signed(p.police)} · civils ${signed(p.civilians)}", color = UltimateIvory, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            UltimateMeter("Peur criminelle", p.criminalFear, UltimateRed)
            Spacer(Modifier.height(5.dp))
            UltimateMeter("Peur civile", p.civilianFear, UltimateGold)
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateGreen) {
            Text("LIENS QUI ONT UNE MÉMOIRE", color = UltimateGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
            d.relationships.filter { it.alive }.sortedByDescending { it.trust + it.affection }.take(8).forEach { r ->
                Text("${r.name} · ${r.role} · ${phaseLabel(r.phase)}", color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Confiance ${r.trust} · affection ${r.affection} · rancune ${r.grudge}${if (r.knowsIdentity) " · connaît ton identité" else ""}", color = UltimateMuted, fontSize = 10.sp)
                r.memories.maxByOrNull { it.weight }?.let { Text("↳ ${it.age} ans — ${it.summary}", color = UltimateGold, fontSize = 10.sp) }
                Spacer(Modifier.height(7.dp))
            }
        }

        if (c.powerRevealed) {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = accent) {
                Text("POUVOIR EN DEVENIR", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
                val power = d.powerEvolution
                if (power == null) Text(c.powerFamily, color = UltimateIvory) else {
                    Text("${power.manifestation} · ${power.architecture.name.lowercase()} · ${power.branch}", color = UltimateIvory, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    UltimateMeter("Maîtrise", power.mastery, accent)
                    Spacer(Modifier.height(5.dp))
                    UltimateMeter("Surcharge", power.strain, UltimateRed)
                    Text("Faiblesse : ${c.weakness}", color = UltimateMuted, fontSize = 11.sp)
                    power.unlockedTechniques.takeLast(6).forEach { Text("• $it", color = UltimateIvory, fontSize = 11.sp) }
                }
            }
        }

        if (d.opportunities.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = UltimateBlue) {
                Text("CE QUE TU NE PEUX PAS TOUT FAIRE", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                d.opportunities.sortedByDescending { it.urgency }.forEach { o ->
                    val turns = (o.expiresTurn - c.turn).coerceAtLeast(0)
                    Text("${o.title} · expire dans ${if (turns == 0) "ce chapitre" else "$turns chapitre(s)"}", color = if (o.urgency >= 4) UltimateRed else UltimateIvory, fontSize = 11.sp)
                }
            }
        }

        if (d.injuries.isNotEmpty() || d.identityEvidence.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = UltimateRed) {
                Text("CE QUE LA VIE LAISSE", color = UltimateRed, fontWeight = FontWeight.Black, fontSize = 9.sp)
                d.injuries.takeLast(6).forEach { i ->
                    Text("${i.bodyPart.replaceFirstChar { it.uppercase() }} · ${i.originAge} ans · ${if (i.chronic) "chronique" else "récupération ${i.recovery}%"}", color = UltimateIvory, fontSize = 11.sp)
                }
                if (d.identityEvidence.isNotEmpty()) {
                    val pressure = d.identityEvidence.sumOf { it.strength }
                    Text("Indices d'identité : ${d.identityEvidence.size} · pression cumulée $pressure", color = UltimateMuted, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = Color(0xFFFFA64D)) {
            Text("RYTHME DE TA VIE", color = Color(0xFFFFA64D), fontWeight = FontWeight.Black, fontSize = 9.sp)
            UltimateMeter("Tension", d.drama.tension, Color(0xFFFFA64D))
            Spacer(Modifier.height(5.dp))
            UltimateMeter("Récupération", d.drama.recoveryNeed, UltimateGreen)
            Spacer(Modifier.height(5.dp))
            UltimateMeter("Pression personnelle", d.drama.personalPressure, UltimateViolet)
            Spacer(Modifier.height(5.dp))
            UltimateMeter("Pression du monde", d.drama.worldPressure, UltimateBlue)
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateMuted) {
            Text("SOUVENIRS FONDATEURS", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 9.sp)
            d.memories.sortedByDescending { it.weight }.take(12).sortedBy { it.turn }.forEach { m ->
                Text("${m.age} ans — ${m.summary}", color = if (m.weight >= 7) UltimateGold else UltimateMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
internal fun DeepLegacyHallScreen(oldHallCount: Int, records: List<DeepLegacyRecord>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Hall of Legacies", "Les vies que le monde n'a pas oubliées", "Chaque partie terminée devient une pièce de musée : pouvoir, liens, blessures et moments qui ont fabriqué sa légende.", UltimateGold)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            UltimatePill("${records.size} archives", UltimateGold)
            UltimatePill("$oldHallCount classiques", UltimateBlue)
        }
        Spacer(Modifier.height(12.dp))
        if (records.isEmpty()) {
            UltimatePanel(accent = UltimateBlue) {
                Text("LE HALL EST ENCORE VIDE", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Termine une destinée. Son empreinte restera ici et donnera un visage à l'histoire de tes différentes vies.", color = UltimateMuted, lineHeight = 18.sp)
            }
        }
        records.forEachIndexed { index, record ->
            LegacyGalleryCard(record, index)
            Spacer(Modifier.height(11.dp))
        }
        MhlSecondaryButton("Retour", onBack, Modifier.fillMaxWidth())
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun LegacyGalleryCard(r: DeepLegacyRecord, index: Int) {
    val profile = powerVisualProfile(r.power)
    val accent = if (r.power.isBlank()) UltimateGold else profile.accent
    val hash = (r.name + r.alias + r.power).hashCode().absoluteValue
    Box(
        Modifier.fillMaxWidth().heightIn(min = 218.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xEE101824), Color(0xF0070A0F))))
            .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(22.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(accent.copy(alpha = .09f), size.minDimension * .62f, Offset(size.width * .93f, size.height * .18f))
            drawCircle(profile.secondary.copy(alpha = .045f), size.minDimension * .42f, Offset(size.width * .78f, size.height * .42f))
            repeat(7) { i ->
                val bw = size.width * .055f
                val bh = size.height * (.12f + ((hash ushr (i * 2)) and 7) / 45f)
                val x = size.width * (.63f + i * .055f)
                drawRect(Color.Black.copy(alpha = .23f), Offset(x, size.height - bh), Size(bw, bh))
            }
            drawRect(accent.copy(alpha = .75f), Offset(0f, 0f), Size(3f, size.height))
            drawLine(Color.White.copy(alpha = .045f), Offset(size.width * .05f, size.height - 1f), Offset(size.width * .95f, size.height - 1f), 1f)
        }

        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(15.dp))
                        .background(Color.Black.copy(alpha = .26f))
                        .border(1.dp, accent.copy(alpha = .34f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text((index + 1).toString().padStart(2, '0'), color = accent, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.alias.ifBlank { r.name }.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = .5.sp)
                    Text("${r.power.ifBlank { "HUMAIN" }.uppercase()} · ${r.finalAge} ANS · ${r.scope.uppercase()}", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .55.sp)
                }
            }
            Spacer(Modifier.height(11.dp))
            Text(r.headline, color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (r.strongestRelationship.isNotBlank()) UltimatePill("Lien majeur", UltimateGreen)
                if (r.nemesis.isNotBlank()) UltimatePill("Némésis", UltimateRed)
                if (r.lastingInjury.isNotBlank()) UltimatePill("Marqué", UltimateMuted)
            }
            if (r.personality.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(r.personality, color = UltimateMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
            if (r.strongestRelationship.isNotBlank()) Text("Lien · ${r.strongestRelationship}", color = UltimateGreen, fontSize = 10.sp)
            if (r.nemesis.isNotBlank()) Text("Némésis · ${r.nemesis}", color = UltimateRed, fontSize = 10.sp)
            if (r.techniques.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text(r.techniques.take(3).joinToString("  ·  "), color = UltimateViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (r.memories.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text("MOMENT RETENU", color = UltimateGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Text(r.memories.first(), color = UltimateMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Spacer(Modifier.height(7.dp))
            Text(r.perception, color = UltimateBlue, fontSize = 9.sp)
        }
    }
}

private fun phaseLabel(p: RelationshipPhase) = when (p) {
    RelationshipPhase.STRANGER -> "inconnu"
    RelationshipPhase.ACQUAINTANCE -> "connaissance"
    RelationshipPhase.FRIEND -> "ami"
    RelationshipPhase.CLOSE -> "proche"
    RelationshipPhase.TRUSTED -> "confiance absolue"
    RelationshipPhase.HURT -> "blessé"
    RelationshipPhase.DISTANT -> "distant"
    RelationshipPhase.RIVAL -> "rival"
    RelationshipPhase.MENTOR -> "mentor"
    RelationshipPhase.EQUAL -> "égal"
    RelationshipPhase.PROTEGE -> "protégé"
    RelationshipPhase.SUCCESSOR -> "successeur"
}
